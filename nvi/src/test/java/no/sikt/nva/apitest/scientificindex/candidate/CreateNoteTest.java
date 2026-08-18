package no.sikt.nva.apitest.scientificindex.candidate;

import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import no.sikt.nva.apitest.base.User;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_PUBLISHING_CURATOR;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_NOTES_PATH;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST " + CANDIDATE_NOTES_PATH)
class CreateNoteTest extends ScientificIndexTestBase {


  @Test
  @DisplayName("Create note on NVI candidate")
  @Description(useJavaDoc = true)
  void shouldCreateNote(SoftAssertions softly) {

    var candidate = createCandidate(OSLO_MET_CREATOR, OSLO_MET_PUBLISHING_CURATOR);

    var canidateIdentifier = candidate.candidateIdentifier();
    var payload = createNote();

    givenAuthenticatedJsonRequestAsUser(OSLO_MET_NVI_CURATOR)
    .body(payload)
    .when()
    .post(CANDIDATE_NOTES_PATH.replace("{candidate}", canidateIdentifier))
    .then()
    .statusCode(200);
  }


  @Test
  @DisplayName("placeholder")
  @Description(useJavaDoc = true)
  void placeholder(SoftAssertions softly) {
    softly.assertThat(CANDIDATE_NOTES_PATH).isNotBlank();
  }

  private NviCandidate createCandidate(User user, User publishingCurator) {
    return CANDIDATE_FACTORY.createCandidate("NVI integration test candidate with note " + UUID.randomUUID(), user, publishingCurator);
  }


  private Map<String, String> createNote() {
    return Map.of("text", "NVI integration test " + UUID.randomUUID());
  }
}
