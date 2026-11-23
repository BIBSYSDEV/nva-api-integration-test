package no.sikt.nva.apitest.cristinapi.person;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

import static org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import static io.restassured.http.Method.GET;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_INSTITUTION_PATH;

@DisplayName("GET " + PERSON_INSTITUTION_PATH)
@ExtendWith(SoftAssertionsExtension.class)
class GetPersonsInstitutionApiTest extends CristinApiTestBase {

  private static final String PERSON_ID = "738";
  private static final String INSTITUTION_ID = "185";

  /** Get institution for person returns institution and status {@code 200 Ok} */
  @Test
  @DisplayName("Get institution for person returns institution")
  @Description
  void shouldReturnInstitution(SoftAssertions softly) {

    var institutionResponse =
        givenAuthenticatedCristinJsonRequest()
            .when()
            .get(PERSON_INSTITUTION_PATH, PERSON_ID, INSTITUTION_ID)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    assertThat(institutionResponse.getString("email")).isEqualTo("daniel.sachse@ceres.no");
  }

  /** Get person institutions when unauthenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Get person institutions when unauthenticated returns Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhenUnauthorized() {
    requestShouldReturnUnauthorized(GET, PERSON_INSTITUTION_PATH, PERSON_ID, INSTITUTION_ID);
  }

  /** Get person institution with invalid person id returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Get person institution with invalid person id returns Bad Request")
  @Description
  void shouldReturnBadRequestWithInvalidPersonId() {

    var personId = "invalid";

    givenAuthenticatedCristinJsonRequest()
        .when()
        .get(PERSON_INSTITUTION_PATH, personId, INSTITUTION_ID)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Get person institution with invalid person id returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Get person institution with invalid institution id returns Bad Request")
  @Description
  void shouldReturnBadRequestWithInvalidInstitutionId() {

    var institutionId = "invalid";

    givenAuthenticatedCristinJsonRequest()
        .when()
        .get(PERSON_INSTITUTION_PATH, PERSON_ID, institutionId)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Get person institutiton with wrong person id returns {@code 404 Not Found} */
  @Test
  @DisplayName("Get person institutiton with wrong person id returns Not Found")
  @Description
  void shouldReturnNotFoundWithNonExistingPersonId() {
    var personId = "9999999";

    givenAuthenticatedCristinJsonRequest()
        .when()
        .get(PERSON_INSTITUTION_PATH, personId, INSTITUTION_ID)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** Get person institutiton with wrong institution id returns {@code 404 Not Found} */
  @Test
  @DisplayName("Get person institutiton with wrong institution id returns Not Found")
  @Description
  void shouldReturnNotFoundWithNonExistingInstitutionId() {
    var institutionId = "999";

    givenAuthenticatedCristinJsonRequest()
        .when()
        .get(PERSON_INSTITUTION_PATH, PERSON_ID, institutionId)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }
}
