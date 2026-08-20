package no.sikt.nva.apitest.scientificindex;

import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_STATUS_PATH;

import io.restassured.response.Response;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.Callable;
import no.sikt.nva.apitest.base.User;

public final class NviApprovals {

  public static final String APPROVED = "Approved";
  public static final String PENDING = "Pending";
  public static final String REJECTED = "Rejected";

  private NviApprovals() {}

  public static Response updateApprovalStatus(User user, NviCandidate candidate, String status) {
    var requestBody = approvalRequest(user, status);
    return pollUntil(
        putApprovalStatusRequest(user, candidate, requestBody), NviApprovals::isNotConflict);
  }

  public static Response updateApprovalStatus(
      User user, NviCandidate candidate, String status, String reason) {
    var requestBody = approvalRequest(user, status, reason);
    return pollUntil(
        putApprovalStatusRequest(user, candidate, requestBody), NviApprovals::isNotConflict);
  }

  private static Map<String, String> approvalRequest(User user, String status) {
    return Map.of("institutionId", user.affiliations().iterator().next(), "status", status);
  }

  private static Map<String, String> approvalRequest(User user, String status, String reason) {
    return Map.of(
        "institutionId", user.affiliations().iterator().next(), "status", status, "reason", reason);
  }

  public static Callable<Response> putApprovalStatusRequest(
      User user, NviCandidate candidate, Map<String, String> requestBody) {
    return () ->
        givenAuthenticatedJsonRequestAsUser(user)
            .body(requestBody)
            .put(CANDIDATE_STATUS_PATH, candidate.candidateIdentifier())
            .then()
            .extract()
            .response();
  }

  private static boolean isNotConflict(Response response) {
    return response.statusCode() != HttpURLConnection.HTTP_CONFLICT;
  }
}
