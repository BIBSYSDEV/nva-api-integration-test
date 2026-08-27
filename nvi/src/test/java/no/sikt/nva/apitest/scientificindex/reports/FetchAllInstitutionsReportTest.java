package no.sikt.nva.apitest.scientificindex.reports;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.sikt.nva.apitest.base.Affiliation.KRISTIANIA;
import static no.sikt.nva.apitest.base.Affiliation.OSLO_MET;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Affiliation.UIS;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.getCurrentYear;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.INSTITUTION_REPORTS_PATH;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + INSTITUTION_REPORTS_PATH)
class FetchAllInstitutionsReportTest extends ScientificIndexTestBase {

  private static final String TYPE_FIELD = "type";
  private static final String ALL_INSTITUTIONS_REPORT = "AllInstitutionsReport";

  /** The report nests each institution under its own report, so the ids are one level down. */
  private static final String INSTITUTION_IDS_FIELD = "institutions.institution.id";

  /**
   * The report for all institutions covers every institution holding candidates in the period, so
   * the institutions the test users belong to should all be reported.
   */
  @Test
  @DisplayName("Fetch report for all institutions")
  @Description(useJavaDoc = true)
  void shouldReturnReportFromAllInstitutionsForCurrentYear(SoftAssertions softly) {
    var response =
        givenAuthenticatedRequestAsUser(UIS_NVI_CURATOR)
            .when()
            .get(INSTITUTION_REPORTS_PATH, CURRENT_YEAR)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .response();

    var affiliations = List.of(UIB, UIS, KRISTIANIA, OSLO_MET);
    var affiliationValues = affiliations.stream().map(Affiliation::getValue).toList();
    var reportedInstitutions = response.jsonPath().getList(INSTITUTION_IDS_FIELD, String.class);

    softly.assertThat(response.jsonPath().getString(TYPE_FIELD)).isEqualTo(ALL_INSTITUTIONS_REPORT);
    softly.assertThat(reportedInstitutions).containsAll(affiliationValues);

    affiliations.forEach(affiliation -> assertContent(response, softly, affiliation));
  }

  private void assertContent(Response response, SoftAssertions softly, Affiliation affiliation) {
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

  /**
   * Trying to fetch report for all institutions when unauthenticated return {@code 401
   * Unauthorized}
   */
  @Test
  @DisplayName("Fetch report for all institutions when unauthenticated return Unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenNotAuthenticated() {
    givenUnauthenticatedJsonRequest()
        .when()
        .get(INSTITUTION_REPORTS_PATH, CURRENT_YEAR)
        .then()
        .statusCode(HTTP_UNAUTHORIZED);
  }

  /**
   * Trying to fetch report for all institutions when not Nvi-curator return {@code 403 Forbidden}
   */
  @ParameterizedTest
  @MethodSource("usersWithoutNviAccess")
  @DisplayName("Fetch report for all institutions when non Nvi-curator returns Forbidden")
  @Description(useJavaDoc = true)
  void shouldReturnForbiddenWhenNonNviCurator(User user) {
    givenAuthenticatedJsonRequestAsUser(user)
        .when()
        .get(INSTITUTION_REPORTS_PATH, CURRENT_YEAR)
        .then()
        .statusCode(HTTP_FORBIDDEN);
  }

  /**
   * Trying to fetch report for all institutions for a non-existing period return {@code 404 Not
   * Found}
   */
  @Test
  @DisplayName("Fetch report for all institutions for a non-existing period return Not Found")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundForNonExistingPeriod() {
    var nonExistingPeriod = getCurrentYear().plusYears(50).toString();
    givenAuthenticatedJsonRequestAsUser(UIS_NVI_CURATOR)
        .when()
        .get(INSTITUTION_REPORTS_PATH, nonExistingPeriod)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }
}
