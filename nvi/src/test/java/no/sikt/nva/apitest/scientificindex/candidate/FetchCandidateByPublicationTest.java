package no.sikt.nva.apitest.scientificindex.candidate;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.APP_ADMIN;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_DOI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_EDITOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_SUPPORT_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_THESIS_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_BY_PUBLICATION_PATH;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import java.util.stream.Stream;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + CANDIDATE_BY_PUBLICATION_PATH)
class FetchCandidateByPublicationTest extends ScientificIndexTestBase {

  private static NviCandidate candidate;

  @BeforeAll
  static void createSharedCandidate() {
    candidate =
        CANDIDATE_FACTORY.createCandidate("NVI integration test publication " + randomUUID());
  }

  /** Fetching a candidate without authentication returns status {@code 401 Unauthorized}. */
  @Test
  @DisplayName("Fetch candidate unauthenticated")
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
  @MethodSource("nonNviUserProvider")
  @DisplayName("Fetch candidate unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenFetchingCandidateWithoutAccess(User user) {
    givenAuthenticatedJsonRequestAsUser(user)
        .get(CANDIDATE_BY_PUBLICATION_PATH, candidate.publicationIdentifier())
        .then()
        .statusCode(403);
  }

  private static Stream<Arguments> nonNviUserProvider() {
    return Stream.of(
        argumentSet("Editor", UIB_EDITOR),
        argumentSet("Registrar", UIB_CREATOR),
        argumentSet("DOI curator", UIB_DOI_CURATOR),
        argumentSet("Publishing curator", UIB_PUBLISHING_CURATOR),
        argumentSet("Support curator", UIB_SUPPORT_CURATOR),
        argumentSet("Thesis curator", UIB_THESIS_CURATOR));
  }

  /** Fetching a candidate for a non-candidate publication returns status {@code 404 Not Found}. */
  @Test
  @DisplayName("Fetch candidate for publication that is not a candidate")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenPublicationIsNotCandidate() {
    CANDIDATE_FACTORY
        .fetchCandidateByPublicationId(UIB_NVI_CURATOR, randomUUID().toString())
        .then()
        .statusCode(404);
  }

  /** Fetching a candidate by its publication identifier returns it with status {@code 200 OK}. */
  @ParameterizedTest
  @MethodSource("nviUserProvider")
  @DisplayName("Fetch candidate as NVI user")
  @Description(useJavaDoc = true)
  void shouldReturnCandidateWhenFetchingByCandidateIdentifier(User user, SoftAssertions softly) {
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

  private static Stream<Arguments> nviUserProvider() {
    return Stream.of(
        argumentSet("Application administrator", APP_ADMIN),
        argumentSet("NVI curator", UIB_NVI_CURATOR));
  }
}
