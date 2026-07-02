package no.sikt.nva.apitest.scientificindex.candidate;

import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.candidateStatusPath;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

// Each test method creates its own NVI candidate, and running them concurrently fires a burst of
// asynchronous evaluations that backs up the pipeline and makes candidate creation time out. Run
// this class's methods on a single thread so at most one candidate is being evaluated at a time.
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(SoftAssertionsExtension.class)
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class UpdateApprovalStatusApiTest extends ScientificIndexTestBase {

  private static final String APPROVED = "Approved";
  private static final String PENDING = "Pending";
  private static final String REJECTED = "Rejected";
  private static final String REJECTION_REASON = "Rejected by API integration test";
  private static final Duration CONFLICT_RETRY_TIMEOUT = Duration.ofMinutes(2);

  @InjectSoftAssertions private SoftAssertions softly;

  @Test
  @DisplayName("Approve candidate as NVI curator")
  @Description("Approving a candidate as an NVI curator returns 200 OK")
  void shouldApproveCandidateWhenRequestedByNviCurator() {
    var candidate = createCandidate();

    var response =
        updateApprovalStatus(UIB_NVI_CURATOR, candidate, approvalRequest(APPROVED))
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("approvals[0].status")).isEqualTo(APPROVED);
    softly.assertThat(response.getString("approvals[0].finalizedBy")).isNotEmpty();
    softly.assertThat(response.getString("approvals[0].finalizedDate")).isNotEmpty();
  }

  @Test
  @DisplayName("Reject candidate with reason")
  @Description("Rejecting a candidate with a reason returns 200 OK")
  void shouldRejectCandidateWhenReasonIsProvided() {
    var candidate = createCandidate();

    var response =
        updateApprovalStatus(
                UIB_NVI_CURATOR, candidate, approvalRequestWithReason(REJECTED, REJECTION_REASON))
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("approvals[0].status")).isEqualTo(REJECTED);
    softly.assertThat(response.getString("approvals[0].reason")).isEqualTo(REJECTION_REASON);
    softly.assertThat(response.getString("approvals[0].finalizedBy")).isNotEmpty();
  }

  @Test
  @DisplayName("Reject candidate without reason")
  @Description("Rejecting a candidate without a reason returns 400 Bad Request")
  void shouldReturnBadRequestWhenRejectingWithoutReason() {
    var candidate = createCandidate();

    updateApprovalStatus(UIB_NVI_CURATOR, candidate, approvalRequest(REJECTED))
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("Reset approved candidate to pending")
  @Description("Reverting an approved candidate to Pending stays Pending, not New")
  void shouldResetApprovalWhenApprovedCandidateIsSetToPending() {
    var candidate = createCandidate();

    updateApprovalStatus(UIB_NVI_CURATOR, candidate, approvalRequest(APPROVED))
        .then()
        .statusCode(200);

    var response =
        updateApprovalStatus(UIB_NVI_CURATOR, candidate, approvalRequest(PENDING))
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("approvals[0].status")).isEqualTo(PENDING);
    softly.assertThat(response.getString("approvals[0].finalizedBy")).isNull();
    softly.assertThat(response.getString("approvals[0].finalizedDate")).isNull();
  }

  @Test
  @DisplayName("Approve candidate without MANAGE_NVI_CANDIDATES access right")
  @Description("Updating approval without MANAGE_NVI_CANDIDATES returns 401 Unauthorized")
  void shouldReturnUnauthorizedWhenUserLacksManageNviCandidates() {
    var candidate = createCandidate();

    updateApprovalStatus(UIB_CREATOR, candidate, approvalRequest(APPROVED)).then().statusCode(401);
  }

  private static NviCandidate createCandidate() {
    return CANDIDATE_FACTORY.createCandidate(
        "NVI integration test publication " + UUID.randomUUID());
  }

  // Trailing re-evaluation events from the publish flow can cause transient DynamoDB transaction
  // conflicts (409) right after the candidate is created, so the request is retried until it
  // returns a non-conflict response, which is then returned to the caller.
  private static Response updateApprovalStatus(
      User user, NviCandidate candidate, Map<String, Object> requestBody) {
    return pollUntil(
        CONFLICT_RETRY_TIMEOUT,
        () ->
            givenAuthenticatedJsonRequestAsUser(user)
                .body(requestBody)
                .put(candidateStatusPath(candidate.candidateIdentifier()))
                .then()
                .extract()
                .response(),
        response -> response.statusCode() != HttpURLConnection.HTTP_CONFLICT);
  }

  private static Map<String, Object> approvalRequest(String status) {
    var requestBody = new HashMap<String, Object>();
    requestBody.put("institutionId", Affiliation.UIB.getValue());
    requestBody.put("status", status);
    return requestBody;
  }

  private static Map<String, Object> approvalRequestWithReason(String status, String reason) {
    var requestBody = approvalRequest(status);
    requestBody.put("reason", reason);
    return requestBody;
  }
}
