package no.sikt.nva.apitest.cristinapi.organization;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.BASE_UNITS_PATH;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.INSTITUTION_PATH;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.UNIT_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET " + BASE_UNITS_PATH)
class GetOrganizationApiTest extends CristinApiTestBase {

  private static final String UNKNOWN_INSTITUTION_FILE = "organizations/unknown_institution.json";
  private static final String EXPECTED_UNITS_FILE = "organizations/units.json";
  private static final String UNIT_ID = "185.53.18.14";
  private static final String TOP_LEVEL_INSTITUTION_FILE =
      "organizations/top_level_institution_response.json";

  /** Fetch a Cristin unit returns unit and status {@code 200 Ok} */
  @Test
  @DisplayName("Fetch a Cristin unit returns unit")
  @Description
  void shouldReturnUnit() {

    var unitResponse =
        givenUnauthenticatedJsonRequest()
            .when()
            .get(UNIT_PATH, UNIT_ID)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .body()
            .asString();

    var expected = readResourceFileAsString(EXPECTED_UNITS_FILE);
    assertThatJson(unitResponse).isEqualTo(expected);
  }

  /** Fetch a non-existing Cristin unit returns status {@code 404 Not Found} */
  @Test
  @DisplayName("Fetch a non-existing Cristin unit returns Not Found")
  @Description
  void shouldReturnNotFoundForNonExistingUnit() {
    var nonExistingUnit = "0.0.0.1";
    givenUnauthenticatedJsonRequest()
        .when()
        .get(UNIT_PATH, nonExistingUnit)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** Fetch a Cristin unit with invalid unit id returns status {@code 400 Bad Request} */
  @Test
  @DisplayName("Fetch a Cristin unit with invalid unit id returns Bad Request")
  @Description
  void shouldReturnBadRequestForInvalidUnitId() {
    var invalidUnitId = "invalid";
    givenUnauthenticatedJsonRequest()
        .when()
        .get(UNIT_PATH, invalidUnitId)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /**
   * Fetch a Cristin institution with non-existing institution id returns unknown institution and
   * status {@code 200 Ok }
   */
  @Test
  @DisplayName("Fetch a non-existing Cristin institution returns unknown institution response")
  @Description
  void shouldReturnUnknownInsitution() {
    var nonExistingInstitution = "1.0.0.0";
    var unknownResponse =
        givenUnauthenticatedJsonRequest()
            .when()
            .get(UNIT_PATH, nonExistingInstitution)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .body()
            .asString();

    var expected = readResourceFileAsString(UNKNOWN_INSTITUTION_FILE);
    assertThatJson(unknownResponse).isEqualTo(expected);
  }

  /**
   * Fetch a Cristin instiution with an id that redirects to another id should return the redirected
   * institution unit and status {@code 200 OK}
   */
  @Test
  @DisplayName(
      "Fetch a institution that with an id that redirects to another id should return the"
          + " redirected institution unit")
  @Description
  void shouldReturnRedirect() {
    var redirectedResponse =
        givenUnauthenticatedJsonRequest()
            .when()
            .get(UNIT_PATH, "209.3.3.0")
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    assertThat(redirectedResponse.getString("cristin_unit_id")).isEqualTo("209.3.2.0");
  }

  /** Fetch a top level institution returns the institution and status {@code 200 Ok} */
  @Test
  @DisplayName("Fetch a top level institution returns the institution")
  @Description
  void shouldReturnTopLevelInstitution() {
    var topLevelInstitutionId = "209";
    var topLevelResponse =
        givenUnauthenticatedJsonRequest()
            .when()
            .get(INSTITUTION_PATH, topLevelInstitutionId)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .body()
            .asString();

    var expected = readResourceFileAsString(TOP_LEVEL_INSTITUTION_FILE);
    assertThatJson(topLevelResponse).isEqualTo(expected);
  }
}
