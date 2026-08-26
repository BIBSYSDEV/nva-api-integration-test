package no.sikt.nva.apitest.scientificindex.reports;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import no.sikt.nva.apitest.base.Affiliation;
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
  void shouldReturnReportFromAllInstitutionsForCurrentPeriod(SoftAssertions softly) {
    var response = givenAuthenticatedRequestAsUser(UserFixtures.UIS_NVI_CURATOR)
    .when()
    .get(INSTITUTION_REPORTS_PATH, CURRENT_YEAR)
    .then()
    .statusCode(200).extract().response();

    softly.assertThat(response.jsonPath().getString("type")).isEqualTo("AllInstitutionsReport");

    var affiliations = List.of(
      UIB,
      UIS,
      KRISTIANIA,
      OSLO_MET
    );

    affiliations.forEach(affiliation -> assertContent(response, softly, affiliation));
  }
  
  private void assertContent(Response response, SoftAssertions softly, Affiliation affiliation) {
    var jsonPath = response.jsonPath().param("affiliation", affiliation.getValue()).setRootPath("institutions.find {it.id == affiliation} ");

    softly.assertThat(jsonPath.getMap("period"))
      .as("period for year %s and %s", CURRENT_YEAR, affiliation.name()).isNotEmpty();
    softly.assertThat(jsonPath.getString("sector"))
      .as("sector for year %s and %s", CURRENT_YEAR, affiliation.name()).isEqualTo("UHI");
  }

  // test for unauthenticated users
  // test for non-existing period
}
