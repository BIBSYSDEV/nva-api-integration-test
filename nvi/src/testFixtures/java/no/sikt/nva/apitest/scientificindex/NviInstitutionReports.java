package no.sikt.nva.apitest.scientificindex;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;

import io.restassured.response.Response;
import no.sikt.nva.apitest.base.Affiliation;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;

public final class NviInstitutionReports {

  public static void assertContent(
      Response response, SoftAssertions softly, Affiliation affiliation) {
    var jsonPath =
        response
            .jsonPath()
            .param("affiliation", affiliation.getValue())
            .setRootPath("institutions.find {it.institution.id == affiliation} ");

    var valueAssertions =
        Map.of(
            "type", "InstitutionReport",
            "period.type", "NviPeriod",
            "period.publishingYear", CURRENT_YEAR,
            "period.status", "OpenPeriod",
            "sector", "UHI",
            "institution.type", "Organization",
            "institutionSummary.type", "InstitutionSummary",
            "institutionSummary.totals.type", "InstitutionTotals");

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
}
