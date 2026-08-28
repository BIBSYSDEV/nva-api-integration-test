package no.sikt.nva.apitest.scientificindex.reports;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Affiliation.UIS;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.DEPRECATED_INSTITUTION_REPORT_PATH;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import java.util.List;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + DEPRECATED_INSTITUTION_REPORT_PATH)
class FetchDeprecatedInstitutionReportTest extends ScientificIndexTestBase {

  /** Fetch deprecated institution report for a single institution */
  @Test
  @DisplayName("Fetch deprecated institution report")
  @Description(useJavaDoc = true)
  void shouldReturnDeprecatedInstitutionReport(SoftAssertions softly) {

    var jsonPath =
        givenAuthenticatedRequestAsUser(UIS_NVI_CURATOR)
            .when()
            .get(DEPRECATED_INSTITUTION_REPORT_PATH, CURRENT_YEAR)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    assertDeprecatedInstitutionReport(jsonPath, softly);
  }

  private void assertDeprecatedInstitutionReport(JsonPath jsonPath, SoftAssertions softly) {

    softly.assertThat(jsonPath.getString("year")).isEqualTo(CURRENT_YEAR);
    softly.assertThat(jsonPath.getString("topLevelOrganizationId")).isEqualTo(UIS.getValue());
    softly.assertThat(jsonPath.getMap("totals")).isNotNull();
    softly.assertThat(jsonPath.getInt("totals.candidateCount")).isNotNull();
    softly.assertThat(jsonPath.getFloat("totals.points")).isNotNull();

    var globalApprovalStatusFields = List.of("Approved", "Pending", "Dispute", "Rejected");
    var approvalStatusFields = List.of("Approved", "Pending", "Rejected", "New");

    softly.assertThat(jsonPath.getMap("totals.globalApprovalStatus")).isNotNull();
    globalApprovalStatusFields.forEach(
        field ->
            softly
                .assertThat(jsonPath.getString("totals.globalApprovalStatus.%s".formatted(field)))
                .as("globalApprovalStatus.%s is not null", field)
                .isNotNull());

    softly.assertThat(jsonPath.getMap("totals.approvalStatus")).isNotNull();
    approvalStatusFields.forEach(
        field ->
            softly
                .assertThat(jsonPath.getString("totals.approvalStatus.%s".formatted(field)))
                .as("approvalStatus.%s is not null", field)
                .isNotNull());

    softly.assertThat(jsonPath.getMap("byOrganization")).isNotNull();
    softly
        .assertThat(
            jsonPath.getMap("byOrganization.'%s'.globalApprovalStatus".formatted(UIS.getValue())))
        .isNotNull();
    globalApprovalStatusFields.forEach(
        field ->
            softly
                .assertThat(
                    jsonPath.getString(
                        "byOrganization.'%s'.globalApprovalStatus.%s"
                            .formatted(UIS.getValue(), field)))
                .as("byOrganizationglobalApprovalStatus.%s is not null", field)
                .isNotNull());

    softly.assertThat(jsonPath.getMap("byOrganization")).isNotNull();
    softly
        .assertThat(jsonPath.getMap("byOrganization.'%s'.approvalStatus".formatted(UIS.getValue())))
        .isNotNull();
    approvalStatusFields.forEach(
        field ->
            softly
                .assertThat(
                    jsonPath.getString(
                        "byOrganization.'%s'.approvalStatus.%s".formatted(UIS.getValue(), field)))
                .as("byOrganization.approvalStatus.%s is not null", field)
                .isNotNull());
  }
}
