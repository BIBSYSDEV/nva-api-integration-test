package no.sikt.nva.apitest.scientificindex.reports;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.qameta.allure.Description;
import io.restassured.response.ValidatableResponse;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.getCurrentYear;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import no.sikt.nva.apitest.base.User;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_NVI_CURATOR;
import no.sikt.nva.apitest.scientificindex.NviReports;
import static no.sikt.nva.apitest.scientificindex.NviReports.Periods.THIS_PERIOD;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.PERIOD_REPORT_PATH;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + PERIOD_REPORT_PATH)
class FetchPeriodReportTest extends ScientificIndexTestBase {

  /** Fetch report for single period */
  @Test
  @DisplayName("Fetch report for single period")
  @Description(useJavaDoc = true)
  void shouldFetchReportForSinglePeriod(SoftAssertions softly) {
    var period = THIS_PERIOD;
    var jsonPath =
        fetchPeriodReport(UIS_NVI_CURATOR, period.getYear(), HTTP_OK).extract().jsonPath();

    NviReports.assertPeriodReportContent(period, jsonPath, softly);
  }

  /** Fetch report for non-existing period returns {@code 404 Not Found} */
  @Test
  @DisplayName("Fetch report for non-existing period returns Not Found")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenFetchingReportForNonExistingPeriod() {

    var nonExistingPeriod = getCurrentYear().plusYears(15).toString();

    fetchPeriodReport(UIS_NVI_CURATOR, nonExistingPeriod, HTTP_NOT_FOUND);
  }

  /** Fetch report when not authenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Fetch report when not authenticated returns Unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenFetchingReportWhenUnauthenticated() {

    givenUnauthenticatedJsonRequest()
        .when()
        .get(PERIOD_REPORT_PATH, THIS_PERIOD.getYear())
        .then()
        .statusCode(HTTP_UNAUTHORIZED);
  }

  /** Fetch report when user doesn't have the role Nvi-curator returns {@code 403 Forbidden} */
  @ParameterizedTest
  @MethodSource("usersWithoutNviReportAccess")
  @DisplayName("Fetch institution report when not Nvi-curator returns Forbidden")
  @Description(useJavaDoc = true)
  void shouldReturnForbiddenWhenNonNvicuratorFetchPeriodReport(User user, SoftAssertions softly) {

    fetchPeriodReport(user, CURRENT_YEAR, HTTP_FORBIDDEN);
  }

  private ValidatableResponse fetchPeriodReport(User user, String year, int expectedResponseCode) {
    return givenAuthenticatedRequestAsUser(user)
        .when()
        .get(PERIOD_REPORT_PATH, year)
        .then()
        .statusCode(expectedResponseCode);
  }
}
