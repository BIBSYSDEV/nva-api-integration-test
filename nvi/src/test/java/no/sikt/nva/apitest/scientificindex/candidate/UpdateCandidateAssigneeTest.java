package no.sikt.nva.apitest.scientificindex.candidate;

import java.util.List;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import no.sikt.Contributor;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_NVI_CURATOR;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_ASSIGNEE_PATH;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("PUT " + CANDIDATE_ASSIGNEE_PATH)
class UpdateCandidateAssigneeTest extends ScientificIndexTestBase {

  @Test
  @DisplayName("Assign curator to candidate")
  @Description(useJavaDoc = true)
  void shouldAssignCuratorToCandidate(SoftAssertions softly) {

    var candidate = createCandidate();
    var candidateIdentifier = candidate.candidateIdentifier();

    givenAuthenticatedRequestAsUser(UIS_NVI_CURATOR)
    .when()
    .put(CANDIDATE_ASSIGNEE_PATH, candidateIdentifier)
    .then()
    .statusCode(200);
  }

  private NviCandidate createCandidate() {
    var title = "Test NVI canidate " + UUID.randomUUID();
    return CANDIDATE_FACTORY.createCandidate(title, UIS_NVI_CURATOR, List.of(Contributor.asCreator(UIS_CREATOR)));
  }
}
