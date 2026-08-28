package no.sikt.nva.apitest.scientificindex.candidate;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.util.UUID.randomUUID;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_BY_PUBLICATION_PATH;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_PATH;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + CANDIDATE_PATH)
class FetchCandidateTest extends ScientificIndexTestBase {

  private static NviCandidate candidate;

  @BeforeAll
  static void createSharedCandidate() {
    candidate = CANDIDATE_FACTORY.createCandidate(title());
  }

  private static String title() {
    return "NVI - Fetch candidate test - %s".formatted(randomUUID());
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
            .statusCode(HTTP_OK)
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
  @ParameterizedTest
  @MethodSource("usersWithNviReadAccess")
  @DisplayName("Fetch candidate as NVI user")
  @Description(useJavaDoc = true)
  void shouldReturnCandidateWhenFetchingByCandidateIdentifier(User user, SoftAssertions softly) {
    var response =
        givenAuthenticatedJsonRequestAsUser(user)
            .get(CANDIDATE_PATH, candidate.candidateIdentifier())
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("identifier")).isEqualTo(candidate.candidateIdentifier());
    softly.assertThat(response.getString("publicationId")).isEqualTo(candidate.publicationId());
  }

  /** Fetching a candidate without authentication returns status {@code 401 Unauthorized}. */
  @Test
  @DisplayName("Fetch candidate unauthenticated")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenFetchingCandidateUnauthenticated() {
    givenUnauthenticatedJsonRequest()
        .get(CANDIDATE_BY_PUBLICATION_PATH, candidate.publicationIdentifier())
        .then()
        .statusCode(HTTP_UNAUTHORIZED);
  }

  /** Fetching a candidate as a non-NVI user returns status {@code 403 Forbidden}. */
  @ParameterizedTest
  @Disabled("FIXME: Returns 401, but should be 403. See NP-51618.")
  @MethodSource("usersWithoutNviAccess")
  @DisplayName("Fetch candidate unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenFetchingCandidateWithoutAccess(User user) {
    givenAuthenticatedJsonRequestAsUser(user)
        .get(CANDIDATE_PATH, candidate.candidateIdentifier())
        .then()
        .statusCode(HTTP_FORBIDDEN);
  }

  /** Fetching a candidate that doesn't exist returns status {@code 404 Not Found}. */
  @Test
  @DisplayName("Fetch candidate that doesn't exist")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenCandidateDoesNotExist() {
    givenAuthenticatedJsonRequestAsUser(UIB_NVI_CURATOR)
        .get(CANDIDATE_PATH, randomUUID().toString())
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }
}
