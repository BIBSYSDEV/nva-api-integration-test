package no.sikt.nva.apitest.scientificindex.candidate;

import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_NOTES_PATH;

import io.qameta.allure.Description;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST " + CANDIDATE_NOTES_PATH)
class CreateNoteTest extends ScientificIndexTestBase {

  /** Creating a note on a NVI candidate returns {@code 200 OK} with the note in the response */
  @Test
  @DisplayName("Create note on NVI candidate")
  @Description(useJavaDoc = true)
  void shouldCreateNote(SoftAssertions softly) {

    var candidate = createCandidate(OSLO_MET_CREATOR);

    var canidateIdentifier = candidate.candidateIdentifier();
    var payload = createNote();

    var response =
        givenAuthenticatedJsonRequestAsUser(OSLO_MET_NVI_CURATOR)
            .body(payload)
            .when()
            .post(CANDIDATE_NOTES_PATH, canidateIdentifier)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("notes[0].text")).isEqualTo(payload.get("text"));
  }

  /**
   * Trying to create a note on a candidate when the user is not a NVI-curator for the owner return
   * {@code 401 Unauthorized}
   */
  @Test
  @DisplayName("Create note on NVI candidate when not NVI-curator for owner")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenCreatingNoteOnCandidateNotOwned() {

    var candidate = createCandidate(OSLO_MET_CREATOR);

    var canidateIdentifier = candidate.candidateIdentifier();
    var payload = createNote();

    givenAuthenticatedJsonRequestAsUser(UIB_NVI_CURATOR)
        .body(payload)
        .when()
        .post(CANDIDATE_NOTES_PATH, canidateIdentifier)
        .then()
        .statusCode(401);
  }

  /** Trying to create a note on a non-existing candidate returns {@code 404 Not Found} */
  @Test
  @DisplayName("Create note on non-existing NVI candidate")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenCreatingNoteOnNonExistingCandidate() {

    var canidateIdentifier = UUID.randomUUID().toString();
    var payload = createNote();

    givenAuthenticatedJsonRequestAsUser(UIB_NVI_CURATOR)
        .body(payload)
        .when()
        .post(CANDIDATE_NOTES_PATH, canidateIdentifier)
        .then()
        .statusCode(404);
  }

  /** Creating a note with no payload returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Create empty note")
  @Description(useJavaDoc = true)
  void shouldReturnInvalidRequestBodyWhenCreatingEmptyNote(SoftAssertions softly) {

    var candidate = createCandidate(OSLO_MET_CREATOR);

    var canidateIdentifier = candidate.candidateIdentifier();

    var response =
        givenAuthenticatedJsonRequestAsUser(OSLO_MET_NVI_CURATOR)
            .when()
            .post(CANDIDATE_NOTES_PATH, canidateIdentifier)
            .then()
            .statusCode(400)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("title")).isEqualTo("Invalid request body");
  }

  /** Creating a note with wrong payload returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Create note with wrong payload")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenCreatingNoteWithWrongPayload(SoftAssertions softly) {

    var candidate = createCandidate(OSLO_MET_CREATOR);

    var canidateIdentifier = candidate.candidateIdentifier();
    var payload = Map.of("noteText", "NVI integration test " + UUID.randomUUID());

    var response =
        givenAuthenticatedJsonRequestAsUser(OSLO_MET_NVI_CURATOR)
            .body(payload)
            .when()
            .post(CANDIDATE_NOTES_PATH, canidateIdentifier)
            .then()
            .statusCode(400)
            .extract()
            .jsonPath();

    softly
        .assertThat(response.getString("detail"))
        .isEqualTo("Request body must contain text field.");
  }

  private NviCandidate createCandidate(User user) {
    return CANDIDATE_FACTORY.createCandidate(
        "NVI integration test candidate with note " + UUID.randomUUID(), user);
  }

  private Map<String, String> createNote() {
    return Map.of("text", "NVI integration test " + UUID.randomUUID());
  }
}
