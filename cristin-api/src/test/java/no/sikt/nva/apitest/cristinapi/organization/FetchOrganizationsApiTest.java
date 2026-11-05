package no.sikt.nva.apitest.cristinapi.organization;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import org.junit.jupiter.params.provider.MethodSource;

import io.qameta.allure.Description;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.BASE_UNITS_PATH;

@DisplayName("GET " + BASE_UNITS_PATH + "/{parameters}")
class FetchOrganizationsApiTest extends CristinApiTestBase {

  /** Search for organizations returns list of organizations and status {@code 200 Ok} */
  @ParameterizedTest
  @MethodSource("fetchParametersProvider")
  @DisplayName("Search for organization returns list of organizations")
  @Description
  void shouldFetchUnits(Map<String, String> parameters, String expectedFile) {

    var searchResponse =
        givenUnauthenticatedJsonRequest()
            .queryParams(parameters)
            .when()
            .get(BASE_UNITS_PATH)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();

    var expected = readResourceFileAsString("organizations/" + expectedFile);
    assertThatJson(searchResponse).isEqualTo(expected);
  }

  private static Stream<Arguments> fetchParametersProvider() {
    return Stream.of(
        argumentSet("id", Map.of("id", "185.53.18.11"), "id_response.json"),
        argumentSet("name", Map.of("name", "UIO"), "name_response.json"),
        argumentSet("institution", Map.of("institution", "185"), "institution_response.json"),
        argumentSet(
            "parent_unit_id",
            Map.of("parent_unit_id", "194.14.0.0"),
            "parent_unit_id_response.json"),
        argumentSet("country", Map.of("country", "al"), "country_response.json"),
        argumentSet(
            "per_page",
            Map.of("country", "al", "per_page", "10"),
            "country_per_page_response.json"),
        argumentSet(
            "page",
            Map.of("country", "al", "per_page", "5", "page", "2"),
            "country_per_page_page_response.json"),
        argumentSet(
            "sort",
            Map.of("country", "al", "per_page", "5", "page", "2", "sort", "name_en desc"),
            "country_sort_response.json"));
  }
}
