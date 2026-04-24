package no.sikt;

import static java.util.Objects.nonNull;
import static org.hamcrest.Matchers.stringContainsInOrder;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

class CognitoLogin {

  /* default */ static final String REGION =
      nonNull(System.getenv("AWS_REGION")) ? System.getenv("AWS_REGION") : "eu-west-1";

  private static final String COGNITO_URI = getCognitoUriFromParameterStore();
  private static final String CLIENT_ID = getClientIdFromParameterStore();
  private static final String REDIRECT_URI = "https://e2e.nva.aws.unit.no";

  private static String secretPassword = "";
  private static final int RESPONSE_OK = 200;

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

  /* default */
  static String getValueFromParameterStore(String name) {
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

  /* default */
  /** Logg in as a Cognito-bruker and return tokens. */
  static Map<String, String> login(String userId) {
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
    JsonPath response =
        RestAssured.given()
            .headers(headers)
            .formParams(body)
            .post(String.format("%s/oauth2/token", COGNITO_URI))
            .then()
            .statusCode(RESPONSE_OK)
            .extract()
            .response()
            .jsonPath();

    Map<String, String> tokens = new HashMap<>();
    tokens.put("accessToken", response.getString("access_token"));
    tokens.put("idToken", response.getString("id_token"));
    tokens.put("refreshToken", response.getString("refresh_token"));
    return tokens;
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
            .headers(headers)
            .formParams(body)
            .post(url)
            .then()
            .statusCode(302)
            .header("Location", stringContainsInOrder("?code="))
            .extract()
            .response();

    return response.getHeader("Location").split("\\?code=", -1)[1];
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
}
