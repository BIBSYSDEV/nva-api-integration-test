package no.sikt.nva.apitest.scientificindex.candidate;

import static java.util.concurrent.TimeUnit.SECONDS;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.candidateStatusPath;
import static org.awaitility.Awaitility.with;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
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

@ExtendWith(SoftAssertionsExtension.class)
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class UpdateApprovalStatusApiTest extends ScientificIndexTestBase {

  private static final String APPROVED = "Approved";
  private static final String PENDING = "Pending";
  private static final String REJECTED = "Rejected";
  private static final String REJECTION_REASON = "Rejected by API integration test";

  @InjectSoftAssertions private SoftAssertions softly;

  @Test
  @DisplayName("Approve candidate as NVI curator")
  @Description(
      "Updating the approval status to Approved as an NVI curator for the curator's own"
          + " institution should return the updated candidate and statuscode 200 OK")
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
  @Description(
      "Updating the approval status to Rejected with a reason should return the updated candidate"
          + " and statuscode 200 OK")
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
  @Description(
      "Updating the approval status to Rejected without a reason should return statuscode 400 Bad"
          + " Request")
  void shouldReturnBadRequestWhenRejectingWithoutReason() {
    var candidate = createCandidate();

    updateApprovalStatus(UIB_NVI_CURATOR, candidate, approvalRequest(REJECTED))
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("Reset approved candidate to pending")
  @Description(
      "Updating the approval status back to Pending after approving should reset the approval."
          + " The status stays Pending rather than New because approving assigned the curator.")
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
  @Description(
      "Updating the approval status as a user without the MANAGE_NVI_CANDIDATES access right"
          + " should return statuscode 401 Unauthorized")
  void shouldReturnUnauthorizedWhenUserLacksManageNviCandidates() {
    var candidate = createCandidate();

    updateApprovalStatus(UIB_CREATOR, candidate, approvalRequest(APPROVED)).then().statusCode(401);
  }

  private static NviCandidate createCandidate() {
    return CANDIDATE_FACTORY.createCandidate(
        "NVI integration test publication " + UUID.randomUUID());
  }

  // Trailing re-evaluation events from the publish flow can cause transient DynamoDB transaction
  // conflicts (409) right after the candidate is created, so conflicts are retried
  private static Response updateApprovalStatus(
      User user, NviCandidate candidate, Map<String, Object> requestBody) {
    var latestResponse = new AtomicReference<Response>();
    with()
        .pollInterval(2, SECONDS)
        .await()
        .atMost(30, SECONDS)
        .until(
            () -> {
              var response = putApprovalStatus(user, candidate, requestBody);
              latestResponse.set(response);
              return response.statusCode() != 409;
            });
    return latestResponse.get();
  }

  private static Response putApprovalStatus(
      User user, NviCandidate candidate, Map<String, Object> requestBody) {
    return givenAuthenticatedJsonRequestAsUser(user)
        .body(requestBody)
        .put(candidateStatusPath(candidate.candidateIdentifier()))
        .then()
        .extract()
        .response();
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
