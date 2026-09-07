package no.sikt.nva.apitest.base;

import static java.util.Objects.nonNull;
import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Authenticates as an external API client with the client credentials grant, which is how
 * third-party integrations reach NVA. The clients themselves are seeded by NVA-end-to-end-testing,
 * since they cannot be deleted once created, and each one's credentials are stored under the secret
 * name passed here.
 */
public final class ClientCredentialsLogin {

  private static final String REGION =
      nonNull(System.getenv("AWS_REGION")) ? System.getenv("AWS_REGION") : "eu-west-1";

  private static final String GRANT_TYPE_PARAMETER = "grant_type";
  private static final String CLIENT_CREDENTIALS_GRANT = "client_credentials";
  private static final String ACCESS_TOKEN_FIELD = "access_token";
  private static final String EXPIRES_IN_FIELD = "expires_in";
  private static final int RESPONSE_OK = 200;
  private static final long TOKEN_EXPIRY_SAFETY_MARGIN_SECONDS = 60;

  private static final Map<String, ExternalClient> CLIENT_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, CachedToken> TOKEN_CACHE = new ConcurrentHashMap<>();

  private ClientCredentialsLogin() {}

  /** The stored client, for tests that need the customer it belongs to. */
  public static ExternalClient client(String secretName) {
    return CLIENT_CACHE.computeIfAbsent(secretName, ClientCredentialsLogin::fetchClient);
  }

  /**
   * An access token for the client, cached until shortly before it expires. Cognito throttles a
   * burst of token requests, and the test classes run concurrently, so the per-client remap holds a
   * lock that makes concurrent first-time logins authenticate once instead of stampeding.
   */
  public static String accessToken(String secretName) {
    var cachedToken =
        TOKEN_CACHE.compute(
            secretName,
            (name, existingToken) ->
                nonNull(existingToken) && !existingToken.isExpired()
                    ? existingToken
                    : requestToken(name));
    return cachedToken.accessToken();
  }

  private static ExternalClient fetchClient(String secretName) {
    try (var secretsManager = SecretsManagerClient.builder().region(Region.of(REGION)).build()) {
      var secret =
          secretsManager.getSecretValue(request -> request.secretId(secretName)).secretString();
      return ExternalClient.fromSecret(secret);
    }
  }

  // noFilters() keeps the client secret out of the Allure report, which logs every other request.
  private static CachedToken requestToken(String secretName) {
    var externalClient = client(secretName);
    var tokenResponse =
        RestAssured.given()
            .noFilters()
            .auth()
            .preemptive()
            .basic(externalClient.clientId(), externalClient.clientSecret())
            .contentType(APPLICATION_FORM_URLENCODED.getMimeType())
            .formParam(GRANT_TYPE_PARAMETER, CLIENT_CREDENTIALS_GRANT)
            .post(externalClient.tokenUri())
            .then()
            .extract()
            .response();
    requireOkResponse(tokenResponse, secretName);

    var token = tokenResponse.jsonPath();
    return new CachedToken(token.getString(ACCESS_TOKEN_FIELD), expiryFrom(token));
  }

  private static Instant expiryFrom(JsonPath tokenResponse) {
    var expiresInSeconds = tokenResponse.getLong(EXPIRES_IN_FIELD);
    return Instant.now().plusSeconds(expiresInSeconds - TOKEN_EXPIRY_SAFETY_MARGIN_SECONDS);
  }

  // The request runs without filters, so a failure would otherwise surface as an AssertionError
  // with no body. Cognito puts the reason there, such as invalid_client for a rotated secret.
  private static void requireOkResponse(Response response, String secretName) {
    if (response.statusCode() != RESPONSE_OK) {
      throw new IllegalStateException(
          "Client credentials login for %s returned status %d (expected %d). Response body: %s"
              .formatted(secretName, response.statusCode(), RESPONSE_OK, response.asString()));
    }
  }

  private record CachedToken(String accessToken, Instant expiresAt) {
    private boolean isExpired() {
      return Instant.now().isAfter(expiresAt);
    }
  }
}
