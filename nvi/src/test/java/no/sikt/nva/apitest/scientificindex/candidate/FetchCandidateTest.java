package no.sikt.nva.apitest.scientificindex.candidate;

import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_PATH;

import io.qameta.allure.Description;
import java.util.UUID;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + CANDIDATE_PATH)
class FetchCandidateTest extends ScientificIndexTestBase {

  private static NviCandidate candidate;

  @BeforeAll
  static void createSharedCandidate() {
    candidate =
        CANDIDATE_FACTORY.createCandidate("NVI integration test publication " + UUID.randomUUID());
  }

  /** Publishing an eligible academic article creates a candidate with a new approval. */
  @Test
  @DisplayName("Published academic article becomes NVI candidate")
  @Description(useJavaDoc = true)
  void shouldCreateCandidateWhenAcademicArticleIsPublished(SoftAssertions softly) {
    var response =
        CANDIDATE_FACTORY
            .fetchCandidateByPublicationId(UIB_NVI_CURATOR, candidate.publicationId())
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("type")).isEqualTo("NviCandidate");
    softly.assertThat(response.getString("publicationId")).isEqualTo(candidate.publicationId());
    softly.assertThat(response.getString("period.status")).isEqualTo("OpenPeriod");
    softly.assertThat(response.getString("period.publishingYear")).isEqualTo(CURRENT_YEAR);
    softly.assertThat(response.getList("approvals")).hasSize(1);
    softly
        .assertThat(response.getString("approvals[0].institutionId"))
        .isEqualTo(Affiliation.UIB.getValue());
    softly.assertThat(response.getString("approvals[0].status")).isEqualTo("New");
    softly.assertThat(response.getDouble("totalPoints")).isPositive();
  }

  /** Fetching a candidate by its identifier returns it with status {@code 200 OK}. */
  @Test
  @DisplayName("Fetch candidate by candidate identifier")
  @Description(useJavaDoc = true)
  void shouldReturnCandidateWhenFetchingByCandidateIdentifier(SoftAssertions softly) {
    var response =
        givenAuthenticatedJsonRequestAsUser(UIB_NVI_CURATOR)
            .get(CANDIDATE_PATH, candidate.candidateIdentifier())
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("identifier")).isEqualTo(candidate.candidateIdentifier());
    softly.assertThat(response.getString("publicationId")).isEqualTo(candidate.publicationId());
  }
}
