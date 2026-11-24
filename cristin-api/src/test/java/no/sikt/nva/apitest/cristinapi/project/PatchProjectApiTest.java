package no.sikt.nva.apitest.cristinapi.project;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNSUPPORTED_TYPE;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import static io.restassured.RestAssured.given;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinRequest;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import no.sikt.nva.apitest.cristinapi.CristinPaths;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PROJECT_PATH;

@DisplayName("PATCH " + CristinPaths.PROJECT_PATH)
class PatchProjectApiTest extends CristinApiTestBase{

  private static final String UPDATED_TITLE = "Api test project 2026 patched";
  private static final String PROJECT_ID = "2751400";
  private static final String INITIAL_TITLE = "Api test project 2026";
  private static final Map<String, Object> DUMMY_PAYLOAD = Map.of("title", Map.of("nb", "dummy"));

  /** Patch project updates project and returns{@code 204 No Content} */
  @Test
  @DisplayName("Patch project updates project")
  @Description
  void shouldUpdateProject() {

    var payload = Map.of("title", Map.of("nb", UPDATED_TITLE));

    givenAuthenticatedCristinRequest()
    .contentType(APPLICATION_MERGE_PATCH_JSON)
    .body(payload)
    .when()
    .patch(PROJECT_PATH, PROJECT_ID)
    .then()
    .statusCode(HTTP_NO_CONTENT);

    var updatedTitle = getProjectTitle(PROJECT_ID);
    assertThat(updatedTitle).isNotEqualTo(INITIAL_TITLE);
    assertThat(updatedTitle).isEqualTo(UPDATED_TITLE);
  }

  /** Update project when unauthenticated should return {@code 401 Unauthorized} */
  @Test 
  @DisplayName("Update project when unauthenticated should return Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhenNotAuthenticated() {
    given()
    .contentType(APPLICATION_MERGE_PATCH_JSON)
    .body(DUMMY_PAYLOAD)
    .when()
    .patch(PROJECT_PATH, PROJECT_ID)
    .then()
    .statusCode(HTTP_UNAUTHORIZED);
  }

  /** Update project with missing content type should return {@code 415 Unsupported Media Type} */
  @Test 
  @DisplayName("Update project with missing content type should return Unsupported Media Type")
  @Description 
  void shouldReturnUnsupportedMediaType() {
    givenAuthenticatedCristinRequest()
    .body(DUMMY_PAYLOAD)
    .when()
    .patch(PROJECT_PATH, PROJECT_ID)
    .then()
    .statusCode(HTTP_UNSUPPORTED_TYPE);
  }

  /** Update project with non-existing project return {@code 404 Not Found} */
  @Test 
  @DisplayName("Update project with non-existing project return Not Found")
  @Description 
  void shouldReturnNotFound() {
    var nonExistingProjectId = "000000";
    givenAuthenticatedCristinRequest()
    .contentType(APPLICATION_MERGE_PATCH_JSON)
    .body(DUMMY_PAYLOAD)
    .when()
    .patch(PROJECT_PATH, nonExistingProjectId)
    .then()
    .statusCode(HTTP_NOT_FOUND);
  }


  private static String getProjectTitle(String projectId) {
    return givenUnauthenticatedJsonRequest()
    .when()
    .get(PROJECT_PATH, projectId)
    .then()
    .statusCode(HTTP_OK)
    .extract()
    .jsonPath()
    .getString("title.nb");
  }

  @AfterAll 
  static void cleanUp() {
    var payload = Map.of("title", Map.of("nb", INITIAL_TITLE));

    givenAuthenticatedCristinRequest()
    .contentType(APPLICATION_MERGE_PATCH_JSON)
    .body(payload)
    .when()
    .patch(PROJECT_PATH, PROJECT_ID)
    .then()
    .statusCode(HTTP_NO_CONTENT);

  }

}
