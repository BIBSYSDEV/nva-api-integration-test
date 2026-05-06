package no.sikt;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;

public class FilePrepareApiTest extends IntegrationTestBase {

  private static final String IDENTIFIER = "identifier";
  private static final String UPLOAD_ID = "uploadId";
  private static final String KEY = "key";
  private static final String NUMBER = "number";
  private static final String BODY = "body";
  private static final String EXAMPLE_FILE = "example.txt";

  private static String creatorAccessToken;
  private static String fileAsString;

  private static Map<String, Object> createFilePayload() {
    try (var resourceStream = PublicationFactory.class.getResourceAsStream("/" + EXAMPLE_FILE)) {
      var fileSize = resourceStream.readAllBytes().length;
      return Map.of(
          "size", Integer.toString(fileSize), MIMETYPE, TEXT_PLAIN, FILE_NAME, EXAMPLE_FILE);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read file " + EXAMPLE_FILE, e);
    }
  }

  private String fileUploadPreparePath(String identifier) {
    return PUBLICATION_PATH + identifier + "/file-upload/prepare";
  }

  @Test
  @DisplayName("file-upload/prepare returns presigned URL")
  @Description("Calling file-upload/prepare should return presigned URL and status code 200 OK")
  void shouldReturnUploadUrlWhenPrepareFile() {
    var identifier = PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER);
    var createResponse = createFileUpload(identifier);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);

    var preparePayload = Map.of(NUMBER, "1", UPLOAD_ID, uploadId, KEY, key, BODY, fileAsString);

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(200)
        .body(URL, notNullValue())
        .body(URL, startsWith("https://nva-resource-storage"));
  }

  @Test
  @DisplayName("file-upload/prepare with no authorization")
  @Description("Calling file-upload/prepare with no authorization should return statuscode 401 Unauthorized")
  void shouldReturnUnauthorizedWhenPrepareWithoutAuthorization() {
    var identifier = PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER);
    var createResponse = createFileUpload(identifier);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);

    var preparePayload = Map.of(NUMBER, "1", UPLOAD_ID, uploadId, KEY, key, BODY, fileAsString);

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
  @Description("Calling file-upload/prepare with non-existing identifier should return statuscode 404 Not"
      + " Found")
  void shouldReturnNotFoundWhenPrepareWithWrongIdentifier() {
    var identifier = UUID.randomUUID().toString();
    var preparePayload = Map.of(NUMBER, "1", UPLOAD_ID, UPLOAD_ID, KEY, KEY, BODY, fileAsString);

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(404);
  }

  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare without file-upload/create")
  @Description("Calling file-upload/prepare without calling file-upload/create should return statuscode 400"
      + " Bad Request")
  void shouldReturnBadRequestWhenPrepareFileWithoutCreate() {
    var identifier = PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER);
    var preparePayload = Map.of(NUMBER, "1", UPLOAD_ID, "dummyUploadId", KEY, "dummyKey", BODY, fileAsString);

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(400);
  }

  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare with wrong uploadId")
  @Description("Calling file-upload/prepare wrong uploadId should return statuscode 400 Bad Request")
  void shouldReturnBadRequestWhenPrepareFileWithWrongUploadId() {
    var identifier = PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER);
    var createResponse = createFileUpload(identifier);
    var key = createResponse.jsonPath().getString(KEY);
    var uploadId = UPLOAD_ID;
    var preparePayload = Map.of(NUMBER, "1", UPLOAD_ID, uploadId, KEY, key, BODY, fileAsString);

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(400);
  }

  @Test
  @Disabled // TODO: Fix bug NP-51209
  @DisplayName("file-upload/prepare with missing uploadId")
  @Description("Calling file-upload/prepare missing uploadId should return statuscode 400 Bad Request")
  void shouldReturnBadRequestWhenPrepareFileWithMissingUploadId() {
    var identifier = PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER);
    var createResponse = createFileUpload(identifier);
    var key = createResponse.jsonPath().getString(KEY);

    var preparePayload = Map.of(NUMBER, "1", KEY, key, BODY, fileAsString);

    givenAuthenticatedJsonRequest(creatorAccessToken)
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
    var identifier = PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER);
    var createResponse = createFileUpload(identifier);
    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = KEY;
    var preparePayload = Map.of(NUMBER, "1", UPLOAD_ID, uploadId, KEY, key, BODY, fileAsString);

    givenAuthenticatedJsonRequest(creatorAccessToken)
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
    var identifier = PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER);
    var createResponse = createFileUpload(identifier);
    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var preparePayload = Map.of(NUMBER, "1", UPLOAD_ID, uploadId, BODY, fileAsString);

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(400);
  }

}
