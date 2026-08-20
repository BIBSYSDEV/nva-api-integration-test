package no.sikt.nva.apitest.scientificindex.candidate;

import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_NOTES_PATH;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_PATH;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
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
@DisplayName("GET " + CANDIDATE_PATH)
class FetchNoteTest extends ScientificIndexTestBase {

  /** Fetch a note on a NVI candidate returns {@code 200 OK} with the note in the response */
  @Test
  @DisplayName("Fetch note fromn NVI candidate")
  @Description(useJavaDoc = true)
  void shouldFetchNote(SoftAssertions softly) {

    var noteText = "NVI integration test " + UUID.randomUUID();
    var candidate = createCandidateWithNote(noteText);

    var candidateIdentifier = candidate.getString("identifier");
    var noteIdentifier = candidate.getString("notes[0].identifier");

    var response =
        givenAuthenticatedJsonRequestAsUser(OSLO_MET_NVI_CURATOR)
            .when()
            .get(CANDIDATE_PATH, candidateIdentifier)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("notes[0].identifier")).isEqualTo(noteIdentifier);
    softly.assertThat(response.getString("notes[0].text")).isEqualTo(noteText);
    softly
        .assertThat(response.getString("notes[0].user"))
        .isEqualTo(OSLO_MET_NVI_CURATOR.cristinId());
  }

  private JsonPath createCandidateWithNote(String noteText) {
    var candidate = createCandidate(OSLO_MET_CREATOR);
    var candidateIdentifier = candidate.candidateIdentifier();

    var candidateNote = createNote(noteText);

    return givenAuthenticatedRequestAsUser(OSLO_MET_NVI_CURATOR)
        .body(candidateNote)
        .when()
        .post(CANDIDATE_NOTES_PATH, candidateIdentifier)
        .then()
        .statusCode(200)
        .extract()
        .jsonPath();
  }

  private NviCandidate createCandidate(User user) {
    return CANDIDATE_FACTORY.createCandidate("NVI integration test " + UUID.randomUUID(), user);
  }

  private Map<String, String> createNote(String text) {
    return Map.of("text", text);
  }
}
