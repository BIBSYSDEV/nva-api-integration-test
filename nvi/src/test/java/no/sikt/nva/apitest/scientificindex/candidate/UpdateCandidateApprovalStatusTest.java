package no.sikt.nva.apitest.scientificindex.candidate;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.util.UUID.randomUUID;
import static no.sikt.nva.apitest.base.UserFixtures.KRISTIANIA_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.KRISTIANIA_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.OSLO_MET_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.NviApprovals.APPROVED;
import static no.sikt.nva.apitest.scientificindex.NviApprovals.PENDING;
import static no.sikt.nva.apitest.scientificindex.NviApprovals.REJECTED;
import static no.sikt.nva.apitest.scientificindex.NviApprovals.updateApprovalStatus;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_STATUS_PATH;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import java.util.List;
import java.util.stream.Stream;
import no.sikt.Contributor;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("PUT " + CANDIDATE_STATUS_PATH)
class UpdateCandidateApprovalStatusTest extends ScientificIndexTestBase {

  private static final String REJECTION_REASON = "Rejected by API integration test";
  private static final String FINALIZED_BY_FIELD = "approvals[0].finalizedBy";
  private static final String FINALIZED_DATE_FIELD = "approvals[0].finalizedDate";
  private static final String STATUS_FIELD = "approvals[0].status";
  private static final String REASON_FIELD = "approvals[0].reason";

  private static String title() {
    return "NVI - Update approval status test - %s".formatted(randomUUID());
  }

  /** Approving a candidate as an NVI curator returns status {@code 200 OK}. */
  @Test
  @DisplayName("Approve candidate as NVI curator")
  @Description(useJavaDoc = true)
  void shouldApproveCandidateWhenRequestedByNviCurator(SoftAssertions softly) {
    var candidate = createCandidate();

    var response =
        updateApprovalStatus(UIB_NVI_CURATOR, candidate, APPROVED)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString(STATUS_FIELD)).isEqualTo(APPROVED);
    softly.assertThat(response.getString(FINALIZED_BY_FIELD)).isNotEmpty();
    softly.assertThat(response.getString(FINALIZED_DATE_FIELD)).isNotEmpty();
  }

  @ParameterizedTest
  @MethodSource("curatorProvider")
  @DisplayName("Approve candidate as NVI curator at a given institution")
  @Description(useJavaDoc = true)
  void shouldApproveCandidateWhenRequestedBySomeNviCurator(
      User curator, User creator, SoftAssertions softly) {
    var contributors = List.of(Contributor.asCreator(creator));
    var candidate = CANDIDATE_FACTORY.createCandidate(title(), curator, contributors);

    var response =
        updateApprovalStatus(curator, candidate, APPROVED)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString(STATUS_FIELD)).isEqualTo(APPROVED);
    softly.assertThat(response.getString(FINALIZED_BY_FIELD)).isEqualTo(curator.cristinId());
    softly.assertThat(response.getString(FINALIZED_DATE_FIELD)).isNotEmpty();
  }

  private static Stream<Arguments> curatorProvider() {
    return Stream.of(
        argumentSet("Kristiania", KRISTIANIA_NVI_CURATOR, KRISTIANIA_CREATOR),
        argumentSet("OsloMet", OSLO_MET_NVI_CURATOR, OSLO_MET_CREATOR),
        argumentSet("UiB", UIB_NVI_CURATOR, UIB_CREATOR),
        argumentSet("UiS", UIS_NVI_CURATOR, UIS_CREATOR));
  }

  /** Rejecting a candidate with a reason returns status {@code 200 OK}. */
  @Test
  @DisplayName("Reject candidate with reason")
  @Description(useJavaDoc = true)
  void shouldRejectCandidateWhenReasonIsProvided(SoftAssertions softly) {
    var candidate = createCandidate();

    var response =
        updateApprovalStatus(UIB_NVI_CURATOR, candidate, REJECTED, REJECTION_REASON)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString(STATUS_FIELD)).isEqualTo(REJECTED);
    softly.assertThat(response.getString(REASON_FIELD)).isEqualTo(REJECTION_REASON);
    softly.assertThat(response.getString(FINALIZED_BY_FIELD)).isNotEmpty();
  }

  /** Rejecting a candidate without a reason returns status {@code 400 Bad Request}. */
  @Test
  @DisplayName("Reject candidate without reason")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenRejectingWithoutReason() {
    var candidate = createCandidate();

    updateApprovalStatus(UIB_NVI_CURATOR, candidate, REJECTED).then().statusCode(HTTP_BAD_REQUEST);
  }

  /** Reverting an approved candidate to Pending stays Pending, not New. */
  @Test
  @DisplayName("Reset approved candidate to pending")
  @Description(useJavaDoc = true)
  void shouldResetApprovalWhenApprovedCandidateIsSetToPending(SoftAssertions softly) {
    var candidate = createCandidate();

    updateApprovalStatus(UIB_NVI_CURATOR, candidate, APPROVED).then().statusCode(HTTP_OK);

    var response =
        updateApprovalStatus(UIB_NVI_CURATOR, candidate, PENDING)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString(STATUS_FIELD)).isEqualTo(PENDING);
    softly.assertThat(response.getString(FINALIZED_BY_FIELD)).isNull();
    softly.assertThat(response.getString(FINALIZED_DATE_FIELD)).isNull();
  }

  /** Updating approval without MANAGE_NVI_CANDIDATES returns status {@code 401 Unauthorized}. */
  @Test
  @DisplayName("Approve candidate without MANAGE_NVI_CANDIDATES access right")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenUserLacksManageNviCandidates() {
    var candidate = createCandidate();

    updateApprovalStatus(UIB_CREATOR, candidate, APPROVED).then().statusCode(HTTP_UNAUTHORIZED);
  }

  private static NviCandidate createCandidate() {
    return CANDIDATE_FACTORY.createCandidate(title());
  }
}
