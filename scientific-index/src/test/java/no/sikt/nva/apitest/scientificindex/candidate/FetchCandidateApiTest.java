package no.sikt.nva.apitest.scientificindex.candidate;

import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.candidateByPublicationIdPath;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.candidatePath;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.reportStatusPath;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import java.util.UUID;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class FetchCandidateApiTest extends ScientificIndexTestBase {

  @InjectSoftAssertions private SoftAssertions softly;

  private static NviCandidate candidate;

  @BeforeAll
  static void createSharedCandidate() {
    candidate =
        CANDIDATE_FACTORY.createCandidate("NVI integration test publication " + UUID.randomUUID());
  }

  @Test
  @DisplayName("Published academic article becomes NVI candidate")
  @Description(
      "Publishing an academic article in a journal with an NVI level, with a verified creator"
          + " affiliated with an NVI institution, should create an NVI candidate with a new"
          + " approval for the creator's institution")
  void shouldCreateCandidateWhenAcademicArticleIsPublished() {
    var response =
        CANDIDATE_FACTORY
            .fetchCandidateByPublicationId(UIB_NVI_CURATOR, candidate.publicationId())
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("type")).isEqualTo("NviCandidate");
    softly.assertThat(response.getString("publicationId")).isEqualTo(candidate.publicationId());
    softly.assertThat(response.getString("period.status")).isEqualTo("OpenPeriod");
    softly.assertThat(response.getString("period.year")).isEqualTo(CURRENT_YEAR);
    softly.assertThat(response.getList("approvals")).hasSize(1);
    softly
        .assertThat(response.getString("approvals[0].institutionId"))
        .isEqualTo(Affiliation.UIB.getValue());
    softly.assertThat(response.getString("approvals[0].status")).isEqualTo("New");
    softly.assertThat(response.getDouble("totalPoints")).isPositive();
  }

  @Test
  @DisplayName("Fetch candidate by candidate identifier")
  @Description(
      "Fetching an existing NVI candidate by its identifier as an NVI curator should return the"
          + " candidate and statuscode 200 OK")
  void shouldReturnCandidateWhenFetchingByCandidateIdentifier() {
    var response =
        givenAuthenticatedJsonRequestAsUser(UIB_NVI_CURATOR)
            .get(candidatePath(candidate.candidateIdentifier()))
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("identifier")).isEqualTo(candidate.candidateIdentifier());
    softly.assertThat(response.getString("publicationId")).isEqualTo(candidate.publicationId());
  }

  @Test
  @DisplayName("Fetch report status for candidate publication")
  @Description(
      "Fetching the report status of a publication that is an NVI candidate should return status"
          + " PENDING_REVIEW without requiring authentication")
  void shouldReturnReportStatusWhenPublicationIsCandidate() {
    var response =
        givenUnauthenticatedJsonRequest()
            .urlEncodingEnabled(false)
            .get(reportStatusPath(candidate.publicationId()))
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("publicationId")).isEqualTo(candidate.publicationId());
    softly.assertThat(response.getString("reportStatus.status")).isEqualTo("PENDING_REVIEW");
    softly.assertThat(response.getString("period")).isEqualTo(CURRENT_YEAR);
  }

  @Test
  @DisplayName("Fetch candidate unauthenticated")
  @Description(
      "Fetching an NVI candidate without authentication should return statuscode 401 Unauthorized")
  void shouldReturnUnauthorizedWhenFetchingCandidateUnauthenticated() {
    givenUnauthenticatedJsonRequest()
        .urlEncodingEnabled(false)
        .get(candidateByPublicationIdPath(candidate.publicationId()))
        .then()
        .statusCode(401);
  }

  @Test
  @DisplayName("Fetch candidate for publication that is not a candidate")
  @Description(
      "Fetching an NVI candidate by a publication id that has no candidate should return"
          + " statuscode 404 Not Found")
  void shouldReturnNotFoundWhenPublicationIsNotCandidate() {
    var unknownPublicationId = RestAssured.baseURI + "/publication/" + UUID.randomUUID();

    CANDIDATE_FACTORY
        .fetchCandidateByPublicationId(UIB_NVI_CURATOR, unknownPublicationId)
        .then()
        .statusCode(404);
  }
}
