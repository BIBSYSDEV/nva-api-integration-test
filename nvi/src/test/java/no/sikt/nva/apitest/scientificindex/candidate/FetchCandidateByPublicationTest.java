package no.sikt.nva.apitest.scientificindex.candidate;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_BY_PUBLICATION_PATH;

import io.qameta.allure.Description;
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
@DisplayName("GET " + CANDIDATE_BY_PUBLICATION_PATH)
class FetchCandidateByPublicationTest extends ScientificIndexTestBase {

  private static NviCandidate candidate;

  @BeforeAll
  static void createSharedCandidate() {
    candidate = CANDIDATE_FACTORY.createCandidate(title());
  }

  private static String title() {
    return "NVI - Fetch candidate by publication test - %s".formatted(randomUUID());
  }

  /** Fetching a candidate by its publication identifier returns it with status {@code 200 OK}. */
  @ParameterizedTest
  @MethodSource("usersWithNviReadAccess")
  @DisplayName("Fetch candidate for publication as NVI user")
  @Description(useJavaDoc = true)
  void shouldReturnCandidateWhenFetchingByPublicationIdentifier(User user, SoftAssertions softly) {
    var response =
        givenAuthenticatedJsonRequestAsUser(user)
            .get(CANDIDATE_BY_PUBLICATION_PATH, candidate.publicationIdentifier())
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("identifier")).isEqualTo(candidate.candidateIdentifier());
    softly.assertThat(response.getString("publicationId")).isEqualTo(candidate.publicationId());
  }

  /** Fetching a candidate without authentication returns status {@code 401 Unauthorized}. */
  @Test
  @DisplayName("Fetch candidate for publication unauthenticated")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenFetchingCandidateUnauthenticated() {
    givenUnauthenticatedJsonRequest()
        .get(CANDIDATE_BY_PUBLICATION_PATH, candidate.publicationIdentifier())
        .then()
        .statusCode(401);
  }

  /** Fetching a candidate as a non-NVI user returns status {@code 403 Forbidden}. */
  @ParameterizedTest
  @Disabled("FIXME: Returns 401, but should be 403. See NP-51618.")
  @MethodSource("usersWithoutNviAccess")
  @DisplayName("Fetch candidate for publication unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenFetchingCandidateWithoutAccess(User user) {
    givenAuthenticatedJsonRequestAsUser(user)
        .get(CANDIDATE_BY_PUBLICATION_PATH, candidate.publicationIdentifier())
        .then()
        .statusCode(403);
  }

  /** Fetching a candidate for a non-candidate publication returns status {@code 404 Not Found}. */
  @Test
  @DisplayName("Fetch candidate for publication that is not a candidate")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenPublicationIsNotCandidate() {
    givenAuthenticatedJsonRequestAsUser(UIB_NVI_CURATOR)
        .get(CANDIDATE_BY_PUBLICATION_PATH, randomUUID().toString())
        .then()
        .statusCode(404);
  }
}
