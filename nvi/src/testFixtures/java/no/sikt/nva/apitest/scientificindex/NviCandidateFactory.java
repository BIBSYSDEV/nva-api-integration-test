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
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.candidateSearchPath;
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
  private static final int CANDIDATE_INDEXING_TIMEOUT_MINUTES = 5;
  private static final int SEARCH_PAGE_SIZE = 100;
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
    return new NviCandidate(candidateIdentifier, publicationId, title, UIB_CREATOR.name());
  }

  public Response fetchCandidateByPublicationId(User user, String publicationId) {
    return givenAuthenticatedJsonRequestAsUser(user)
        .urlEncodingEnabled(false)
        .get(candidateByPublicationIdPath(publicationId))
        .then()
        .extract()
        .response();
  }

  public Response searchCandidates(User curator, String query) {
    return givenAuthenticatedJsonRequestAsUser(curator)
        .queryParam("query", query)
        .queryParam("size", SEARCH_PAGE_SIZE)
        .get(candidateSearchPath())
        .then()
        .extract()
        .response();
  }

  /**
   * Polls the candidate search by title until the candidate is indexed, since indexing runs
   * asynchronously after evaluation.
   *
   * @return the settled search response containing the candidate
   */
  public Response awaitCandidateInSearchIndex(User curator, NviCandidate candidate) {
    var settledResponse = new AtomicReference<Response>();
    with()
        .pollInterval(fibonacci().with().unit(SECONDS))
        .ignoreExceptions()
        .await()
        .atMost(CANDIDATE_INDEXING_TIMEOUT_MINUTES, MINUTES)
        .until(
            () -> {
              var response = searchCandidates(curator, candidate.title());
              settledResponse.set(response);
              return isCandidateInSearchHits(response, candidate.publicationId());
            });
    return settledResponse.get();
  }

  /**
   * Returns a GPath expression selecting the search hit for the given publication id, independent
   * of result ordering.
   */
  public static String indexedCandidateByPublicationId(String publicationId) {
    return "hits.find { it.publicationDetails?.id == '%s' }".formatted(publicationId);
  }

  /** Returns the publication ids of every hit in a search response. */
  public static List<String> indexedPublicationIds(Response response) {
    return response.jsonPath().getList("hits.publicationDetails.id", String.class);
  }

  private static boolean isCandidateInSearchHits(Response response, String publicationId) {
    var indexed = false;
    if (response.statusCode() == HttpURLConnection.HTTP_OK) {
      var matchingHit = response.jsonPath().getMap(indexedCandidateByPublicationId(publicationId));
      indexed = nonNull(matchingHit);
    }
    return indexed;
  }

  /**
   * Waits until the candidate is fully evaluated, not just fetchable, since a 200 response may
   * arrive before the evaluator has populated approvals and points.
   *
   * @return the candidate identifier from the settled response
   */
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
