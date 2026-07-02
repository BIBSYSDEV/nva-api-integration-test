package no.sikt.nva.apitest.scientificindex.candidate;

import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.NviCandidateFactory.indexedCandidateByPublicationId;
import static no.sikt.nva.apitest.scientificindex.NviCandidateFactory.indexedPublicationIds;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.candidateSearchPath;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.UUID;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.UnitTestContainsTooManyAsserts"})
class SearchNviCandidateApiTest extends ScientificIndexTestBase {

  private static final int SEARCH_PAGE_SIZE = 100;

  @InjectSoftAssertions private SoftAssertions softly;

  private static NviCandidate candidate;
  private static Response indexedCandidateResponse;

  @BeforeAll
  static void createAndIndexCandidate() {
    candidate =
        CANDIDATE_FACTORY.createCandidate("NVI integration test publication " + UUID.randomUUID());
    indexedCandidateResponse =
        CANDIDATE_FACTORY.awaitCandidateInSearchIndex(UIB_NVI_CURATOR, candidate);
  }

  @Test
  @DisplayName("Candidate is indexed and searchable with its NVI data")
  @Description("A new candidate is returned by search with creator, approval and points")
  void shouldIndexCandidateForSearchWhenPublicationBecomesCandidate() {
    var json =
        indexedCandidateResponse
            .jsonPath()
            .setRootPath(indexedCandidateByPublicationId(candidate.publicationId()));

    assertHitFieldEquals(json, "type", "NviCandidate");
    assertHitFieldEquals(json, "identifier", candidate.candidateIdentifier());
    assertHitFieldEquals(json, "publicationDetails.id", candidate.publicationId());
    assertHitFieldEquals(json, "publicationDetails.title", candidate.title());
    softly
        .assertThat(json.getList("publicationDetails.nviContributors.name", String.class))
        .contains(candidate.creatorName());
    softly
        .assertThat(json.getList("approvals.institutionId", String.class))
        .contains(Affiliation.UIB.getValue());
    softly.assertThat(json.getDouble("points")).isPositive();
  }

  @Test
  @DisplayName("Search by contributor name")
  @Description("Searching by the creator name from the original publication returns the candidate")
  void shouldFindCandidateWhenSearchingByContributorName() {
    var response = search("query", candidate.creatorName());

    assertThat(indexedPublicationIds(response)).contains(candidate.publicationId());
  }

  @Test
  @DisplayName("Filter by publication title")
  @Description("Filtering by the publication title returns the candidate")
  void shouldFindCandidateWhenFilteringByTitle() {
    var response = search("title", candidate.title());

    assertThat(indexedPublicationIds(response)).contains(candidate.publicationId());
  }

  @Test
  @DisplayName("Filter by organization")
  @Description("Filtering by the creator's top-level organization returns the candidate")
  void shouldFindCandidateWhenFilteringByOrganization() {
    var response = search("affiliations", topLevelOrganizationIdentifier());

    assertThat(indexedPublicationIds(response)).contains(candidate.publicationId());
  }

  @Test
  @DisplayName("Search with non-matching term")
  @Description("Searching by a term that matches nothing does not return the candidate")
  void shouldNotFindCandidateWhenSearchingByNonMatchingTerm() {
    var response = search("query", "no-match-" + UUID.randomUUID());

    assertThat(indexedPublicationIds(response)).doesNotContain(candidate.publicationId());
  }

  @Test
  @DisplayName("Search candidates unauthenticated")
  @Description("Searching for NVI candidates without authentication returns 401 Unauthorized")
  void shouldReturnUnauthorizedWhenSearchingUnauthenticated() {
    givenUnauthenticatedJsonRequest()
        .queryParam("query", candidate.title())
        .get(candidateSearchPath())
        .then()
        .statusCode(401);
  }

  private void assertHitFieldEquals(JsonPath json, String field, String expected) {
    softly.assertThat(json.getString(field)).as(field).isEqualTo(expected);
  }

  private static String topLevelOrganizationIdentifier() {
    var organizationUri = Affiliation.UIB.getValue();
    return organizationUri.substring(organizationUri.lastIndexOf('/') + 1);
  }

  private static Response search(String queryParameter, String value) {
    return givenAuthenticatedJsonRequestAsUser(UIB_NVI_CURATOR)
        .queryParam(queryParameter, value)
        .queryParam("size", SEARCH_PAGE_SIZE)
        .get(candidateSearchPath())
        .then()
        .statusCode(200)
        .extract()
        .response();
  }
}
