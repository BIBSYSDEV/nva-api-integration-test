package no.sikt.nva.apitest.scientificindex;

import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.sikt.Role.CREATOR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.candidateByPublicationIdPath;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import no.sikt.Category;
import no.sikt.Contributor;
import no.sikt.nva.PublicationFactory;
import no.sikt.nva.apitest.base.User;

public class NviCandidateFactory {

  private static final int CANDIDATE_EVALUATION_TIMEOUT_MINUTES = 5;
  private static final String IDENTIFIER_FIELD_IN_CANDIDATE = "identifier";

  private final PublicationFactory publicationFactory = new PublicationFactory();

  public NviCandidate createCandidate(String title) {
    var publicationIdentifier =
        publicationFactory.createPublishedPublication(
            UIB_CREATOR,
            title,
            Category.ACADEMIC_ARTICLE,
            List.of(new Contributor(UIB_CREATOR, CREATOR)),
            UIB_PUBLISHING_CURATOR);
    var publicationId = RestAssured.baseURI + "/publication/" + publicationIdentifier;
    var candidateIdentifier = awaitCandidate(publicationId);
    return new NviCandidate(candidateIdentifier, publicationId);
  }

  public Response fetchCandidateByPublicationId(User user, String publicationId) {
    return givenAuthenticatedJsonRequestAsUser(user)
        .urlEncodingEnabled(false)
        .get(candidateByPublicationIdPath(publicationId))
        .then()
        .extract()
        .response();
  }

  // The candidate may become fetchable (200) before the evaluator has populated its approvals and
  // points, and trailing re-evaluation events may upsert it multiple times. Waiting only for a 200
  // returns a half-evaluated candidate and makes assertions flaky, so this waits until it is fully
  // evaluated and reads the identifier from that same settled response.
  private String awaitCandidate(String publicationId) {
    var evaluatedCandidate = new AtomicReference<Response>();
    with()
        .pollInterval(fibonacci().with().unit(SECONDS))
        .ignoreExceptions()
        .await()
        .atMost(CANDIDATE_EVALUATION_TIMEOUT_MINUTES, MINUTES)
        .until(
            () -> {
              var response = fetchCandidateByPublicationId(UIB_NVI_CURATOR, publicationId);
              evaluatedCandidate.set(response);
              return isFullyEvaluated(response);
            });
    return evaluatedCandidate.get().jsonPath().getString(IDENTIFIER_FIELD_IN_CANDIDATE);
  }

  private boolean isFullyEvaluated(Response response) {
    var fullyEvaluated = false;
    if (response.statusCode() == HttpURLConnection.HTTP_OK) {
      var candidate = response.jsonPath();
      var approvals = candidate.getList("approvals");
      var totalPoints = candidate.get("totalPoints");
      fullyEvaluated =
          nonNull(approvals)
              && !approvals.isEmpty()
              && totalPoints instanceof Number points
              && points.doubleValue() > 0;
    }
    return fullyEvaluated;
  }
}
