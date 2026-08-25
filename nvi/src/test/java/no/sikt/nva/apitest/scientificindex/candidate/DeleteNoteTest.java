package no.sikt.nva.apitest.scientificindex.candidate;

import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_PATH;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.DELETE_NOTE_PATH;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import no.sikt.Contributor;
import no.sikt.nva.apitest.base.IntegrationTestBase;
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
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("DELETE " + DELETE_NOTE_PATH)
class DeleteNoteTest extends ScientificIndexTestBase {

  /** Deleting a note on a NVI candidate returns {@code 200 Ok} */
  @Test
  @DisplayName("Delete note from candidate")
  @Description(useJavaDoc = true)
  void shouldDeleteNoteFromCandidate(SoftAssertions softly) {

    var candidateWithNote = createCandidateWithNote(OSLO_MET_NVI_CURATOR);

    var candidateIdentifier = candidateWithNote.jsonPath().getString("identifier");
    var noteIdentifier = candidateWithNote.jsonPath().getString("notes[0].identifier");

    var deleteNoteResponse =
        givenDeleteNoteRequest(OSLO_MET_NVI_CURATOR, candidateIdentifier, noteIdentifier);

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
   * Trying to delete a note from a NVI candidate when not a Nvi-curator returns {@code 403
   * Forbidden}
   */
  @ParameterizedTest
  @Disabled("Fixme: See NP-51618")
  @MethodSource("usersWithoutNviAccess")
  @DisplayName("Delete note when not Nvi-curator returns forbidden")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenNonNviCuratprDeletingNote(User user, SoftAssertions softly) {

    var candidateWithNote = createCandidateWithNote(UIB_NVI_CURATOR);

    var candidateIdentifier = candidateWithNote.jsonPath().getString("identifier");
    var noteIdentifier = candidateWithNote.jsonPath().getString("notes[0].identifier");

    var deleteNoteResponse =
        pollUntil(
            deleteCandidateNote(user, candidateIdentifier, noteIdentifier),
            IntegrationTestBase::isNotConflict);
    softly.assertThat(deleteNoteResponse.statusCode()).isEqualTo(403);
  }

  /**
   * Trying to delete a note from a NVI candidate when not a Nvi-curator for the owner's institution
   * returns {@code 403 Forbidden}
   */
  @Test
  @Disabled("Fixme: See NP-51618")
  @DisplayName("Delete note from candidate from other institution returns unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenDeletingNoteFromOtherInstitution(SoftAssertions softly) {

    var candidateWithNote = createCandidateWithNote(OSLO_MET_NVI_CURATOR);

    var candidateIdentifier = candidateWithNote.jsonPath().getString("identifier");
    var noteIdentifier = candidateWithNote.jsonPath().getString("notes[0].identifier");

    var deleteNoteResponse =
        pollUntil(
            deleteCandidateNote(UIB_NVI_CURATOR, candidateIdentifier, noteIdentifier),
            IntegrationTestBase::isNotConflict);
    softly.assertThat(deleteNoteResponse.statusCode()).isEqualTo(403);
  }

  /** Trying to delete a non-existing note from a NVI candidate returns {@code 404 Not Found} */
  @Test
  @Disabled("FIXME: Returns 502, should return 404. See NP-51616")
  @DisplayName("Delete non-existing note from candidate returns not found")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenDeletingNonExistingNote(SoftAssertions softly) {

    var candidate = createCandidate(OSLO_MET_NVI_CURATOR);

    var candidateIdentifier = candidate.candidateIdentifier();
    var noteIdentifier = UUID.randomUUID().toString();

    var deleteNoteResponse =
        pollUntil(
            deleteCandidateNote(OSLO_MET_NVI_CURATOR, candidateIdentifier, noteIdentifier),
            IntegrationTestBase::isNotConflict);
    softly.assertThat(deleteNoteResponse.statusCode()).isEqualTo(401);
  }

  private NviCandidate createCandidate(User user) {
    return CANDIDATE_FACTORY.createCandidate(
        "NVI integration test " + UUID.randomUUID(), user, List.of(Contributor.asCreator(user)));
  }

  private static Response givenDeleteNoteRequest(
      User user, String candidateIdentifier, String noteIdentifier) {
    return pollUntil(
        deleteCandidateNote(user, candidateIdentifier, noteIdentifier),
        IntegrationTestBase::isNotConflict);
  }

  private static Callable<Response> deleteCandidateNote(
      User user, String candidateIdentifier, String noteIdentifier) {
    return () ->
        givenAuthenticatedRequestAsUser(user)
            .delete(DELETE_NOTE_PATH, candidateIdentifier, noteIdentifier);
  }

  private Response createCandidateWithNote(User user) {
    return CANDIDATE_FACTORY.createCandidateWithNote(
        "NVI integration test " + UUID.randomUUID(), user);
  }
}
