package no.sikt.nva.apitest.scientificindex.reports;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.REPORTS_PATH;

import io.qameta.allure.Description;
import java.util.List;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.base.UserFixtures;
import no.sikt.nva.apitest.scientificindex.NviReports;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + REPORTS_PATH)
class FetchAllPeriodsReportTest extends ScientificIndexTestBase {

  /** Fetching periods report as Nvi-curator returns the report with status {@code 200 Ok} */
  @Test
  @DisplayName("Fetch periods report")
  @Description(useJavaDoc = true)
  void shouldFetchPeriodsReport(SoftAssertions softly) {

    var response =
        givenAuthenticatedRequestAsUser(UserFixtures.UIB_NVI_CURATOR)
            .when()
            .get(REPORTS_PATH)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .response();

    softly.assertThat(response.jsonPath().getString("type")).isEqualTo("AllPeriodsReport");
    softly.assertThat(response.jsonPath().getList("periods").size()).isEqualTo(3);

    List.of(NviReports.Periods.values())
        .forEach(
            period -> {
              var jsonPath = response.jsonPath().param("year", period.getYear());
              jsonPath.setRootPath("periods.find { it.period.publishingYear == year }");

              softly
                  .assertThat(jsonPath.getMap(""))
                  .as("periods exist for %s", period.getYear())
                  .isNotEmpty();
              NviReports.assertPeriodReportContent(period, jsonPath, softly);
            });
  }

  /**
   * Trying to fetch periods report while not authenticated retunr status {@code 401 Unauthorized}
   */
  @Test
  @DisplayName("Fetch periods report when unauthenticated should return Unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenNotAunthenticated() {

    givenUnauthenticatedJsonRequest().when().get(REPORTS_PATH).then().statusCode(HTTP_UNAUTHORIZED);
  }

  /** Trying to fetch periods report while not Nvi-curator returns status {@code 403 Forbidden} */
  @ParameterizedTest
  @MethodSource("usersWithoutNviReportAccess")
  @DisplayName("Fetch periods report when non-nvi-curator should return Forbidden")
  @Description(useJavaDoc = true)
  void shouldReturnForbiddenWhenNotNviCurator(User user) {

    givenAuthenticatedRequestAsUser(user)
        .when()
        .get(REPORTS_PATH)
        .then()
        .statusCode(HTTP_FORBIDDEN);
  }
}
