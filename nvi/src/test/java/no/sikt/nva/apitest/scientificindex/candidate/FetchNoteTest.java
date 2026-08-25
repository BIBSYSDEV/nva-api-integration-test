package no.sikt.nva.apitest.scientificindex.candidate;

import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_PATH;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import java.util.UUID;
import java.util.concurrent.Callable;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import no.sikt.nva.apitest.base.User;
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
  @DisplayName("Fetch note from NVI candidate")
  @Description(useJavaDoc = true)
  void shouldFetchNote(SoftAssertions softly) {

    var title = "NVI integration test " + UUID.randomUUID();
    var noteText = "NVI integration test " + UUID.randomUUID();
    var candidate =
        pollUntil(
            createCandidateWithNote(title, noteText, OSLO_MET_NVI_CURATOR),
            IntegrationTestBase::isNotConflict);

    var candidateIdentifier = candidate.jsonPath().getString("identifier");
    var noteIdentifier = candidate.jsonPath().getString("notes[0].identifier");

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

  private Callable<Response> createCandidateWithNote(String title, String noteText, User user) {
    return () -> CANDIDATE_FACTORY.createCandidateWithNote(title, noteText, user);
  }

  // private static boolean isNotConflict(Response response) {
  //   return response.statusCode() != HttpURLConnection.HTTP_CONFLICT;
  // }
}
