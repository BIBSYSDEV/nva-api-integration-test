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
  @Description("Publishing an eligible academic article creates a candidate with a new approval")
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
    softly.assertThat(response.getString("period.publishingYear")).isEqualTo(CURRENT_YEAR);
    softly.assertThat(response.getList("approvals")).hasSize(1);
    softly
        .assertThat(response.getString("approvals[0].institutionId"))
        .isEqualTo(Affiliation.UIB.getValue());
    softly.assertThat(response.getString("approvals[0].status")).isEqualTo("New");
    softly.assertThat(response.getDouble("totalPoints")).isPositive();
  }

  @Test
  @DisplayName("Fetch candidate by candidate identifier")
  @Description("Fetching a candidate by its identifier returns it with 200 OK")
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
  @Description("Fetching the report status of a candidate publication returns PENDING_REVIEW")
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
  @Description("Fetching a candidate without authentication returns 401 Unauthorized")
  void shouldReturnUnauthorizedWhenFetchingCandidateUnauthenticated() {
    givenUnauthenticatedJsonRequest()
        .urlEncodingEnabled(false)
        .get(candidateByPublicationIdPath(candidate.publicationId()))
        .then()
        .statusCode(401);
  }

  @Test
  @DisplayName("Fetch candidate for publication that is not a candidate")
  @Description("Fetching a candidate for a non-candidate publication returns 404 Not Found")
  void shouldReturnNotFoundWhenPublicationIsNotCandidate() {
    var unknownPublicationId = RestAssured.baseURI + "/publication/" + UUID.randomUUID();

    CANDIDATE_FACTORY
        .fetchCandidateByPublicationId(UIB_NVI_CURATOR, unknownPublicationId)
        .then()
        .statusCode(404);
  }
}
