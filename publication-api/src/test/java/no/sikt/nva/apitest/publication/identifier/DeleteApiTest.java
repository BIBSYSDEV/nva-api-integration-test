package no.sikt.nva.apitest.publication.identifier;

import static io.restassured.http.Method.DELETE;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationPaths.publicationPath;

import io.qameta.allure.Description;
import java.util.UUID;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DELETE /publication/{identifier}")
class DeleteApiTest extends PublicationTestBase {

  /** A Creator calling delete on own publication should return status {@code 202 Accepted}. */
  @Test
  @DisplayName("Delete draft publication")
  @Description(useJavaDoc = true)
  void shouldDeleteDraftWhenRequestedByOwner() {
    var identifier = setupDraftPublication();

    givenAuthenticatedRequestAsUser(UIB_CREATOR)
        .when()
        .delete(publicationPath(identifier))
        .then()
        .statusCode(HTTP_ACCEPTED);
  }

  /**
   * A Creator calling delete on non-existing publication should return status {@code 404 Not
   * Found}.
   */
  @Test
  @DisplayName("Deleting non-existing draft publication")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenDeletingUnknownIdentifier() {

    givenAuthenticatedRequestAsUser(UIB_CREATOR)
        .when()
        .delete(publicationPath(UUID.randomUUID().toString()))
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** A non authenticated call to delete should return status {@code 401 Unauthorized}. */
  @Test
  @DisplayName("Non authorized user tries to delete publication")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenDeletingWithoutAuthentication() {
    var identifier = setupDraftPublication();

    requestShouldReturnUnauthorized(DELETE, publicationPath(identifier));
  }

  /** A non authorized user calling delete should return status {@code 403 Forbidden}. */
  @Test
  @DisplayName("Non authorized user tries to delete publication")
  @Disabled("FIXME: Returns 401, see NP-51618")
  @Description(useJavaDoc = true)
  void shouldReturnForbiddemWhenNotOwnerDeletingDraftPublication() {
    var identifier = setupDraftPublication();

    requestShouldReturnForbidden(DELETE, UIB_CONTRIBUTOR, publicationPath(identifier));
  }
}
