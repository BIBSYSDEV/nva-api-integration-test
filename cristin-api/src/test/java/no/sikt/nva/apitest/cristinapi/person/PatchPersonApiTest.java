package no.sikt.nva.apitest.cristinapi.person;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import java.util.Map;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PATCH " + PERSON_PATH)
class PatchPersonApiTest extends CristinApiTestBase {

  private static final String FIRST_NAME = "first_name";
  private static final String PERSON_ID = "1862519";
  private static final String PATCHED_FIRST_NAME = "Patch Kristiania " + System.currentTimeMillis();
  private static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";
  private static final Map<String, String> DUMMY_PAYLOAD = Map.of(FIRST_NAME, "dummy");

  /** Patch person updates person and return {@code 204 No Content} */
  @Test
  @DisplayName("Patch person updates person")
  @Description
  void shouldUpdatePerson() {

    var payload = Map.of(FIRST_NAME, PATCHED_FIRST_NAME);

    givenAuthenticatedCristinRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .body(payload)
        .when()
        .patch(PERSON_PATH, PERSON_ID)
        .then()
        .statusCode(HTTP_NO_CONTENT);

    var patchedResponse = fetchPerson(PERSON_ID);
    assertThat(patchedResponse.getString(FIRST_NAME)).isEqualTo(PATCHED_FIRST_NAME);
  }

  private static JsonPath fetchPerson(String personId) {
    return givenAuthenticatedCristinRequest()
        .when()
        .get(PERSON_PATH, personId)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath();
  }

  /** Update person when unauthenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Update person when unauthenticated returns Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhenUnauthenticated() {

    given()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .body(DUMMY_PAYLOAD)
        .when()
        .patch(PERSON_PATH, PERSON_ID)
        .then()
        .statusCode(HTTP_UNAUTHORIZED);
  }

  /** Update non-existing person should return {@code 404 Not Found} */
  @Test
  @DisplayName("Update non-existing person should return Not Found")
  @Description
  void shouldReturnNotFoundWhenNonExistingPerson() {

    var personId = "9999999";
    givenAuthenticatedCristinRequest()
        .contentType(APPLICATION_MERGE_PATCH_JSON)
        .body(DUMMY_PAYLOAD)
        .when()
        .patch(PERSON_PATH, personId)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }
}
