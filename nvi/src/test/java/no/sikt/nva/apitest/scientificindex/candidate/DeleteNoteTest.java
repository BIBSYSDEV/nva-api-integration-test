package no.sikt.nva.apitest.scientificindex.candidate;

import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_DOI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_EDITOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_SUPPORT_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_NOTES_DELETE_PATH;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_PATH;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.DELETE_NOTE_PATH;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import no.sikt.Contributor;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("DELETE " + DELETE_NOTE_PATH)
class DeleteNoteTest extends ScientificIndexTestBase {

  private static Stream<Arguments> userByRoleProvider() {
    return Stream.of(
        argumentSet("Registrar", UIB_CREATOR),
        argumentSet("DOI-curator", UIB_DOI_CURATOR),
        argumentSet("Publishing-curator", UIB_PUBLISHING_CURATOR),
        argumentSet("Support curator", UIB_SUPPORT_CURATOR),
        argumentSet("Editor", UIB_EDITOR));
  }

  /** Deleting a note on a NVI candidate returns {@code 200 Ok} */
  @Test
  @DisplayName("Delete note from candidate")
  @Description(useJavaDoc = true)
  void shouldDeleteNoteFromCandidate(SoftAssertions softly) {

    var candidateWithNote =
        pollUntil(createCandidateWithNote(OSLO_MET_NVI_CURATOR), DeleteNoteTest::isNotConflict);

    var candidateIdentifier = candidateWithNote.jsonPath().getString("identifier");
    var noteIdentifier = candidateWithNote.jsonPath().getString("notes[0].identifier");

    var deleteNoteResponse =
        pollUntil(
            deleteCandidateNote(OSLO_MET_NVI_CURATOR, candidateIdentifier, noteIdentifier),
            DeleteNoteTest::isNotConflict);

    softly.assertThat(deleteNoteResponse.statusCode()).isEqualTo(200);
    softly.assertThat(deleteNoteResponse.jsonPath().getList("notes")).isEmpty();

    var verifyResponse =
        givenAuthenticatedRequestAsUser(OSLO_MET_NVI_CURATOR)
            .when()
            .get(CANDIDATE_PATH, candidateIdentifier)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(verifyResponse.getList("notes")).isEmpty();
  }

  /**
   * Trying to delete a note from a NVI candidate when not a Nvi-curator returns {@code 401
   * Unauthorized}
   */
  @ParameterizedTest
  @MethodSource("userByRoleProvider")
  @DisplayName("Delete note when not Nvi-curator returns unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenNonNviCuratprDeletingNote(User user, SoftAssertions softly) {

    var candidateWithNote =
        pollUntil(createCandidateWithNote(UIB_NVI_CURATOR), DeleteNoteTest::isNotConflict);

    var candidateIdentifier = candidateWithNote.jsonPath().getString("identifier");
    var noteIdentifier = candidateWithNote.jsonPath().getString("notes[0].identifier");

    var deleteNoteResponse =
        pollUntil(
            deleteCandidateNote(user, candidateIdentifier, noteIdentifier),
            DeleteNoteTest::isNotConflict);
    softly.assertThat(deleteNoteResponse.statusCode()).isEqualTo(401);
  }

  /**
   * Trying to delete a note from a NVI candidate when not a Nvi-curator for the owner's institution
   * returns {@code 401 Unauthorized}
   */
  @Test
  @DisplayName("Delete note from candidate from other institution returns unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenDeletingNoteFromOtherInstitution(SoftAssertions softly) {

    var candidateWithNote =
        pollUntil(createCandidateWithNote(OSLO_MET_NVI_CURATOR), DeleteNoteTest::isNotConflict);

    var candidateIdentifier = candidateWithNote.jsonPath().getString("identifier");
    var noteIdentifier = candidateWithNote.jsonPath().getString("notes[0].identifier");

    var deleteNoteResponse =
        pollUntil(
            deleteCandidateNote(UIB_NVI_CURATOR, candidateIdentifier, noteIdentifier),
            DeleteNoteTest::isNotConflict);
    softly.assertThat(deleteNoteResponse.statusCode()).isEqualTo(401);
  }

  /** Trying to delete a non-existing note from a NVI candidate returns {@code 404 Not Found} */
  @Test
  @Disabled("FIXME: Returns 502, should return 505. See NP-51616")
  @DisplayName("Delete non-existing note from candidate returns not found")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenDeletingNonExistingNote(SoftAssertions softly) {

    var candidate = createCandidate(OSLO_MET_NVI_CURATOR);

    var candidateIdentifier = candidate.candidateIdentifier();
    var noteIdentifier = UUID.randomUUID().toString();

    var deleteNoteResponse =
        pollUntil(
            deleteCandidateNote(OSLO_MET_NVI_CURATOR, candidateIdentifier, noteIdentifier),
            DeleteNoteTest::isNotConflict);
    softly.assertThat(deleteNoteResponse.statusCode()).isEqualTo(401);
  }

  private NviCandidate createCandidate(User user) {
    return CANDIDATE_FACTORY.createCandidate(
        "NVI integration test " + UUID.randomUUID(), user, List.of(Contributor.asCreator(user)));
  }

  private static Callable<Response> deleteCandidateNote(
      User user, String candidateIdentifier, String noteIdentifier) {
    return () ->
        givenAuthenticatedRequestAsUser(user)
            .delete(CANDIDATE_NOTES_DELETE_PATH, candidateIdentifier, noteIdentifier);
  }

  private Callable<Response> createCandidateWithNote(User user) {
    return () ->
        CANDIDATE_FACTORY.createCandidateWithNote(
            "NVI integration test " + UUID.randomUUID(), user);
  }

  private static boolean isNotConflict(Response response) {
    return response.statusCode() != HttpURLConnection.HTTP_CONFLICT;
  }
}
