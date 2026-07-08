package no.sikt.nva.apitest.publication.identifier.fileupload;

import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadPreparePath;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PrepareApiTest extends FileUploadTestBase {

  @Test
  @DisplayName("file-upload/prepare returns presigned URL")
  @Description("Calling file-upload/prepare should return presigned URL and status code 200 OK")
  void shouldReturnUploadUrlWhenPrepareFile() {
    var identifier = setupDraftPublication();

    var url = createAndPrepareFileUpload(identifier);

    assertThat(url).startsWith("https://nva-resource-storage");
  }

  @Test
  @DisplayName("file-upload/prepare with no authorization")
  @Description(
      "Calling file-upload/prepare with no authorization should return statuscode 401 Unauthorized")
  void shouldReturnUnauthorizedWhenPrepareWithoutAuthorization() {
    var identifier = setupDraftPublication();
    var createResponse = createFileUpload(identifier);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);

    var preparePayload =
        Map.of(NUMBER, "1", UPLOAD_ID, uploadId, KEY, key, BODY, getFileAsString());

    givenUnauthenticatedJsonRequest()
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(401);
  }

  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare with non-existing identifier")
  @Description(
      "Calling file-upload/prepare with non-existing identifier should return statuscode 404 Not"
          + " Found")
  void shouldReturnNotFoundWhenPrepareWithWrongIdentifier() {
    var identifier = UUID.randomUUID().toString();
    var preparePayload =
        Map.of(NUMBER, "1", UPLOAD_ID, "uploadId", KEY, "key", BODY, getFileAsString());

    givenAuthenticatedJsonRequest(getCreatorAccessToken())
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(404);
  }

  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare without file-upload/create")
  @Description(
      "Calling file-upload/prepare without calling file-upload/create should return statuscode 400"
          + " Bad Request")
  void shouldReturnBadRequestWhenPrepareFileWithoutCreate() {
    var identifier = setupDraftPublication();
    var preparePayload =
        Map.of(NUMBER, "1", UPLOAD_ID, "dummyUploadId", KEY, "dummyKey", BODY, getFileAsString());

    givenAuthenticatedJsonRequest(getCreatorAccessToken())
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(400);
  }

  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare with wrong uploadId")
  @Description(
      "Calling file-upload/prepare wrong uploadId should return statuscode 400 Bad Request")
  void shouldReturnBadRequestWhenPrepareFileWithWrongUploadId() {
    var identifier = setupDraftPublication();
    var createResponse = createFileUpload(identifier);
    var key = createResponse.jsonPath().getString(KEY);
    var uploadId = UPLOAD_ID;
    var preparePayload =
        Map.of(NUMBER, "1", UPLOAD_ID, uploadId, KEY, key, BODY, getFileAsString());

    givenAuthenticatedJsonRequest(getCreatorAccessToken())
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(400);
  }

  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare with missing uploadId")
  @Description(
      "Calling file-upload/prepare missing uploadId should return statuscode 400 Bad Request")
  void shouldReturnBadRequestWhenPrepareFileWithMissingUploadId() {
    var identifier = setupDraftPublication();
    var createResponse = createFileUpload(identifier);
    var key = createResponse.jsonPath().getString(KEY);

    var preparePayload = Map.of(NUMBER, "1", KEY, key, BODY, getFileAsString());

    givenAuthenticatedJsonRequest(getCreatorAccessToken())
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(400);
  }

  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare with wrong key")
  @Description("Calling file-upload/prepare wrong key should return statuscode 400 Bad Request")
  void shouldReturnBadRequestWhenPrepareFileWithWrongKey() {
    var identifier = setupDraftPublication();
    var createResponse = createFileUpload(identifier);
    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = KEY;
    var preparePayload =
        Map.of(NUMBER, "1", UPLOAD_ID, uploadId, KEY, key, BODY, getFileAsString());

    givenAuthenticatedJsonRequest(getCreatorAccessToken())
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("file-upload/prepare with missing key")
  @Description("Calling file-upload/prepare missing key should return statuscode 400 Bad Request")
  void shouldReturnBadRequestWhenPrepareFileWithMissingKey() {
    var identifier = setupDraftPublication();
    var createResponse = createFileUpload(identifier);
    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var preparePayload = Map.of(NUMBER, "1", UPLOAD_ID, uploadId, BODY, getFileAsString());

    givenAuthenticatedJsonRequest(getCreatorAccessToken())
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(400);
  }
}
