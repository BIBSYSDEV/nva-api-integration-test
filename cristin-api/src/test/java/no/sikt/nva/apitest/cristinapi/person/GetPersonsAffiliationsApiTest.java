package no.sikt.nva.apitest.cristinapi.person;

import static io.restassured.http.Method.GET;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_AFFILIATIONS_PATH;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET " + PERSON_AFFILIATIONS_PATH)
class GetPersonsAffiliationsApiTest extends CristinApiTestBase {

  private static final String PERSON_ID = "43419";

  /** Get affiliations for a person returns list of affiliations and status {@code 200 Ok} */
  @Test
  @DisplayName("Get affiliations for a person returns list of affiliations")
  @Description
  void shouldReturnAffiliationsForPerson() {
    var affilationsResponse =
        givenAuthenticatedCristinJsonRequest()
            .when()
            .get(PERSON_AFFILIATIONS_PATH, PERSON_ID)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .body()
            .asString();

    var expected = readResourceFileAsString("persons/person_affiliations_response.json");
    assertThatJson(affilationsResponse).isEqualTo(expected);
  }

  /** Get affiliations for a person with invalid id returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Get affiliations for a person with invalid id returns Bad Request")
  @Description
  void shouldReturnBadRequestWhenPersonIdIsInvalid() {
    var invalidId = "invalid";
    givenAuthenticatedCristinJsonRequest()
        .when()
        .get(PERSON_AFFILIATIONS_PATH, invalidId)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Get affiliations for a person when unauthenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Get affiliations for a person when unauthenticated returns Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhenNotAuthenticated() {
    requestShouldReturnUnauthorized(GET, PERSON_AFFILIATIONS_PATH, PERSON_ID);
  }
}
