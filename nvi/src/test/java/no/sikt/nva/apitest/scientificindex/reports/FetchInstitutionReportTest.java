package no.sikt.nva.apitest.scientificindex.reports;

import static java.net.HttpURLConnection.HTTP_OK;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import static no.sikt.nva.apitest.base.Affiliation.UIS;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_NVI_CURATOR;
import no.sikt.nva.apitest.scientificindex.NviInstitutionReports;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.INSTITUTION_REPORT_PATH;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + INSTITUTION_REPORT_PATH)
class FetchInstitutionReportTest extends ScientificIndexTestBase {

  // TODO: Add tests

  @Test
  @DisplayName("Fetch institution report")
  @Description(useJavaDoc = true)
  void shouldReturnInstitutionReport(SoftAssertions softly) {

    var response =
        givenAuthenticatedRequestAsUser(UIS_NVI_CURATOR)
            .when()
            .get(INSTITUTION_REPORT_PATH, CURRENT_YEAR, UIS.getCristinId())
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .response();

    NviInstitutionReports.assertContent(response, softly, UIS);
  }
}
