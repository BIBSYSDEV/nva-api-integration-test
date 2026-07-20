package no.sikt.nva.apitest.publication.identifier.fileupload;

import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadCreatePath;

import io.qameta.allure.Description;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class CreateApiTest extends FileUploadTestBase {

  private static final String UPLOAD_ID = "uploadId";
  private static final String KEY = "key";

  @Test
  @DisplayName("file-upload/create returns uploadId and key")
  @Description("Calling file-upload/create should return uploadId and key with statuscode 200")
  void shouldReturnUploadIdAndKeyWhenCreatingFileUpload(SoftAssertions softly) {
    var identifier = setupDraftPublication();

    var response =
        givenAuthenticatedJsonRequest(getCreatorAccessToken())
            .body(CREATE_PAYLOAD)
            .when()
            .post(fileUploadCreatePath(identifier))
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString(UPLOAD_ID)).isNotNull();
    softly.assertThat(response.getString(KEY)).isNotNull();
  }

  @Test
  @DisplayName("file-upload/create with no authorization")
  @Description(
      "Calling file-upload/create with no authorization should return statuscode 401 Unauthorized")
  void shouldReturnUnauthorizedWhenCreateWithoutAuthorization() {
    var identifier = setupDraftPublication();

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
