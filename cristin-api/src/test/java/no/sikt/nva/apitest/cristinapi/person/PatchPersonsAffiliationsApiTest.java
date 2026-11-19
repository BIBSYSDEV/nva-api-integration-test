package no.sikt.nva.apitest.cristinapi.person;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNSUPPORTED_TYPE;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.AFFILIATION_PATH;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_AFFILIATIONS_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import java.util.Map;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PATCH " + PERSON_AFFILIATIONS_PATH)
class PatchPersonsAffiliationsApiTest extends CristinApiTestBase {

  private static final String PERSON_ID = "1862465";
  private static final String INSTITUTION_ID = "184";
  private static final Map<String, Float> DEFAULT_CHANGE_PAYLOAD = Map.of("fte_percentage", 100f);
  private static final String CRISTIN_REPRESENTING_INSTITUTION = "Cristin-Representing-Institution";
  private static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";

  private static final String AFFILIATION_ID = findExistingAffiliationId(PERSON_ID, INSTITUTION_ID);

  private float readFtePercentage(String personId, String institutionId) {
    return givenAuthenticatedCristinJsonRequest()
        .when()
        .get(PERSON_AFFILIATIONS_PATH, personId)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath()
        .param("institutionId", institutionId)
        .getFloat(
            "find { it.affiliation.institution.cristin_institution_id == institutionId"
                + " }.fte_percentage");
  }

  /** Patch person affiliation updates person affiliation and returns {@code 204 No Content} */
  @Test
  @DisplayName("Patch person affiliation updates person affiliation and returns No Content")
  @Description
  void shouldUpdatePersonAffiliation() {

    var currentPercentage = readFtePercentage(PERSON_ID, INSTITUTION_ID);
    var percentageChange = currentPercentage == 100f ? 50f : 100f;

    var changePayload = Map.of("fte_percentage", percentageChange);

    givenAuthenticatedCristinJsonRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .header(CRISTIN_REPRESENTING_INSTITUTION, INSTITUTION_ID)
        .body(changePayload)
        .when()
        .patch(AFFILIATION_PATH, PERSON_ID, AFFILIATION_ID)
        .then()
        .statusCode(HTTP_NO_CONTENT);

    var changedPercentage = readFtePercentage(PERSON_ID, INSTITUTION_ID);
    assertThat(changedPercentage).isEqualTo(percentageChange);
  }

  /** Patch person affiliation with non-existing person returns {@code 404 Not Found} */
  @Test
  @DisplayName("Patch person affiliation with non-existing person returns Not Found")
  @Description
  void shouldReturnNotFoundWithNonExistingPerson() {

    var nonExistingPerson = "0";

    givenAuthenticatedCristinJsonRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .header(CRISTIN_REPRESENTING_INSTITUTION, INSTITUTION_ID)
        .body(DEFAULT_CHANGE_PAYLOAD)
        .when()
        .patch(AFFILIATION_PATH, nonExistingPerson, AFFILIATION_ID)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** Patch person affiliation with non-existing affiliation returns {@code 404 Not Found} */
  @Test
  @DisplayName("Patch person affiliation with non-existing affiliation returns Not Found")
  @Description
  void shouldReturnNotFoundWithNonExistingAffiliation() {

    var nonExistingAffiliation = "0";

    givenAuthenticatedCristinJsonRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .header(CRISTIN_REPRESENTING_INSTITUTION, INSTITUTION_ID)
        .body(DEFAULT_CHANGE_PAYLOAD)
        .when()
        .patch(AFFILIATION_PATH, PERSON_ID, nonExistingAffiliation)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** Patch person affiliation when unauthenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Patch person affiliation when unauthenticated returns Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhenUnauthenticated() {

    givenUnauthenticatedJsonRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .header(CRISTIN_REPRESENTING_INSTITUTION, INSTITUTION_ID)
        .body(DEFAULT_CHANGE_PAYLOAD)
        .when()
        .patch(AFFILIATION_PATH, PERSON_ID, AFFILIATION_ID)
        .then()
        .statusCode(HTTP_UNAUTHORIZED);
  }

  /**
   * Patch person affiliation with missing content type returns {@code 415 Unsupported Media Type}
   */
  @Test
  @DisplayName("Patch person affiliation with missing content type returns Unsupported Media Type")
  @Description
  void shouldReturnUnsupportedTypeWithMissingContentType() {

    givenAuthenticatedCristinJsonRequest()
        .header(CRISTIN_REPRESENTING_INSTITUTION, INSTITUTION_ID)
        .body(DEFAULT_CHANGE_PAYLOAD)
        .when()
        .patch(AFFILIATION_PATH, PERSON_ID, AFFILIATION_ID)
        .then()
        .statusCode(HTTP_UNSUPPORTED_TYPE);
  }

  /** Patch person affiliation with missing institution header returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Patch person affiliation with missing institution header returns Bad Request")
  @Description
  void shouldReturnUnsupportedTypeWithMissingHeader() {

    givenAuthenticatedCristinJsonRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .body(DEFAULT_CHANGE_PAYLOAD)
        .when()
        .patch(AFFILIATION_PATH, PERSON_ID, AFFILIATION_ID)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }
}
