package no.sikt.nva.apitest.scientificindex.candidate;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import java.util.List;
import java.util.Map;
import static java.util.UUID.randomUUID;
import java.util.concurrent.Callable;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import no.sikt.Contributor;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import no.sikt.nva.apitest.base.User;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_NOTES_PATH;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST " + CANDIDATE_NOTES_PATH)
class CreateNoteTest extends ScientificIndexTestBase {

  /** Creating a note on a NVI candidate returns {@code 200 OK} with the note in the response */
  @Test
  @DisplayName("Create note on NVI candidate")
  @Description(useJavaDoc = true)
  void shouldCreateNote(SoftAssertions softly) {
    var candidate = createCandidate();
    var candidateIdentifier = candidate.candidateIdentifier();
    var candidateNote = createNoteRequest();

    var response =
        givenCreateNoteRequest(OSLO_MET_NVI_CURATOR, candidateIdentifier, candidateNote)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("notes[0].text")).isEqualTo(candidateNote.get("text"));
  }

  /**
   * Trying to create a note on a candidate when the user is not a NVI-curator for the owner return
   * {@code 401 Unauthorized}
   */
  @Test
  @DisplayName("Create note on NVI candidate when not NVI-curator for owner")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenCreatingNoteOnCandidateNotOwned() {
    var candidate = createCandidate();
    var candidateIdentifier = candidate.candidateIdentifier();
    var payload = createNoteRequest();

    givenCreateNoteRequest(UIB_NVI_CURATOR, candidateIdentifier, payload).then().statusCode(HTTP_UNAUTHORIZED);
  }

  /** Trying to create a note on a non-existing candidate returns {@code 404 Not Found} */
  @Test
  @DisplayName("Create note on non-existing NVI candidate")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenCreatingNoteOnNonExistingCandidate() {
    var candidateIdentifier = randomUUID().toString();
    var payload = createNoteRequest();

    givenCreateNoteRequest(UIB_NVI_CURATOR, candidateIdentifier, payload).then().statusCode(HTTP_NOT_FOUND);
  }

  /** Creating a note with no candidateNote returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Create empty note")
  @Description(useJavaDoc = true)
  void shouldReturnInvalidRequestBodyWhenCreatingEmptyNote(SoftAssertions softly) {
    var candidate = createCandidate();
    var candidateIdentifier = candidate.candidateIdentifier();

    var response =
        givenAuthenticatedJsonRequestAsUser(OSLO_MET_NVI_CURATOR)
            .when()
            .post(CANDIDATE_NOTES_PATH, candidateIdentifier)
            .then()
            .statusCode(HTTP_BAD_REQUEST)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("title")).isEqualTo("Invalid request body");
  }

  /** Creating a note with wrong candidateNote returns {@code 400 Bad Request} */
  @Test
  @DisplayName("Create note with wrong candidateNote")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenCreatingNoteWithWrongPayload(SoftAssertions softly) {
    var candidate = createCandidate();
    var candidateIdentifier = candidate.candidateIdentifier();
    var payload = Map.of("noteText", "NVI integration test " + randomUUID());

    var response =
        givenCreateNoteRequest(OSLO_MET_NVI_CURATOR, candidateIdentifier, payload)
            .then()
            .statusCode(HTTP_BAD_REQUEST)
            .extract()
            .jsonPath();

    softly
        .assertThat(response.getString("detail"))
        .isEqualTo("Request body must contain text field.");
  }

  private NviCandidate createCandidate() {
    var title = "NVI - Report status test - %s".formatted(randomUUID());
    var contributors = List.of(Contributor.asCreator(OSLO_MET_CREATOR));
    return CANDIDATE_FACTORY.createCandidate(title, OSLO_MET_PUBLISHING_CURATOR, contributors);
  }

  private static Response givenCreateNoteRequest(
      User user, String candidateIdentifier, Map<String, String> requestBody) {
    return pollUntil(
        postCreateNoteRequest(user, candidateIdentifier, requestBody),
        IntegrationTestBase::isNotConflict);
  }

  private static Map<String, String> createNoteRequest() {
    return Map.of("text", "NVI integration test " + randomUUID());
  }

  private static Callable<Response> postCreateNoteRequest(
      User user, String candidateIdentifier, Map<String, String> requestBody) {
    return () ->
        givenAuthenticatedJsonRequestAsUser(user)
            .body(requestBody)
            .post(CANDIDATE_NOTES_PATH, candidateIdentifier);
  }
}
