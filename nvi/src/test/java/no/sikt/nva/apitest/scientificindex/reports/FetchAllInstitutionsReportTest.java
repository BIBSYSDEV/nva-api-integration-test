package no.sikt.nva.apitest.scientificindex.reports;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.base.Affiliation;
import static no.sikt.nva.apitest.base.Affiliation.KRISTIANIA;
import static no.sikt.nva.apitest.base.Affiliation.OSLO_MET;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Affiliation.UIS;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.getCurrentYear;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import no.sikt.nva.apitest.base.User;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_NVI_CURATOR;
import no.sikt.nva.apitest.scientificindex.NviInstitutionReports;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.INSTITUTION_REPORTS_PATH;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;

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

    affiliations.forEach(
        affiliation -> NviInstitutionReports.assertContent(response, softly, affiliation));
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
