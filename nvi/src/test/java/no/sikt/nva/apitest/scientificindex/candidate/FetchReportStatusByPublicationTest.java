package no.sikt.nva.apitest.scientificindex.candidate;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.UUID.randomUUID;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.SettledCondition.settledWhen;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.NviApprovals.APPROVED;
import static no.sikt.nva.apitest.scientificindex.NviApprovals.updateApprovalStatus;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.PUBLICATION_REPORT_STATUS_PATH;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.concurrent.Callable;
import no.sikt.Contributor;
import no.sikt.nva.apitest.base.SettledCondition;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + PUBLICATION_REPORT_STATUS_PATH)
class FetchReportStatusByPublicationTest extends ScientificIndexTestBase {

  private static final String STATUS_FIELD = "reportStatus.status";
  private static final String STATUS_DESCRIPTION_FIELD = "reportStatus.description";

  private static JsonPath fetchReportStatus(
      String publicationIdentifier, ReportStatus expectedStatus) {
    return pollUntil(
        fetchReportStatusRequest(publicationIdentifier), hasExpectedStatus(expectedStatus));
  }

  private static Callable<JsonPath> fetchReportStatusRequest(String publicationIdentifier) {
    return () ->
        givenUnauthenticatedJsonRequest()
            .get(PUBLICATION_REPORT_STATUS_PATH, publicationIdentifier)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();
  }

  private static SettledCondition<JsonPath> hasExpectedStatus(ReportStatus expectedStatus) {
    return settledWhen(
        STATUS_FIELD, expectedStatus.name(), response -> response.getString(STATUS_FIELD));
  }

  private static void assertReportStatus(
      SoftAssertions softly, JsonPath response, NviCandidate candidate, ReportStatus expected) {
    softly.assertThat(response.getString("publicationId")).isEqualTo(candidate.publicationId());
    softly.assertThat(response.getString(STATUS_FIELD)).isEqualTo(expected.name());
    softly
        .assertThat(response.getString(STATUS_DESCRIPTION_FIELD))
        .isEqualTo(expected.description());
    softly.assertThat(response.getString("period")).isEqualTo(CURRENT_YEAR);
  }

  private static String title() {
    return "NVI - Fetch report status test - %s".formatted(randomUUID());
  }

  /**
   * Fetching the report status of a publication that doesn't exist returns status {@code 200 OK}
   * and placeholder response.
   */
  @Test
  @DisplayName("Fetch report status for non-existent publication")
  @Description(useJavaDoc = true)
  void shouldReturnReportStatusWhenPublicationDoesNotExist(SoftAssertions softly) {
    var expected = ReportStatus.NOT_CANDIDATE;
    var publicationIdentifier = randomUUID().toString();
    var response = fetchReportStatus(publicationIdentifier, expected);

    softly.assertThat(response.getString("publicationId")).contains(publicationIdentifier);
    softly.assertThat(response.getString(STATUS_FIELD)).isEqualTo(expected.name());
    softly
        .assertThat(response.getString(STATUS_DESCRIPTION_FIELD))
        .isEqualTo(expected.description());
  }

  /** Fetching the report status of a new candidate publication returns PENDING_REVIEW. */
  @Test
  @DisplayName("Fetch report status for new candidate")
  @Description(useJavaDoc = true)
  void shouldReturnReportStatusForNewCandidate(SoftAssertions softly) {
    var contributors = List.of(Contributor.asCreator(UIB_CREATOR));
    var candidate =
        CANDIDATE_FACTORY.createCandidate(title(), UIB_PUBLISHING_CURATOR, contributors);

    var expected = ReportStatus.PENDING_REVIEW;
    var response = fetchReportStatus(candidate.publicationIdentifier(), expected);

    assertReportStatus(softly, response, candidate, expected);
  }

  /** Fetching the report status of an approved candidate publication returns APPROVED. */
  @Test
  @DisplayName("Fetch report status for approved candidate")
  @Description(useJavaDoc = true)
  void shouldReturnReportStatusForApprovedCandidate(SoftAssertions softly) {
    var contributors = List.of(Contributor.asCreator(UIB_CREATOR));
    var candidate =
        CANDIDATE_FACTORY.createCandidate(title(), UIB_PUBLISHING_CURATOR, contributors);
    updateApprovalStatus(UIB_NVI_CURATOR, candidate, "Approved");

    var expected = ReportStatus.APPROVED;
    var response = fetchReportStatus(candidate.publicationIdentifier(), expected);

    assertReportStatus(softly, response, candidate, expected);
  }

  /** Fetching the report status of a rejected candidate publication returns REJECTED. */
  @Test
  @DisplayName("Fetch report status for rejected candidate")
  @Description(useJavaDoc = true)
  void shouldReturnReportStatusForRejectedCandidate(SoftAssertions softly) {
    var contributors = List.of(Contributor.asCreator(UIB_CREATOR));
    var candidate =
        CANDIDATE_FACTORY.createCandidate(title(), UIB_PUBLISHING_CURATOR, contributors);
    updateApprovalStatus(UIB_NVI_CURATOR, candidate, "Rejected", "Doesn't have enough pictures");

    var expected = ReportStatus.REJECTED;
    var response = fetchReportStatus(candidate.publicationIdentifier(), expected);

    assertReportStatus(softly, response, candidate, expected);
  }

  /** Fetching the report status of a disputed candidate publication returns UNDER_REVIEW. */
  @Test
  @DisplayName("Fetch report status for disputed candidate")
  @Description(useJavaDoc = true)
  void shouldReturnReportStatusForDisputedCandidate(SoftAssertions softly) {
    var contributors =
        List.of(Contributor.asCreator(UIB_CREATOR), Contributor.asCreator(UIS_CREATOR));
    var candidate =
        CANDIDATE_FACTORY.createCandidate(title(), UIB_PUBLISHING_CURATOR, contributors);
    updateApprovalStatus(UIB_NVI_CURATOR, candidate, "Rejected", "Doesn't have enough pictures");
    updateApprovalStatus(UIS_NVI_CURATOR, candidate, APPROVED);

    var expected = ReportStatus.UNDER_REVIEW;
    var response = fetchReportStatus(candidate.publicationIdentifier(), expected);

    assertReportStatus(softly, response, candidate, expected);
  }

  enum ReportStatus {
    PENDING_REVIEW("Pending review. Awaiting approval from all institutions"),
    UNDER_REVIEW("Under review. At least one institution has approved/rejected"),
    APPROVED("Approved by all involved institutions in open period"),
    REJECTED("Rejected by all involved institutions in open period"),
    NOT_CANDIDATE("Not a candidate");

    private final String statusDescription;

    ReportStatus(String statusDescription) {
      this.statusDescription = statusDescription;
    }

    String description() {
      return statusDescription;
    }
  }
}
