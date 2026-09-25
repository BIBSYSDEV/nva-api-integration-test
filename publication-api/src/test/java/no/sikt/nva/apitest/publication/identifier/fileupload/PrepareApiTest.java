package no.sikt.nva.apitest.publication.identifier.fileupload;

import static io.restassured.http.Method.POST;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadPreparePath;
import static no.sikt.nva.apitest.publication.file.FileUploadFlow.createFileUpload;
import static no.sikt.nva.apitest.publication.file.FileUploadFlow.prepareFileUpload;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import java.util.UUID;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import no.sikt.nva.apitest.publication.file.MultipartUpload;
import no.sikt.nva.apitest.publication.file.PrepareUploadRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("POST /publication/{identifier}/file-upload/prepare")
class PrepareApiTest extends PublicationTestBase {

  /** Calling file-upload/prepare should return presigned URL and status {@code 200 OK}. */
  @Test
  @DisplayName("file-upload/prepare returns presigned URL")
  @Description(useJavaDoc = true)
  void shouldReturnUploadUrlWhenPrepareFile() {
    var identifier = setupDraftPublication();

    var url = prepareFileUpload(UIB_CREATOR, identifier, createFileUpload(UIB_CREATOR, identifier));

    assertThat(url).startsWith("https://nva-resource-storage");
  }

  /**
   * Calling file-upload/prepare with no authorization should return status {@code 401
   * Unauthorized}.
   */
  @Test
  @DisplayName("file-upload/prepare with no authorization")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenPrepareWithoutAuthorization() {
    var identifier = setupDraftPublication();

    requestShouldReturnUnauthorized(POST, fileUploadPreparePath(identifier));
  }

  /**
   * Calling file-upload/prepare with non-existing identifier should return status {@code 404 Not
   * Found}.
   */
  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare with non-existing identifier")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenPrepareWithWrongIdentifier() {
    var identifier = UUID.randomUUID().toString();
    var unknownUpload = new MultipartUpload("dummyUploadId", "dummyKey");

    requestPrepare(identifier, PrepareUploadRequest.forSinglePart(unknownUpload))
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /**
   * Calling file-upload/prepare without calling file-upload/create should return status {@code 400
   * Bad Request}.
   */
  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare without file-upload/create")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenPrepareFileWithoutCreate() {
    var identifier = setupDraftPublication();
    var uploadNeverCreated = new MultipartUpload("dummyUploadId", "dummyKey");

    requestPrepare(identifier, PrepareUploadRequest.forSinglePart(uploadNeverCreated))
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Calling file-upload/prepare wrong uploadId should return status {@code 400 Bad Request}. */
  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare with wrong uploadId")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenPrepareFileWithWrongUploadId() {
    var identifier = setupDraftPublication();
    var upload = createFileUpload(UIB_CREATOR, identifier);
    var uploadWithWrongId = new MultipartUpload("wrongUploadId", upload.key());

    requestPrepare(identifier, PrepareUploadRequest.forSinglePart(uploadWithWrongId))
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Calling file-upload/prepare missing uploadId should return status {@code 400 Bad Request}. */
  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare with missing uploadId")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenPrepareFileWithMissingUploadId() {
    var identifier = setupDraftPublication();
    var upload = createFileUpload(UIB_CREATOR, identifier);
    var uploadWithoutId = new MultipartUpload(null, upload.key());

    requestPrepare(identifier, PrepareUploadRequest.forSinglePart(uploadWithoutId))
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Calling file-upload/prepare wrong key should return status {@code 400 Bad Request}. */
  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare with wrong key")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenPrepareFileWithWrongKey() {
    var identifier = setupDraftPublication();
    var upload = createFileUpload(UIB_CREATOR, identifier);
    var uploadWithWrongKey = new MultipartUpload(upload.uploadId(), "wrongKey");

    requestPrepare(identifier, PrepareUploadRequest.forSinglePart(uploadWithWrongKey))
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Calling file-upload/prepare missing key should return status {@code 400 Bad Request}. */
  @Test
  @DisplayName("file-upload/prepare with missing key")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenPrepareFileWithMissingKey() {
    var identifier = setupDraftPublication();
    var upload = createFileUpload(UIB_CREATOR, identifier);
    var uploadWithoutKey = new MultipartUpload(upload.uploadId(), null);

    requestPrepare(identifier, PrepareUploadRequest.forSinglePart(uploadWithoutKey))
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  private static Response requestPrepare(String identifier, PrepareUploadRequest prepareRequest) {
    return givenAuthenticatedJsonRequestAsUser(UIB_CREATOR)
        .body(prepareRequest)
        .when()
        .post(fileUploadPreparePath(identifier));
  }
}
