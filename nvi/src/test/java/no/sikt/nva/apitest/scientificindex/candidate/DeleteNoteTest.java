package no.sikt.nva.apitest.scientificindex.candidate;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import org.junit.jupiter.params.provider.MethodSource;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import no.sikt.nva.apitest.base.User;
import static no.sikt.nva.apitest.base.UserFixtures.KRISTIANIA_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.KRISTIANIA_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_DOI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_EDITOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_SUPPORT_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_NVI_CURATOR;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_NOTES_PATH;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_PATH;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.DELETE_NOTE_PATH;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("DELETE " + DELETE_NOTE_PATH)
class DeleteNoteTest extends ScientificIndexTestBase {

  private static final String CANDIDATE_NOTES_DELETE_PATH =
      CANDIDATE_NOTES_PATH + "/{noteIdentifier}";

  private static Stream<Arguments> userByRoleProvider() {
    return Stream.of(
        argumentSet("Registrar", UIB_CREATOR),
        argumentSet("DOI-curator", UIB_DOI_CURATOR),
        argumentSet("Publishing-curator", UIB_PUBLISHING_CURATOR),
        argumentSet("Support curator", UIB_SUPPORT_CURATOR),
        argumentSet("Editor", UIB_EDITOR));
  }

  private final Map<User, User> nviCurators =
      Map.of(
          UIB_CREATOR, UIB_NVI_CURATOR,
          UIS_CREATOR, UIS_NVI_CURATOR,
          KRISTIANIA_CREATOR, KRISTIANIA_NVI_CURATOR,
          OSLO_MET_CREATOR, OSLO_MET_NVI_CURATOR);

  @Test
  @DisplayName("Delete note from candidate")
  @Description(useJavaDoc = true)
  void shouldDeleteNoteFromCandidate(SoftAssertions softly) {

    var candidateWithNote = createCandidateWithNote(OSLO_MET_CREATOR);

    var candidateIdentifier = candidateWithNote.getString("identifier");
    var noteIdentifer = candidateWithNote.getString("notes[0].identifier");

    var deleteResponse =
        givenAuthenticatedRequestAsUser(OSLO_MET_NVI_CURATOR)
            .when()
            .delete(CANDIDATE_NOTES_DELETE_PATH, candidateIdentifier, noteIdentifer)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(deleteResponse.getList("notes")).isEmpty();

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

  @ParameterizedTest
  @MethodSource("userByRoleProvider")
  @DisplayName("Delete note when not Nvi-curator returns unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenNonNviCuratprDeletingNote(User user) {

    var candidateWithNote = createCandidateWithNote(UIB_CREATOR);

    var candidateIdentifier = candidateWithNote.getString("identifier");
    var noteIdentifer = candidateWithNote.getString("notes[0].identifier");

    givenAuthenticatedRequestAsUser(user)
        .when()
        .delete(CANDIDATE_NOTES_DELETE_PATH, candidateIdentifier, noteIdentifer)
        .then()
        .statusCode(401);
  }

  @Test
  @DisplayName("Delete note from candidate from other institution returns unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenDeletingNoteFromOtherInstitution() {

    var candidateWithNote = createCandidateWithNote(OSLO_MET_CREATOR);

    var candidateIdentifier = candidateWithNote.getString("identifier");
    var noteIdentifer = candidateWithNote.getString("notes[0].identifier");

    givenAuthenticatedRequestAsUser(UIS_NVI_CURATOR)
        .when()
        .delete(CANDIDATE_NOTES_DELETE_PATH, candidateIdentifier, noteIdentifer)
        .then()
        .statusCode(401);
  }

  @Test
  @Disabled("Bug: See NP-51616")
  @DisplayName("Delete non-existing note from candidate returns not found")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenDeletingNonExistingNote() {

    var candidate = createCandidate(OSLO_MET_CREATOR);

    var candidateIdentifier = candidate.candidateIdentifier();
    var noteIdentifer = UUID.randomUUID().toString();

    givenAuthenticatedRequestAsUser(OSLO_MET_NVI_CURATOR)
        .when()
        .delete(CANDIDATE_NOTES_DELETE_PATH, candidateIdentifier, noteIdentifer)
        .then()
        .statusCode(404);
  }

  private JsonPath createCandidateWithNote(User user) {
    var candidate = createCandidate(user);
    var candidateIdentifier = candidate.candidateIdentifier();

    var candidateNote = createNote();

    var createResponse =
        givenAuthenticatedRequestAsUser(nviCurators.get(user))
            .body(candidateNote)
            .when()
            .post(CANDIDATE_NOTES_PATH, candidateIdentifier)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    return createResponse;
  }

  private NviCandidate createCandidate(User user) {
    return CANDIDATE_FACTORY.createCandidate("NVI integration test " + UUID.randomUUID(), user);
  }

  private Map<String, String> createNote() {
    return Map.of("text", "NVI integration test " + UUID.randomUUID());
  }
}
