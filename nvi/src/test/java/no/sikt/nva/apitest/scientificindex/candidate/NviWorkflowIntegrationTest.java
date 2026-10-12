package no.sikt.nva.apitest.scientificindex.candidate;

import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.NviApprovals.APPROVED;
import static no.sikt.nva.apitest.scientificindex.NviApprovals.updateApprovalStatus;
import static no.sikt.nva.apitest.scientificindex.NviCandidateFactory.indexedPublicationIds;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import no.sikt.nva.apitest.scientificindex.Approval;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("NVI-tickets workflow test")
class NviWorkflowIntegrationTest extends ScientificIndexTestBase {

  /**
   * A publication that qualifies as a NVI-candidate is listed among a NVI-curators NVI-candidates
   */
  @Test
  @DisplayName(
      "A publication that qualifies as a NVI-candidate is listed among a NVI-curators"
          + " NVI-candidates")
  @Description
  void shouldBeANviCandidate() {
    var candidate = createNviCandidate(UIB_CREATOR);
    assertPublicationIsListedAmongNviCandidates(UIB_NVI_CURATOR, candidate);
  }

  /** A NVI-curator approves a NVI-candidate */
  @Test
  @DisplayName("A NVI-curator approves a NVI-candidate")
  @Description
  void shouldApproveANviCandidate() {
    var candidate = createNviCandidate(UIB_CREATOR);
    approveNviCandidate(UIB_NVI_CURATOR, candidate);
    assertCandidateIsApproved(UIB_NVI_CURATOR, candidate.publicationIdentifier());
  }

  @Step("Given a NVI-candidate")
  private static NviCandidate createNviCandidate(User user) {
    return CANDIDATE_FACTORY.createCandidate(PublicationTestBase.randomTitle(), user);
  }

  @Step("Then the publication is listed among the NVI-candidates for a NVI-curator")
  private static void assertPublicationIsListedAmongNviCandidates(
      User nviCurator, NviCandidate candidate) {
    var response = CANDIDATE_FACTORY.awaitCandidateInSearchIndex(nviCurator, candidate);
    assertThat(indexedPublicationIds(response)).containsExactly(candidate.publicationId());
  }

  @Step("When a NVI-curator approves the candidate")
  private static void approveNviCandidate(User nviCurator, NviCandidate candidate) {
    updateApprovalStatus(nviCurator, candidate, APPROVED);
  }

  @Step("Then the candidate get status approved")
  private static void assertCandidateIsApproved(User nviCurator, String publicationIdentifier) {
    var response =
        CANDIDATE_FACTORY.fetchCandidateByPublicationIdentifier(nviCurator, publicationIdentifier);
    var approvals = response.jsonPath().getList("approvals", Approval.class);
    assertThat(approvals)
        .filteredOn(Approval::assignee, nviCurator.cristinId())
        .singleElement()
        .extracting(Approval::status)
        .isEqualTo(APPROVED);
  }
}
