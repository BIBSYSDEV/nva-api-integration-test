package no.sikt.nva.apitest.cristinapi.person;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_INSTITUTION_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import java.util.Map;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PATCH " + PERSON_INSTITUTION_PATH)
class PatchPersonsInstitutionApiTest extends CristinApiTestBase {

  private static final String PERSON_ID = "1862480";
  private static final String INSTITUTION_ID = "215";
  private static final String EMAIL = "api.test.user@test.sikt.no";
  private static final Map<String, String> DUMMY_PAYLOAD = Map.of("email", "dummy@test.sikt.no");
  private static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";

  /** Patch person institution updates person instution email returns {@code 204 No Content} */
  @Test
  @DisplayName("Patch person institution updates person instution email")
  @Description
  void shouldUpdatePersonInstitutionEmail() {

    var payload = Map.of("email", EMAIL);

    givenAuthenticatedCristinRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .body(payload)
        .when()
        .patch(PERSON_INSTITUTION_PATH, PERSON_ID, INSTITUTION_ID)
        .then()
        .statusCode(HTTP_NO_CONTENT);

    var fetchResponse =
        givenAuthenticatedCristinRequest()
            .when()
            .get(PERSON_INSTITUTION_PATH, PERSON_ID, INSTITUTION_ID)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    assertThat(fetchResponse.getString("email")).isEqualTo(EMAIL);
  }

  /** Patch person institutions when unauthenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Patch person institutions when unauthenticated returns Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhenUnauthorized() {
    given()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .body(DUMMY_PAYLOAD)
        .when()
        .patch(PERSON_INSTITUTION_PATH, PERSON_ID, INSTITUTION_ID)
        .then()
        .statusCode(HTTP_UNAUTHORIZED);
  }

  /** Patch person institution with invalid person id returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Patch person institution with invalid person id returns Bad Request")
  @Description
  void shouldReturnBadRequestWithInvalidPersonId() {

    var personId = "invalid";

    givenAuthenticatedCristinJsonRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .body(DUMMY_PAYLOAD)
        .when()
        .patch(PERSON_INSTITUTION_PATH, personId, INSTITUTION_ID)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Patch person institution with invalid person id returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Patch person institution with invalid institution id returns Bad Request")
  @Description
  void shouldReturnBadRequestWithInvalidInstitutionId() {

    var institutionId = "invalid";

    givenAuthenticatedCristinJsonRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .body(DUMMY_PAYLOAD)
        .when()
        .patch(PERSON_INSTITUTION_PATH, PERSON_ID, institutionId)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Patch person institutiton with wrong person id returns {@code 404 Not Found} */
  @Test
  @DisplayName("Patch person institutiton with wrong person id returns Not Found")
  @Description
  void shouldReturnNotFoundWithWrongPersonId() {
    var personId = "999999";
    var institutionId = "192";

    givenAuthenticatedCristinJsonRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .body(DUMMY_PAYLOAD)
        .when()
        .patch(PERSON_INSTITUTION_PATH, personId, institutionId)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** Patch person institutiton with wrong institution id returns {@code 404 Not Found} */
  @Test
  @DisplayName("Patch person institutiton with wrong institution id returns Not Found")
  @Description
  void shouldReturnNotFoundWithWrongInstitutionId() {
    var institutionId = "999";

    givenAuthenticatedCristinJsonRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .body(DUMMY_PAYLOAD)
        .when()
        .patch(PERSON_INSTITUTION_PATH, PERSON_ID, institutionId)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  @AfterAll
  static void resetEmail() {

    givenAuthenticatedCristinRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .body(DUMMY_PAYLOAD)
        .when()
        .patch(PERSON_INSTITUTION_PATH, PERSON_ID, INSTITUTION_ID)
        .then()
        .statusCode(HTTP_NO_CONTENT);
  }
}
