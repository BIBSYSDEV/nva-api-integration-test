package no.sikt.nva.apitest.scientificindex;

import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.getCurrentYear;

import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.Map;
import no.sikt.nva.apitest.base.Affiliation;
import org.assertj.core.api.SoftAssertions;

public final class NviReports {

  public enum Periods {
    PREVIOUS_PERIOD("Last year", getCurrentYear().minusYears(1).toString()),
    THIS_PERIOD("This year", CURRENT_YEAR),
    NEXT_PERIOD("Next year", getCurrentYear().plusYears(1).toString());

    private final String description;
    private final String year;

    Periods(String description, String year) {
      this.description = description;
      this.year = year;
    }

    public String getDescription() {
      return description;
    }

    public String getYear() {
      return year;
    }
  }

  private static final String PERIOD_STATUS = "period.status";

  private NviReports() {}

  public static void assertInstitutionReportContent(
      JsonPath jsonPath, SoftAssertions softly, Affiliation affiliation) {

    var valueAssertions =
        Map.of(
            "type",
            "InstitutionReport",
            "period.type",
            "NviPeriod",
            "period.publishingYear",
            CURRENT_YEAR,
            PERIOD_STATUS,
            "OpenPeriod",
            "sector",
            "UHI",
            "institution.type",
            "Organization",
            "institutionSummary.type",
            "InstitutionSummary",
            "institutionSummary.totals.type",
            "InstitutionTotals");

    valueAssertions
        .entrySet()
        .forEach(
            (entry) -> {
              softly
                  .assertThat(jsonPath.getString(entry.getKey()))
                  .as("%s for year %s and %s", entry.getKey(), CURRENT_YEAR, affiliation.name())
                  .isEqualTo(entry.getValue());
            });

    var notEmptyMapAssertions =
        List.of(
            "period",
            "institution",
            "institution.labels",
            "institutionSummary",
            "institutionSummary.totals");

    notEmptyMapAssertions.forEach(
        path ->
            softly
                .assertThat(jsonPath.getMap(path))
                .as("%s for year %s and %s", path, CURRENT_YEAR, affiliation.name())
                .isNotEmpty());

    softly
        .assertThat(jsonPath.getFloat("institutionSummary.totals.validPoints"))
        .as(
            "InstitutionSummary.totals.validPoints for year %s and %s",
            CURRENT_YEAR, affiliation.name())
        .isNotNaN();
    var totalsFields =
        List.of(
            "disputedCount",
            "globalApprovedCount",
            "globalRejectedCount",
            "undisputedProcessedCount",
            "undisputedTotalCount");
    totalsFields.forEach(
        field ->
            softly
                .assertThat(jsonPath.getInt("institutionSummary.totals.%s".formatted(field)))
                .as(
                    "institutionSummary.totals.%s for year %s and %s",
                    field, CURRENT_YEAR, affiliation.name())
                .isNotNull());
    var byLocalApprovalStatusFields = List.of("new", "pending", "approved", "rejected");
    byLocalApprovalStatusFields.forEach(
        field ->
            softly
                .assertThat(
                    jsonPath.getInt("institutionSummary.byLocalApprovalStatus.%s".formatted(field)))
                .as(
                    "institutionSummary.byLocalApprovalStatus.%s for year %s and %s",
                    field, CURRENT_YEAR, affiliation.name())
                .isNotNull());

    softly
        .assertThat(jsonPath.getList("units"))
        .as("units for year %s and %s", CURRENT_YEAR, affiliation.name())
        .isNotNull();
  }

  public static void assertPeriodReportContent(
      Periods period, JsonPath jsonPath, SoftAssertions softly) {

    var year = period.getYear();
    var periodPaths = List.of("id", "startDate", "reportingDate");

    var totalsPaths =
        List.of("validPoints", "disputedCount", "undisputedProcessedCount", "undisputedTotalCount");

    var byGlobalApprovalStatusPaths = List.of("dispute", "pending", "rejected", "approved");

    softly.assertThat(jsonPath.getMap("period")).as("period exists for %s", year).isNotEmpty();
    softly.assertThat(jsonPath.getMap("totals")).as("totals exists for %s", year).isNotEmpty();
    softly
        .assertThat(jsonPath.getMap("byGlobalApprovalStatus"))
        .as("byGlobalApprovalStatus exists for %s", year)
        .isNotEmpty();

    softly
        .assertThat(jsonPath.getString("period.type"))
        .as("period.type for %s", year)
        .isEqualTo("NviPeriod");
    periodPaths.forEach(
        path ->
            softly
                .assertThat(jsonPath.getString("period.%s".formatted(path)))
                .as("period.%s for %s", path, year)
                .isNotEmpty());

    softly
        .assertThat(jsonPath.getString("totals.type"))
        .as("period.type for %s", year)
        .isEqualTo("PeriodTotals");
    totalsPaths.forEach(
        path ->
            softly
                .assertThat(jsonPath.getString("totals.%s".formatted(path)))
                .as("period.%s for %s", path, year)
                .isNotEmpty());

    softly
        .assertThat(jsonPath.getString("byGlobalApprovalStatus.type"))
        .as("period.type for %s", year)
        .isEqualTo("CandidatesByGlobalApprovalStatus");
    byGlobalApprovalStatusPaths.forEach(
        path ->
            softly
                .assertThat(jsonPath.getString("byGlobalApprovalStatus.%s".formatted(path)))
                .as("period.%s for %s", path, year)
                .isNotEmpty());

    switch (period) {
      case PREVIOUS_PERIOD ->
          softly
              .assertThat(jsonPath.getString(PERIOD_STATUS))
              .as("period.status for %s (%s)", period.getDescription(), year)
              .isEqualTo("ClosedPeriod");
      case THIS_PERIOD ->
          softly
              .assertThat(jsonPath.getString(PERIOD_STATUS))
              .as("period.status for %s (%s)", period.getDescription(), year)
              .isEqualTo("OpenPeriod");
      case NEXT_PERIOD ->
          softly
              .assertThat(jsonPath.getString(PERIOD_STATUS))
              .as("period.status for %s (%s)", period.getDescription(), year)
              .isEqualTo("UnopenedPeriod");
    }
  }
}
