package no.sikt.nva.apitest.publication.identifier;

import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequest;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationPaths.publicationPath;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import java.util.UUID;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class DeleteApiTest extends PublicationTestBase {

  private static String creatorAccessToken;

  @BeforeAll
  static void init() {
    creatorAccessToken = CognitoLogin.login(UIB_CREATOR.userId()).get("accessToken");
  }

  @Test
  @DisplayName("Delete draft publication")
  @Description("A Creator calling delete on own publication should return 202 Accepted")
  void shouldDeleteDraftWhenRequestedByOwner() {
    var identifier = setupDraftPublication();

    givenAuthenticatedRequest(creatorAccessToken)
        .when()
        .delete(publicationPath(identifier))
        .then()
        .statusCode(202);
  }

  @Test
  @DisplayName("Deleting non-existing draft publication")
  @Description("A Creator calling delete on non-existing publication should return 404 Not Found")
  void shouldReturnNotFoundWhenDeletingUnknownIdentifier() {

    givenAuthenticatedRequest(creatorAccessToken)
        .when()
        .delete(publicationPath(UUID.randomUUID().toString()))
        .then()
        .statusCode(404);
  }

  @Test
  @DisplayName("Non authorized user tries to delete publication")
  @Description("A non authorized user calling delete should return 401 Unauthorized")
  void shouldReturnUnauthorizedWhenDeletingWithoutAuthentication() {
    var identifier = setupDraftPublication();

    var response =
        givenUnauthenticatedJsonRequest()
            .when()
            .delete(publicationPath(identifier))
            .then()
            .statusCode(401)
            .extract()
            .jsonPath();

    assertThat(response.getString("message")).isEqualTo("Unauthorized");
  }
}
