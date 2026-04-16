package no.sikt;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.SsmException;

class CognitoLogin {

    private static final String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
    private static final String AWS_SESSION_TOKEN = System.getenv("AWS_SESSION_TOKEN");
    private static final String REGION = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION")
            : "eu-west-1";
    private static final AwsSessionCredentials credentials = AwsSessionCredentials.create(
            AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN);

    private static final String COGNITO_URI = getCognitoUriFromParameterStore();
    private static final String CLIENT_ID = getClientIdFromParameterStore();
    private static final String REDIRECT_URI = "https://e2e.nva.aws.unit.no";

    private static String secretPassword = "";

    /**
     * Get password AWS Secrets Manager.
     */
    private static String fetchPasswordFromSecretsManager() {

        SecretsManagerClient secretsManager = SecretsManagerClient.builder()
                .region(Region.of(REGION))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId("TestUserPassword")
                .build();

        GetSecretValueResponse response = secretsManager.getSecretValue(request);
        return response.secretString();
    }

    private static String getClientIdFromParameterStore() {
        String env = System.getenv("AWS_CLIENT_ID");
        if (env != null && !env.isEmpty()) {
            return env;
        }

        try (SsmClient ssm = SsmClient.builder()
                .region(Region.of(REGION))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()) {

            GetParameterRequest request = GetParameterRequest.builder()
                    .name("CognitoUserPoolAppClientId")
                    .withDecryption(false)
                    .build();

            GetParameterResponse response = ssm.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch CLIENT_ID from Parameter Store", e);
        }
    }

    private static String getCognitoUriFromParameterStore() {
        String env = System.getenv("COGNITO_URI");
        if (env != null && !env.isEmpty()) {
            return env;
        }

        try (SsmClient ssm = SsmClient.builder()
                .region(Region.of(REGION))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()) {

            GetParameterRequest request = GetParameterRequest.builder()
                    .name("/NVA/CognitoUri")
                    .withDecryption(false)
                    .build();

            GetParameterResponse response = ssm.getParameter(request);

            if (response == null || response.parameter() == null || response.parameter().value() == null
                    || response.parameter().value().isEmpty()) {
                throw new RuntimeException("Parameter '/NVA/CognitoUri' was not found or contains no value. "
                        + "Set environment variable COGNITO_URI or create the parameter in Parameter Store.");
            }

            String value = response.parameter().value();
            return value;
        } catch (ParameterNotFoundException pnfe) {
            throw new RuntimeException("Cognito URI parameter '/NVA/CognitoUri' not found in Parameter Store. "
                    + "Set environment variable COGNITO_URI or create the parameter.", pnfe);
        } catch (SsmException ssmEx) {
            throw new RuntimeException("AWS SSM error while fetching '/NVA/CognitoUri': " + ssmEx.getMessage(), ssmEx);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error fetching Cognito URI from Parameter Store", e);
        }
    }

    /**
     * Logg in as a Cognito-bruker and return tokens.
     */
    public static Map<String, String> login(String userId) {
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
        Response response = RestAssured.given()
                .headers(headers)
                .formParams(body)
                .post(String.format("%s/oauth2/token", COGNITO_URI));

        if (response.statusCode() == 200) {
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", response.jsonPath().getString("access_token"));
            tokens.put("idToken", response.jsonPath().getString("id_token"));
            tokens.put("refreshToken", response.jsonPath().getString("refresh_token"));
            return tokens;
        } else {
            throw new RuntimeException("Failed to login: " + response.getBody().asString());
        }
    }

    /**
     * Gets an authorization code
     */
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
        Response response = RestAssured.given()
                .headers(headers)
                .formParams(body)
                .post(url);

        if (response.statusCode() == 302) {
            String location = response.getHeader("Location");
            if (location != null && location.contains("?code=")) {
                return location.split("\\?code=")[1];
            }
        }

        throw new RuntimeException("Failed to get authorization code. Response: " + response.getBody().asString());
    }

    /**
     * Generate uri to login to Cognito
     */
    private static String generateUrl() {

        String baseUrl = COGNITO_URI + "/login";
        String queryString = String.format(
                "client_id=%s&response_type=code&scope=%s&redirect_uri=%s",
                CLIENT_ID,
                "aws.cognito.signin.user.admin email https://api.nva.unit.no/scopes/frontend openid phone profile",
                REDIRECT_URI);
        return baseUrl + "?" + queryString;
    }
}