package no.sikt.nva.apitest.cristinapi.person;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import static io.restassured.RestAssured.given;
import io.restassured.path.json.JsonPath;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinRequest;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import no.sikt.nva.apitest.cristinapi.CristinPaths;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_PATH;

@DisplayName("PATCH " + CristinPaths.PERSON_PATH)
class PatchPersonApiTest extends CristinApiTestBase{

  private static final String PERSON_ID = "1862476";
  private static final String INITIAL_FIRST_NAME = "Registrar Kristiania";
  private static final String PATCHED_FIRST_NAME = "Registrar Kristiania Patched";
  private static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";
  private static final Map<String, String> DUMMY_PAYLOAD = Map.of("first_name", "dummy");

  /** Patch person updates person and return {@code 204 No Content} */
  @Test 
  @DisplayName("Patch person updates person")
  @Description 
  void shouldUpdatePerson() {

    var initialResponse = fetchPerson(PERSON_ID);
    assertThat(initialResponse.getString("first_name")).isEqualTo(INITIAL_FIRST_NAME);

    var payload = Map.of("first_name", PATCHED_FIRST_NAME);

    givenAuthenticatedCristinRequest()
    .contentType(APPLICATION_MERGE_PATCH_JSON)
    .body(payload)
    .when()
    .patch(PERSON_PATH, PERSON_ID)
    .then()
    .statusCode(HTTP_NO_CONTENT);

    var patchedResponse = fetchPerson(PERSON_ID);
    assertThat(patchedResponse.getString("first_name")).isEqualTo(PATCHED_FIRST_NAME);
  }

  private JsonPath fetchPerson(String personId) {
    var initialResponse = givenAuthenticatedCristinJsonRequest()
    .when()
    .get(PERSON_PATH, personId)
    .then()
    .statusCode(HTTP_OK)
    .extract()
    .jsonPath();
    return initialResponse;
  }

  /** Patch person when unauthenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Patch person when unauthenticated returns Unauthorized")
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

  @AfterAll
  static void cleanUp() {
    var payload = Map.of("first_name", INITIAL_FIRST_NAME);

    givenAuthenticatedCristinRequest()
    .contentType(APPLICATION_MERGE_PATCH_JSON)
    .body(payload)
    .when()
    .patch(PERSON_PATH, PERSON_ID)
    .then()
    .statusCode(HTTP_NO_CONTENT);
  }
}
