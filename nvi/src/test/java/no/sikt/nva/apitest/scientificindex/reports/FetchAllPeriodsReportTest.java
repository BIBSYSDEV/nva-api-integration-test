package no.sikt.nva.apitest.scientificindex.reports;

import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.getCurrentYear;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.REPORTS_PATH;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.base.UserFixtures;
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

  private static final String PREVIOUS_PERIOD = "Last year";
  private static final String THIS_PERIOD = "This year";
  private static final String NEXT_PERIOD = "Next year";

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
            .statusCode(200)
            .extract()
            .response();

    softly.assertThat(response.jsonPath().getString("type")).isEqualTo("AllPeriodsReport");
    softly.assertThat(response.jsonPath().getList("periods").size()).isEqualTo(3);

    var years =
        Map.of(
            PREVIOUS_PERIOD, getCurrentYear().minusYears(1).toString(),
            THIS_PERIOD, CURRENT_YEAR,
            NEXT_PERIOD, getCurrentYear().plusYears(1).toString());

    years
        .entrySet()
        .forEach(entry -> assertContent(entry.getKey(), entry.getValue(), response, softly));
  }

  private void assertContent(String period, String year, Response response, SoftAssertions softly) {

    var periodPaths = List.of("id", "startDate", "reportingDate");

    var totalsPaths =
        List.of("validPoints", "disputedCount", "undisputedProcessedCount", "undisputedTotalCount");

    var byGlobalApprovalStatusPaths = List.of("dispute", "pending", "rejected", "approved");

    var jsonPath = response.jsonPath().param("year", year);

    softly
        .assertThat(jsonPath.getString("periods.find { it.period.publishingYear == year }"))
        .isNotEmpty();
    softly
        .assertThat(jsonPath.getString("periods.find { it.period.publishingYear == year }.totals"))
        .isNotEmpty();
    softly
        .assertThat(
            jsonPath.getString(
                "periods.find { it.period.publishingYear == year }.byGlobalApprovalStatus"))
        .isNotEmpty();

    softly
        .assertThat(
            jsonPath.getString("periods.find { it.period.publishingYear == year }.period.type"))
        .isEqualTo("NviPeriod");
    periodPaths.forEach(
        path ->
            softly
                .assertThat(
                    jsonPath.getString(
                        String.format(
                            "periods.find { it.period.publishingYear == year }.period.%s", path)))
                .isNotEmpty());

    softly
        .assertThat(
            jsonPath.getString("periods.find { it.period.publishingYear == year }.totals.type"))
        .isEqualTo("PeriodTotals");
    totalsPaths.forEach(
        path ->
            softly
                .assertThat(
                    jsonPath.getString(
                        String.format(
                            "periods.find { it.period.publishingYear == year }.totals.%s", path)))
                .isNotEmpty());
    softly
        .assertThat(
            jsonPath.getString(
                "periods.find { it.period.publishingYear == year }.byGlobalApprovalStatus.type"))
        .isEqualTo("CandidatesByGlobalApprovalStatus");
    byGlobalApprovalStatusPaths.forEach(
        path ->
            softly
                .assertThat(
                    jsonPath.getString(
                        String.format(
                            "periods.find { it.period.publishingYear == year"
                                + " }.byGlobalApprovalStatus.%s",
                            path)))
                .isNotEmpty());

    switch (period) {
      case PREVIOUS_PERIOD ->
          softly
              .assertThat(
                  jsonPath.getString(
                      "periods.find { it.period.publishingYear == year }.period.status"))
              .isEqualTo("ClosedPeriod");
      case THIS_PERIOD ->
          softly
              .assertThat(
                  jsonPath.getString(
                      "periods.find { it.period.publishingYear == year }.period.status"))
              .isEqualTo("OpenPeriod");
      case NEXT_PERIOD ->
          softly
              .assertThat(
                  jsonPath.getString(
                      "periods.find { it.period.publishingYear == year }.period.status"))
              .isEqualTo("UnopenedPeriod");
      default -> throw new AssertionError();
    }
  }

  /**
   * Trying to fetch periods report while not authenticated retunr status {@code 401 Unauthorized}
   */
  @Test
  @DisplayName("Fetch periods report when unauthenticated should return Unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenNotAunthenticated() {

    givenUnauthenticatedJsonRequest().when().get(REPORTS_PATH).then().statusCode(401);
  }

  /** Trying to fetch periods report while not Nvi-curator returns status {@code 403 Forbidden} */
  @ParameterizedTest
  @MethodSource("usersWithoutNviAccess")
  @DisplayName("Fetch periods report when non-nvi-curator should return Forbidden")
  @Description(useJavaDoc = true)
  void shouldReturnForbiddenWhenNotNviCurator(User user) {

    givenAuthenticatedRequestAsUser(user).when().get(REPORTS_PATH).then().statusCode(403);
  }
}
