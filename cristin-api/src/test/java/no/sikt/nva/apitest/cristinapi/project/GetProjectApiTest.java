package no.sikt.nva.apitest.cristinapi.project;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PROJECT_PATH;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET " + PROJECT_PATH)
class GetProjectApiTest extends CristinApiTestBase {

  private static final String PROJECT_IDENTIFIER = "435010";
  private static final String EXPECTED_PROJECT_FILE = "project.json";

  /** Fetch a Cristin project returns project and status {@code 200 Ok} */
  @Test
  @DisplayName("Fetch a Cristin project returns project")
  @Description
  void shouldReturnProject() {

    var jsonResponse =
        givenUnauthenticatedJsonRequest()
            .when()
            .get(PROJECT_PATH, PROJECT_IDENTIFIER)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .body()
            .asString();

    var expected = readResourceFileAsString(EXPECTED_PROJECT_FILE);
    assertThatJson(jsonResponse).when(IGNORING_EXTRA_FIELDS).isEqualTo(expected);
  }

  /** Fetch non-existing project returns {@code 404 Not Found} */
  @Test
  @DisplayName("Fetch non-existing project returns Not Found")
  @Description
  void shouldReturnNotFoundForNonExistingProject() {
    var nonExistingProjectIdentifier = "000000";
    givenUnauthenticatedJsonRequest()
        .when()
        .get(PROJECT_PATH, nonExistingProjectIdentifier)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** Fetch project with invalid project identifier returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Fetch project with invalid project identifier returns Bad Request")
  @Description
  void shouldReturnBadRequestWithInvalidProjectIdentifier() {
    var invalidProjectIdentifier = "invalid";
    givenUnauthenticatedJsonRequest()
        .when()
        .get(PROJECT_PATH, invalidProjectIdentifier)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }
}
