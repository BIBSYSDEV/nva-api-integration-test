package no.sikt.nva.apitest.cristinapi.organization;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.BASE_UNITS_PATH;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import java.util.Map;
import java.util.stream.Stream;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("GET " + BASE_UNITS_PATH + "/{parameters}")
class FetchOrganizationsApiTest extends CristinApiTestBase {

  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String INSTITUTION = "institution";
  private static final String COUNTRY = "country";
  private static final String PARENT_UNIT_ID = "parent_unit_id";
  private static final String PER_PAGE = "per_page";
  private static final String PAGE = "page";
  private static final String SORT = "sort";

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
        argumentSet(ID, Map.of(ID, "185.53.18.11"), "id_response.json"),
        argumentSet(NAME, Map.of(NAME, "UIO"), "name_response.json"),
        argumentSet(INSTITUTION, Map.of(INSTITUTION, "185"), "institution_response.json"),
        argumentSet(
            PARENT_UNIT_ID, Map.of(PARENT_UNIT_ID, "194.14.0.0"), "parent_unit_id_response.json"),
        argumentSet(COUNTRY, Map.of(COUNTRY, "al"), "country_response.json"),
        argumentSet(
            PER_PAGE, Map.of(COUNTRY, "al", PER_PAGE, "10"), "country_per_page_response.json"),
        argumentSet(
            PAGE,
            Map.of(COUNTRY, "al", PER_PAGE, "5", PAGE, "2"),
            "country_per_page_page_response.json"),
        argumentSet(
            "sort",
            Map.of(COUNTRY, "al", PER_PAGE, "5", PAGE, "2", SORT, "name_en desc"),
            "country_sort_response.json"));
  }
}
