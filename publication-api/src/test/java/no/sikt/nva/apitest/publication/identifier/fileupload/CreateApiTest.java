package no.sikt.nva.apitest.publication.identifier.fileupload;

import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static org.hamcrest.Matchers.notNullValue;

import io.qameta.allure.Description;
import java.util.UUID;
import no.sikt.nva.apitest.base.UserFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class CreateApiTest extends FileUploadTestBase {

  private static final String UPLOAD_ID = "uploadId";
  private static final String KEY = "key";
  private static final String IDENTIFIER = "identifier";

  @Test
  @DisplayName("file-upload/create returns uploadId and key")
  @Description("Calling file-upload/create should return uploadId and key with statuscode 200")
  void shouldReturnUploadIdAndKeyWhenCreatingFileUpload() {
    var identifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);

    givenAuthenticatedJsonRequest(getCreatorAccessToken())
        .body(CREATE_PAYLOAD)
        .when()
        .post(fileUploadCreatePath(identifier))
        .then()
        .statusCode(200)
        .body(UPLOAD_ID, notNullValue())
        .body(KEY, notNullValue());
  }

  @Test
  @DisplayName("file-upload/create with no authorization")
  @Description(
      "Calling file-upload/create with no authorization should return statuscode 401 Unauthorized")
  void shouldReturnUnauthorizedWhenCreateWithoutAuthorization() {
    var identifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);

    givenUnauthenticatedJsonRequest()
        .body(CREATE_PAYLOAD)
        .when()
        .post(fileUploadCreatePath(identifier))
        .then()
        .statusCode(401);
  }

  @Test
  @DisplayName("file-upload/create with non-existing identifier")
  @Description(
      "Calling file-upload/create with non-existing identifier should return statuscode 404 Not"
          + " Found")
  void shouldReturnNotFoundWhenCreateWithNonExistingIdentifier() {
    var identifier = UUID.randomUUID().toString();

    givenAuthenticatedJsonRequest(getCreatorAccessToken())
        .body(CREATE_PAYLOAD)
        .when()
        .post(fileUploadCreatePath(identifier))
        .then()
        .statusCode(404);
  }
}
