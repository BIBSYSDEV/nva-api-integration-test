package no.sikt.nva.apitest.scientificindex.candidate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.qameta.allure.Description;
import no.sikt.Contributor;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import no.sikt.nva.apitest.base.User;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_NVI_CURATOR;
import no.sikt.nva.apitest.scientificindex.NviCandidate;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CANDIDATE_ASSIGNEE_PATH;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("PUT " + CANDIDATE_ASSIGNEE_PATH)
class UpdateCandidateAssigneeTest extends ScientificIndexTestBase {

  /* Assigning a nvi-curator to a candidate returns the candidate with the curator assigned to it and status {@code Ok} */
  @Test
  @DisplayName("Assign curator to candidate")
  @Description(useJavaDoc = true)
  void shouldAssignCuratorToCandidate(SoftAssertions softly) {

    var candidate = createCandidate(UIS_NVI_CURATOR, List.of(Contributor.asCreator(UIS_CREATOR)));
    var candidateIdentifier = candidate.candidateIdentifier();

    var payload = createPayload(UIS_NVI_CURATOR);

    var response =
        givenAuthenticatedRequestAsUser(UIS_NVI_CURATOR)
            .body(payload)
            .when()
            .put(CANDIDATE_ASSIGNEE_PATH, candidateIdentifier)
            .then()
            .statusCode(200)
            .extract()
            .response();

    softly
        .assertThat(response.jsonPath().getString("approvals[0].institutionId"))
        .isEqualTo(UIS_NVI_CURATOR.affiliations().iterator().next());
    softly
        .assertThat(response.jsonPath().getString("approvals[0].assignee"))
        .isEqualTo(UIS_NVI_CURATOR.cristinId());
    softly.assertThat(response.jsonPath().getString("approvals[0].status")).isEqualTo("Pending");
  }

  /** Assigning a curator from the insitution of a contributor to a candidate returns the candidate with the curator assigned to it and status {@code 200 Ok} */
  @Test
  @DisplayName(
      "Assign curator to candidate created at other institution with contributor from own"
          + " institution")
  @Description(useJavaDoc = true)
  void shouldAssignCuratorToCandidateWithContributorFromOwnInstitution(SoftAssertions softly) {

    var candidate =
        createCandidate(
            UIS_NVI_CURATOR,
            List.of(Contributor.asCreator(UIS_CREATOR), Contributor.asCreator(UIB_CREATOR)));
    var candidateIdentifier = candidate.candidateIdentifier();

    var payload = createPayload(UIB_NVI_CURATOR);

    var response =
        givenAuthenticatedRequestAsUser(UIB_NVI_CURATOR)
            .body(payload)
            .when()
            .put(CANDIDATE_ASSIGNEE_PATH, candidateIdentifier)
            .then()
            .statusCode(200)
            .extract()
            .response();

    softly
        .assertThat(response.jsonPath().getString("approvals[0].institutionId"))
        .isEqualTo(UIB_NVI_CURATOR.affiliations().iterator().next());
    softly
        .assertThat(response.jsonPath().getString("approvals[0].assignee"))
        .isEqualTo(UIB_NVI_CURATOR.cristinId());
    softly.assertThat(response.jsonPath().getString("approvals[0].status")).isEqualTo("Pending");
  }

  /** Trying to assign a curator to a non-existing candidate returns status {@code 404 Not Found} */
  @Test
  @DisplayName("Trying to assign a curator to a non-existing candidate")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenTryingToAssignCuratorToNonExistingCandidate() {

    var candidateIdentifier = UUID.randomUUID().toString();

    var payload = createPayload(UIB_NVI_CURATOR);

    givenAuthenticatedRequestAsUser(UIB_NVI_CURATOR)
        .body(payload)
        .when()
        .put(CANDIDATE_ASSIGNEE_PATH, candidateIdentifier)
        .then()
        .statusCode(404);
  }

  /** Calling the service with no body returns status {@code 400 Bad Request} */
  @Test
  @DisplayName("Calling with no body should return 400 Bad Request")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenCallingWIthNoBody(SoftAssertions softly) {

    var candidate = createCandidate(UIS_NVI_CURATOR, List.of(Contributor.asCreator(UIS_CREATOR)));
    var candidateIdentifier = candidate.candidateIdentifier();

    var response =
        givenAuthenticatedRequestAsUser(UIS_NVI_CURATOR)
            .when()
            .put(CANDIDATE_ASSIGNEE_PATH, candidateIdentifier)
            .then()
            .statusCode(400)
            .extract()
            .response();

    softly.assertThat(response.jsonPath().getString("title")).isEqualTo("Invalid request body");
    softly
        .assertThat(response.jsonPath().getString("detail"))
        .isEqualTo("[Unknown error parsing request body]");
  }

  /** Assigning a curator to a candidate with no authentication returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Assigning a curator with no authentication should return 401 Unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenNotAuthenticated() {

    var candidate = createCandidate(UIS_NVI_CURATOR, List.of(Contributor.asCreator(UIS_CREATOR)));
    var candidateIdentifier = candidate.candidateIdentifier();

    var payload = createPayload(UIS_NVI_CURATOR);

    givenUnauthenticatedJsonRequest()
        .body(payload)
        .when()
        .put(CANDIDATE_ASSIGNEE_PATH, candidateIdentifier)
        .then()
        .statusCode(401);
  }

  /** Assigning a curator while not a nvi-curator return {@code 401 Unauthorized} */
  @ParameterizedTest
  @MethodSource("usersWithoutNviAccess")
  @DisplayName("Non Nvi-curator should return 401 Unauthorized")
  @Description(useJavaDoc = true)
  void shouldReturnForbiddenWhenNonNviCurator(User user) {

    var candidate = createCandidate(UIB_NVI_CURATOR, List.of(Contributor.asCreator(UIB_CREATOR)));
    var candidateIdentifier = candidate.candidateIdentifier();

    var payload = createPayload(user);

    givenAuthenticatedRequestAsUser(user)
        .body(payload)
        .when()
        .put(CANDIDATE_ASSIGNEE_PATH, candidateIdentifier)
        .then()
        .statusCode(401);
  }

  private Map<String, String> createPayload(User user) {
    return Map.of(
        "institutionId", user.affiliations().iterator().next(),
        "assignee", user.cristinId());
  }

  private NviCandidate createCandidate(User curator, List<Contributor> contributors) {
    var title = "NVI test candidate " + UUID.randomUUID();
    return CANDIDATE_FACTORY.createCandidate(title, curator, contributors);
  }
}
