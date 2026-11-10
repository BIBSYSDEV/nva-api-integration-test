package no.sikt.nva.apitest.cristinapi.person;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_PATH;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetPersonApiTest extends CristinApiTestBase {

  private static final String EXPECTED_PERSON_FILE = "persons/person_response.json";

  /** Get person by person id returns person and status {@code 200 OK} */
  @Test
  @DisplayName("Get person by person id returns person")
  @Description
  void shouldReturnPerson() {

    var personId = "250";

    var personResponse =
        givenUnauthenticatedJsonRequest()
            .when()
            .get(PERSON_PATH, personId)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .body()
            .asString();

    var expected = readResourceFileAsString(EXPECTED_PERSON_FILE);
    assertThatJson(personResponse).isEqualTo(expected);
  }

  /** Get person with invalid id returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Get person with invalid id returns Bad Request")
  @Description
  void shouldReturnBadRequestForInvalidPersonId() {

    var invalidPersonId = "invalid";

    givenUnauthenticatedJsonRequest()
        .when()
        .get(PERSON_PATH, invalidPersonId)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Get non-existing person returns status {@code 404 Not Found} */
  @Test
  @DisplayName("Get non-existing person returns Not Found")
  @Description
  void shouldReturnNotFoundForNonExistingPerson() {

    var nonExistingPersonId = "0";

    givenUnauthenticatedJsonRequest()
        .when()
        .get(PERSON_PATH, nonExistingPersonId)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }
}
