package no.sikt.nva.apitest.base;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

public final class CognitoLogin {

  private static final String REGION =
      nonNull(System.getenv("AWS_REGION")) ? System.getenv("AWS_REGION") : "eu-west-1";

  private static final String COGNITO_URI = getCognitoUriFromParameterStore();
  private static final String CLIENT_ID = getClientIdFromParameterStore();
  private static final String REDIRECT_URI = "https://e2e.nva.aws.unit.no";

  private static String secretPassword = "";
  private static final int RESPONSE_OK = 200;
  private static final int RESPONSE_REDIRECT = 302;
  private static final Map<String, CachedToken> TOKEN_CACHE = new ConcurrentHashMap<>();
  private static final long TOKEN_EXPIRY_SAFETY_MARGIN_SECONDS = 60;

  private CognitoLogin() {}

  /** Get password AWS Secrets Manager. */
  private static String fetchPasswordFromSecretsManager() {
    try (SecretsManagerClient secretsManager =
        SecretsManagerClient.builder().region(Region.of(REGION)).build()) {

      GetSecretValueRequest request =
          GetSecretValueRequest.builder().secretId("TestUserPassword").build();

      GetSecretValueResponse response = secretsManager.getSecretValue(request);
      return response.secretString();
    }
  }

  public static String getValueFromParameterStore(String name) {
    try (SsmClient ssm = SsmClient.builder().region(Region.of(REGION)).build()) {

      GetParameterRequest request =
          GetParameterRequest.builder().name(name).withDecryption(false).build();

      GetParameterResponse response = ssm.getParameter(request);
      return response.parameter().value();
    }
  }

  private static String getClientIdFromParameterStore() {
    String env = System.getenv("AWS_CLIENT_ID");

    return nonNull(env) ? env : getValueFromParameterStore("CognitoUserPoolAppClientId");
  }

  private static String getCognitoUriFromParameterStore() {
    String env = System.getenv("COGNITO_URI");
    return nonNull(env) ? env : getValueFromParameterStore("/NVA/CognitoUri");
  }

  /**
   * Log in as a Cognito user and return tokens, cached per user until shortly before they expire.
   * Every authenticated request would otherwise trigger a full login, and under parallel execution
   * that floods the Cognito pre-token-generation Lambda and gets throttled (rate exceeded). The
   * per-user remap holds a lock for that user, so concurrent first-time logins for the same user
   * authenticate once instead of stampeding.
   */
  public static Map<String, String> login(String userId) {
    var cachedToken =
        TOKEN_CACHE.compute(
            userId,
            (user, existingToken) ->
                nonNull(existingToken) && !existingToken.isExpired()
                    ? existingToken
                    : requestTokens(user));
    return cachedToken.tokens();
  }

  private static CachedToken requestTokens(String userId) {
    if (secretPassword.isEmpty()) {
      secretPassword = fetchPasswordFromSecretsManager();
    }

    String code = getCode(userId, secretPassword);

    // Build headers
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");

    // Build body
    Map<String, String> body = new HashMap<>();
    body.put("grant_type", "authorization_code");
    body.put("client_id", CLIENT_ID);
    body.put("redirect_uri", REDIRECT_URI);
    body.put("code", code);

    // Get tokens from Cognito
    var tokenResponse =
        RestAssured.given()
            .noFilters()
            .headers(headers)
            .formParams(body)
            .post(String.format("%s/oauth2/token", COGNITO_URI))
            .then()
            .extract()
            .response();
    requireStatus(tokenResponse, RESPONSE_OK, "Cognito token exchange for user " + userId);
    JsonPath response = tokenResponse.jsonPath();

    Map<String, String> tokens = new HashMap<>();
    tokens.put("accessToken", response.getString("access_token"));
    tokens.put("idToken", response.getString("id_token"));
    tokens.put("refreshToken", response.getString("refresh_token"));
    return new CachedToken(tokens, expiryFrom(response));
  }

  private static Instant expiryFrom(JsonPath tokenResponse) {
    var expiresInSeconds = tokenResponse.getLong("expires_in");
    return Instant.now().plusSeconds(expiresInSeconds - TOKEN_EXPIRY_SAFETY_MARGIN_SECONDS);
  }

  public static Map<String, String> loginUser(User user) {
    return login(user.userId());
  }

  /** Gets an authorization code */
  private static String getCode(String userName, String password) {
    String url = generateUrl();
    String randomUuid = UUID.randomUUID().toString();

    // Build headers
    Map<String, String> headers = new HashMap<>();
    headers.put("Cookie", "XSRF-TOKEN=" + randomUuid);
    headers.put("Origin", COGNITO_URI);
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Referer", url);

    // Build body
    Map<String, String> body = new HashMap<>();
    body.put("_csrf", randomUuid);
    body.put("username", userName);
    body.put("password", password);

    // Get code
    Response response =
        RestAssured.given()
            .noFilters()
            .headers(headers)
            .formParams(body)
            .post(url)
            .then()
            .extract()
            .response();
    requireStatus(response, RESPONSE_REDIRECT, "Cognito authorization for user " + userName);

    assertThat(response.header("Location")).contains("?code=");

    return response.getHeader("Location").split("\\?code=", -1)[1];
  }

  // RestAssured's statusCode() throws an AssertionError without the response body, and the login
  // calls use noFilters() so the global validation-failure logging never runs. Cognito returns the
  // failure reason (e.g. invalid_grant) in the body, so surface status and body in the exception.
  private static void requireStatus(Response response, int expectedStatus, String context) {
    if (response.statusCode() != expectedStatus) {
      throw new IllegalStateException(
          String.format(
              "%s returned status %d (expected %d). Response body: %s",
              context, response.statusCode(), expectedStatus, response.asString()));
    }
  }

  /** Generate uri to login to Cognito */
  private static String generateUrl() {

    String baseUrl = COGNITO_URI + "/login";
    String queryString =
        String.format(
            "client_id=%s&response_type=code&scope=%s&redirect_uri=%s",
            CLIENT_ID,
            "aws.cognito.signin.user.admin email https://api.nva.unit.no/scopes/frontend openid"
                + " phone profile",
            REDIRECT_URI);
    return baseUrl + "?" + queryString;
  }

  private record CachedToken(Map<String, String> tokens, Instant expiresAt) {
    private boolean isExpired() {
      return Instant.now().isAfter(expiresAt);
    }
  }
}
