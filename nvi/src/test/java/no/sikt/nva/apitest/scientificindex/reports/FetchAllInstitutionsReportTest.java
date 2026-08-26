package no.sikt.nva.apitest.scientificindex.reports;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import static no.sikt.nva.apitest.base.Affiliation.KRISTIANIA;
import static no.sikt.nva.apitest.base.Affiliation.OSLO_MET;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Affiliation.UIS;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import no.sikt.nva.apitest.base.UserFixtures;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.INSTITUTION_REPORTS_PATH;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + INSTITUTION_REPORTS_PATH)
class FetchAllInstitutionsReportTest extends ScientificIndexTestBase {

  @Test
  @DisplayName("Fetch report for all institutions")
  @Description(useJavaDoc = true)
  void shouldReturnReportFromAllInstitutionsForCurrentYear(SoftAssertions softly) {
    var response = givenAuthenticatedRequestAsUser(UserFixtures.UIS_NVI_CURATOR)
    .when()
    .get(INSTITUTION_REPORTS_PATH, CURRENT_YEAR)
    .then()
    .statusCode(200).extract().response();

    var affiliations = List.of(
      UIB.getValue(),
      UIS.getValue(),
      KRISTIANIA.getValue(),
      OSLO_MET.getValue()
    );

    var jsonPath = response.jsonPath().para("affiliation", affiliation);

    softly.assertThat(response.jsonPath().getString("type")).isEqualTo("AllInstitutionsReport");
  }
}
