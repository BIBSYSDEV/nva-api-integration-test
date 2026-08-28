package no.sikt.nva.apitest.scientificindex.reports;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Affiliation.UIS;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.getCurrentYear;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.INSTITUTION_REPORT_PATH;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.base.User;
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
@DisplayName("GET " + INSTITUTION_REPORT_PATH)
class FetchInstitutionReportTest extends ScientificIndexTestBase {

  /** Fetch institution report for a single institution */
  @Test
  @DisplayName("Fetch institution report")
  @Description(useJavaDoc = true)
  void shouldReturnInstitutionReport(SoftAssertions softly) {

    var jsonPath =
        givenAuthenticatedRequestAsUser(UIS_NVI_CURATOR)
            .when()
            .get(INSTITUTION_REPORT_PATH, CURRENT_YEAR, UIS.getCristinId())
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    NviReports.assertInstitutionReportContent(jsonPath, softly, UIS);
  }

  /** Fetch report when user doesn't have the role Nvi-curator returns {@code 403 Forbidden} */
  @ParameterizedTest
  @MethodSource("usersWithoutNviAccess")
  @DisplayName("Fetch institution report when not Nvi-curator returns Forbidden")
  @Description(useJavaDoc = true)
  void shouldReturnForbiddenWhenNonNviCuratorFetchInstitutionReport(
      User user, SoftAssertions softly) {

    givenAuthenticatedRequestAsUser(user)
        .when()
        .get(INSTITUTION_REPORT_PATH, CURRENT_YEAR, UIB.getCristinId())
        .then()
        .statusCode(HTTP_FORBIDDEN);
  }

  /** Fetch institution report for non-existing institution return {@code 404 Not Found} */
  @Test
  @DisplayName("Fetch institution report for non-existing institution returns Not Found")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenFetchingReportForNonExistingInstitution() {

    givenAuthenticatedRequestAsUser(UIS_NVI_CURATOR)
        .when()
        .get(INSTITUTION_REPORT_PATH, CURRENT_YEAR, "1.0.0.0")
        .then()
        .statusCode(HTTP_NOT_FOUND)
        .extract()
        .jsonPath();
  }

  /** Fetch institution report for non-existing period return {@code 404 Not Found} */
  @Test
  @DisplayName("Fetch institution report for non-existing period returns Not Found")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenFetchingReportForNonExistingPeriod() {

    var nonExistingPeriod = getCurrentYear().plusYears(10).toString();

    givenAuthenticatedRequestAsUser(UIS_NVI_CURATOR)
        .when()
        .get(INSTITUTION_REPORT_PATH, nonExistingPeriod, UIS.getCristinId())
        .then()
        .statusCode(HTTP_NOT_FOUND)
        .extract()
        .jsonPath();
  }
}
