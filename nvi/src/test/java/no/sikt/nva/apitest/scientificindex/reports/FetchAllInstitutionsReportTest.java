package no.sikt.nva.apitest.scientificindex.reports;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Affiliation.KRISTIANIA;
import static no.sikt.nva.apitest.base.Affiliation.OSLO_MET;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Affiliation.UIS;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.INSTITUTION_REPORTS_PATH;

import io.qameta.allure.Description;
import java.util.List;
import no.sikt.nva.apitest.base.UserFixtures;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
        givenAuthenticatedRequestAsUser(UserFixtures.UIS_NVI_CURATOR)
            .when()
            .get(INSTITUTION_REPORTS_PATH, CURRENT_YEAR)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .response();

    var affiliations =
        List.of(UIB.getValue(), UIS.getValue(), KRISTIANIA.getValue(), OSLO_MET.getValue());
    var reportedInstitutions = response.jsonPath().getList(INSTITUTION_IDS_FIELD, String.class);

    softly.assertThat(response.jsonPath().getString(TYPE_FIELD)).isEqualTo(ALL_INSTITUTIONS_REPORT);
    softly.assertThat(reportedInstitutions).containsAll(affiliations);
  }
}
