package no.sikt.nva.apitest.publication.identifier;

import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequest;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static org.hamcrest.Matchers.equalTo;

import io.qameta.allure.Description;
import java.util.UUID;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.base.UserFixtures;
import no.sikt.nva.apitest.publication.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class DeleteApiTest extends IntegrationTestBase {

  private static final String IDENTIFIER = "identifier";
  private static String creatorAccessToken;

  @BeforeAll
  static void init() {
    creatorAccessToken = CognitoLogin.login(UserFixtures.UIB_CREATOR.userId()).get("accessToken");
  }

  @Test
  @DisplayName("Delete draft publication")
  @Description("A Creator calling delete on own publication should return 202 Accepted")
  void shouldDeleteDraftWhenRequestedByOwner() {
    var identifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);

    givenAuthenticatedRequest(creatorAccessToken)
        .when()
        .delete(PUBLICATION_PATH + identifier)
        .then()
        .statusCode(202);
  }

  @Test
  @DisplayName("Deleting non-existing draft publication")
  @Description("A Creator calling delete on non-existing publication should return 404 Not Found")
  void shouldReturnNotFoundWhenDeletingUnknownIdentifier() {

    givenAuthenticatedRequest(creatorAccessToken)
        .when()
        .delete(PUBLICATION_PATH + UUID.randomUUID())
        .then()
        .statusCode(404);
  }

  @Test
  @DisplayName("Non authorized user tries to delete publication")
  @Description("A non authorized user calling delete should return 401 Unauthorized")
  void shouldReturnUnauthorizedWhenDeletingWithoutAuthentication() {
    var identifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);

    givenUnauthenticatedJsonRequest()
        .when()
        .delete(PUBLICATION_PATH + identifier)
        .then()
        .statusCode(401)
        .body("message", equalTo("Unauthorized"));
  }
}
