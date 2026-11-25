package no.sikt.nva.apitest.cristinapi.person;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsClient;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("POST " + LookupPersonByNinApiTest.PERSON_LOOKUP_PATH)
class LookupPersonByNinApiTest extends IntegrationTestBase {

  static final String PERSON_LOOKUP_PATH = "/cristin/person/identityNumber";

  /**
   * NVA's API, set on each request because the Cristin tests in this module point the global
   * RestAssured base URI at Cristin's own API, and test classes run concurrently.
   */
  private static final String NVA_API_URI =
      "https://" + CognitoLogin.getValueFromParameterStore("/NVA/ApiDomain");

  /** Seeded by NVA-end-to-end-testing with the approval-upsert scope only. */
  private static final String APPROVAL_CLIENT_SECRET = "ApiTestApprovalClientUib";

  private static final String SYNTHETIC_NIN = "14913439067";
  private static final String NIN_TYPE = "NationalIdentificationNumber";

  /** A lookup without a token is rejected by API Gateway before it reaches the handler. */
  @Test
  @DisplayName("Lookup without token returns Unauthorized")
  @Description
  void shouldReturnUnauthorizedWithoutToken() {
    lookUp(givenUnauthenticatedJsonRequest()).statusCode(HTTP_UNAUTHORIZED);
  }

  /**
   * A valid client credentials token without the person-lookup-by-nin scope is rejected by API
   * Gateway. The handler has no scope check of its own, so this is the only place it is verified.
   */
  @Test
  @DisplayName("Lookup with token lacking person-lookup-by-nin scope returns Unauthorized")
  @Description
  void shouldReturnUnauthorizedForTokenWithoutPersonLookupScope() {
    lookUp(givenAuthenticatedJsonRequestAsClient(APPROVAL_CLIENT_SECRET))
        .statusCode(HTTP_UNAUTHORIZED);
  }

  // The global config logs the whole request when validation fails, and the body holds the
  // national identity number. LogConfig.logConfig() is RestAssured's default, which does not log
  // on failure, so setting it for this request keeps the number out of the test output.
  private static ValidatableResponse lookUp(RequestSpecification request) {
    return request
        .baseUri(NVA_API_URI)
        .config(RestAssured.config().logConfig(LogConfig.logConfig()))
        .body(Map.of("type", NIN_TYPE, "value", SYNTHETIC_NIN))
        .when()
        .post(PERSON_LOOKUP_PATH)
        .then();
  }
}
