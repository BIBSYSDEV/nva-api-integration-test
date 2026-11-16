package no.sikt.nva.apitest.cristinapi.person;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSONS_BASE_PATH;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import java.util.Map;
import java.util.stream.Stream;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("GET " + PERSONS_BASE_PATH + "?{parameters}")
class FetchPersonsApiTest extends CristinApiTestBase {

  private static final String ADD = " + ";
  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String INSTITUTION = "institution";
  private static final String PARENT_UNIT_ID = "parent_unit_id";
  private static final String LEVELS = "levels";

  /** Search for persons returns list of persons and status {@code 200 Ok} */
  @ParameterizedTest
  @MethodSource("fetchPersonsParametersProvider")
  @DisplayName("Search for persons returns list of persons")
  @Description
  void shouldReturnPersons(Map<String, String> parameters, String expectedFile) {
    var searchResponse =
        givenUnauthenticatedJsonRequest()
            .queryParams(parameters)
            .when()
            .get(PERSONS_BASE_PATH)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .body()
            .asString();

    var expected = readResourceFileAsString("persons/" + expectedFile);
    assertThatJson(searchResponse).isEqualTo(expected);
  }

  private static Stream<Arguments> fetchPersonsParametersProvider() {
    return Stream.of(
        argumentSet(ID, Map.of(ID, "30621"), "id_response.json"),
        argumentSet(NAME, Map.of(NAME, "eirik+nilsen"), "name_response.json"),
        argumentSet(
            INSTITUTION + ADD + NAME,
            Map.of(INSTITUTION, "uio", NAME, "røed"),
            "institution_name_response.json"),
        argumentSet(
            INSTITUTION + "Id + " + NAME,
            Map.of(INSTITUTION, "185", NAME, "røed"),
            "institution_name_response.json"),
        argumentSet(
            PARENT_UNIT_ID + ADD + NAME,
            Map.of(PARENT_UNIT_ID, "192.0.0.0", NAME, "nilsen"),
            "parent_unit_id_name_response.json"),
        argumentSet(
            PARENT_UNIT_ID + ADD + NAME + ADD + LEVELS,
            Map.of(PARENT_UNIT_ID, "192.0.0.0", NAME, "nilsen", LEVELS, "1"),
            "parent_unit_id_name_level_response.json"));
  }

  /** Search for persons with invalid search query returns status {@code 400 Bad Request} */
  @Test
  @DisplayName("Search for person with invalid search query returns Bad Request")
  @Description
  void shouldReturnBadRequestWithInvalidSearchQuery() {
    givenUnauthenticatedJsonRequest()
        .queryParams("invalid", "query")
        .when()
        .get(PERSONS_BASE_PATH)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }
}
