package no.sikt.nva.apitest.publication.identifier.fileupload;

import static io.restassured.http.Method.POST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadCreatePath;

import io.qameta.allure.Description;
import java.util.UUID;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import no.sikt.nva.apitest.publication.file.CreateUploadRequest;
import no.sikt.nva.apitest.publication.file.MultipartUpload;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST /publication/{identifier}/file-upload/create")
class CreateApiTest extends PublicationTestBase {

  /** Calling file-upload/create should return uploadId and key with status {@code 200 OK}. */
  @Test
  @DisplayName("file-upload/create returns uploadId and key")
  @Description(useJavaDoc = true)
  void shouldReturnUploadIdAndKeyWhenCreatingFileUpload(SoftAssertions softly) {
    var identifier = setupDraftPublication();

    var upload =
        givenAuthenticatedJsonRequestAsUser(UIB_CREATOR)
            .body(CreateUploadRequest.forExampleFile())
            .when()
            .post(fileUploadCreatePath(identifier))
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .as(MultipartUpload.class);

    softly.assertThat(upload.uploadId()).isNotNull();
    softly.assertThat(upload.key()).isNotNull();
  }

  /**
   * Calling file-upload/create with no authorization should return status {@code 401 Unauthorized}.
   */
  @Test
  @DisplayName("file-upload/create with no authorization")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenCreateWithoutAuthorization() {
    var identifier = setupDraftPublication();

    requestShouldReturnUnauthorized(POST, fileUploadCreatePath(identifier));
  }

  /**
   * Calling file-upload/create with non-existing identifier should return status {@code 404 Not
   * Found}.
   */
  @Test
  @DisplayName("file-upload/create with non-existing identifier")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenCreateWithNonExistingIdentifier() {
    var identifier = UUID.randomUUID().toString();

    givenAuthenticatedJsonRequestAsUser(UIB_CREATOR)
        .body(CreateUploadRequest.forExampleFile())
        .when()
        .post(fileUploadCreatePath(identifier))
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }
}
