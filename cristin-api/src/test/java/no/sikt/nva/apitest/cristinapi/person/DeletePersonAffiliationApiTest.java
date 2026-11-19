package no.sikt.nva.apitest.cristinapi.person;

import static io.restassured.http.Method.DELETE;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.AFFILIATION_PATH;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_AFFILIATIONS_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DELETE " + AFFILIATION_PATH)
class DeletePersonAffiliationApiTest extends CristinApiTestBase {

  private static final String PERSON_ID = "1421086";
  private static final String INSTIUTION_ID = "2012";
  private static final String CRISTIN_REPRESENTING_INSTITUTION = "Cristin-Representing-Institution";

  @BeforeAll
  static void init() {

    var existingAffiliation = findExistingAffiliationId(PERSON_ID, INSTIUTION_ID);

    if (existingAffiliation == null) {
      var createPayload = readResourceFileAsString("persons/create_affiliation_payload.json");
      givenAuthenticatedCristinJsonRequest()
          .header(CRISTIN_REPRESENTING_INSTITUTION, INSTIUTION_ID)
          .body(createPayload)
          .when()
          .post(PERSON_AFFILIATIONS_PATH, PERSON_ID)
          .then()
          .statusCode(HTTP_CREATED);
    }
  }

  /** Delete affiliation from person removes affiliation from person and returns {@code 200 Ok} */
  @Test
  @DisplayName("Delete affiliation from person removes affiliation from person")
  @Description
  void shouldDeleteAffiliationFromPerson() {

    var existingAffiliation = findExistingAffiliationId(PERSON_ID, INSTIUTION_ID);
    assertThat(existingAffiliation).isNotNull();

    givenAuthenticatedCristinJsonRequest()
        .header(CRISTIN_REPRESENTING_INSTITUTION, INSTIUTION_ID)
        .when()
        .delete(AFFILIATION_PATH, PERSON_ID, existingAffiliation)
        .then()
        .statusCode(HTTP_OK);

    var deletedAffiliation = findExistingAffiliationId(PERSON_ID, INSTIUTION_ID);
    assertThat(deletedAffiliation).isNull();
  }

  /** Delete affiliation when unauthenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Delete affiliation when unauthenticated returns Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhenNotAuthenticated() {
    var dummyAffiliationId = "123456";

    requestShouldReturnUnauthorized(DELETE, AFFILIATION_PATH, PERSON_ID, dummyAffiliationId);
  }

  /** Trying to delete non-existing affiliation returns {@code 404 Not Found} */
  @Test
  @DisplayName("Trying to delete non-existing affiliation returns Not Found")
  @Description
  void shouldReturnNotFoundWhenNonExistingAffiliation() {
    var nonExistingAffiliation = "0";

    givenAuthenticatedCristinJsonRequest()
        .header(CRISTIN_REPRESENTING_INSTITUTION, INSTIUTION_ID)
        .when()
        .delete(AFFILIATION_PATH, PERSON_ID, nonExistingAffiliation)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** Delete person affiliation with invalid affilation id returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Delete person affiliation with invalid affiliation id returns Bad Request")
  @Description
  void shouldReturnBadRequestWhenDeleteWithInvalidAffiliationId() {
    var invalidAffilationId = "invalid";

    givenAuthenticatedCristinJsonRequest()
        .header(CRISTIN_REPRESENTING_INSTITUTION, INSTIUTION_ID)
        .when()
        .delete(AFFILIATION_PATH, PERSON_ID, invalidAffilationId)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  @AfterAll
  static void cleanUp() {
    var existingAffiliation = findExistingAffiliationId(PERSON_ID, INSTIUTION_ID);

    if (existingAffiliation != null) {
      givenAuthenticatedCristinJsonRequest()
          .header(CRISTIN_REPRESENTING_INSTITUTION, INSTIUTION_ID)
          .when()
          .delete(AFFILIATION_PATH, PERSON_ID, existingAffiliation)
          .then()
          .statusCode(HTTP_OK);
    }
  }
}
