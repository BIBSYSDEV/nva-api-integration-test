package no.sikt.nva.apitest.scientificindex;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.sikt.Role.CREATOR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.CONTEXT_FIELD;
import static no.sikt.nva.apitest.publication.PublicationFields.ENTITY_DESCRIPTION_FIELD;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.candidateByPublicationIdPath;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import no.sikt.Category;
import no.sikt.Contributor;
import no.sikt.nva.PublicationFactory;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.base.User;

public class NviCandidateFactory {

  private static final int CANDIDATE_EVALUATION_TIMEOUT_MINUTES = 3;
  private static final String ACCESS_TOKEN_KEY = "accessToken";
  private static final String IDENTIFIER_FIELD_IN_CANDIDATE = "identifier";

  private final PublicationFactory publicationFactory = new PublicationFactory();

  public NviCandidate createCandidate(String title) {
    var publicationIdentifier = createNviEligiblePublication(title);
    var publicationId = RestAssured.baseURI + "/publication/" + publicationIdentifier;
    var candidateIdentifier = awaitCandidate(publicationId);
    return new NviCandidate(candidateIdentifier, publicationId, publicationIdentifier);
  }

  public Response fetchCandidateByPublicationId(User user, String publicationId) {
    return givenAuthenticatedJsonRequestAsUser(user)
        .urlEncodingEnabled(false)
        .get(candidateByPublicationIdPath(publicationId))
        .then()
        .extract()
        .response();
  }

  private String createNviEligiblePublication(String title) {
    var createResponse = publicationFactory.createDraftPublication(UIB_CREATOR);
    var publicationIdentifier = createResponse.jsonPath().getString(IDENTIFIER_FIELD);
    Map<String, Object> publication = createResponse.jsonPath().getMap("");
    publication.remove(CONTEXT_FIELD);

    var entityDescription =
        publicationFactory.createEntityDescription(
            title, Category.ACADEMIC_ARTICLE, List.of(new Contributor(UIB_CREATOR, CREATOR)));
    publication.put(ENTITY_DESCRIPTION_FIELD, entityDescription);

    publicationFactory.updatePublication(UIB_CREATOR, publication);
    publicationFactory.publish(UIB_PUBLISHING_CURATOR, publicationIdentifier);
    return publicationIdentifier;
  }

  private String awaitCandidate(String publicationId) {
    var accessToken = CognitoLogin.login(UIB_NVI_CURATOR.userId()).get(ACCESS_TOKEN_KEY);
    with()
        .pollInterval(fibonacci().with().unit(SECONDS))
        .await()
        .atMost(CANDIDATE_EVALUATION_TIMEOUT_MINUTES, MINUTES)
        .until(() -> fetchCandidate(accessToken, publicationId).statusCode() == 200);
    return fetchCandidate(accessToken, publicationId)
        .jsonPath()
        .getString(IDENTIFIER_FIELD_IN_CANDIDATE);
  }

  private Response fetchCandidate(String accessToken, String publicationId) {
    return givenAuthenticatedJsonRequest(accessToken)
        .urlEncodingEnabled(false)
        .get(candidateByPublicationIdPath(publicationId))
        .then()
        .extract()
        .response();
  }
}
