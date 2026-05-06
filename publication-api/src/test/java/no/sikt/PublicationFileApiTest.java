package no.sikt;

import static io.restassured.RestAssured.given;
import static no.sikt.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.Requests.givenUnauthenticatedJsonRequest;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class PublicationFileApiTest extends IntegrationTestBase {

  private static final String URL = "url";
  private static final String PARTS = "parts";
  private static final String BODY = "body";
  private static final String NUMBER = "number";
  private static final String FILE_NAME = "filename";
  private static final String TEXT_PLAIN = "text/plain";
  private static final String MIMETYPE = "mimetype";
  private static final String UPLOAD_ID = "uploadId";
  private static final String KEY = "key";
  private static final String TYPE = "type";
  private static final String IDENTIFIER = "identifier";

  private static final String EXAMPLE_FILE = "example.txt";

  private static final PublicationFactory PUBLICATION_FACTORY = new PublicationFactory();
  private static final String PUBLICATION_PATH = "/publication/";

  private static String creatorAccessToken;
  private static String fileAsString;

  private static final Map<String, Object> CREATE_PAYLOAD = new HashMap<>();

  private static Map<String, Object> createFilePayload() {
    try (var resourceStream = PublicationFactory.class.getResourceAsStream("/" + EXAMPLE_FILE)) {
      var bytes = resourceStream.readAllBytes();
      var fileSize = bytes.length;
      fileAsString = new String(bytes, StandardCharsets.UTF_8);
      return Map.of(
          "size", Integer.toString(fileSize), MIMETYPE, TEXT_PLAIN, FILE_NAME, EXAMPLE_FILE);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read file " + EXAMPLE_FILE, e);
    }
  }

  @BeforeAll
  static void init() {
    creatorAccessToken = CognitoLogin.login(UserFixtures.UIB_CREATOR.userId()).get("accessToken");
    CREATE_PAYLOAD.putAll(createFilePayload());
  }

  private String fileUploadCreatePath(String identifier) {
    return PUBLICATION_PATH + identifier + "/file-upload/create";
  }

  @Test
  @DisplayName("file-upload/create returns uploadId and key")
  @Description("Calling file-upload/create should return uploadId and key with statuscode 200")
  void shouldReturnUploadIdAndKeyWhenCreatingFileUpload() {
    var identifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);

    givenAuthenticatedJsonRequest(creatorAccessToken)
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

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(CREATE_PAYLOAD)
        .when()
        .post(fileUploadCreatePath(identifier))
        .then()
        .statusCode(404);
  }

  private String fileUploadPreparePath(String identifier) {
    return PUBLICATION_PATH + identifier + "/file-upload/prepare";
  }

  @Test
  @DisplayName("file-upload/prepare returns presigned URL")
  @Description("Calling file-upload/prepare should return presigned URL and status code 200 OK")
  void shouldReturnUploadUrlWhenPrepareFile() {
    var identifier =
        PUBLICATION_FACTORY
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
  @Description(
      "Calling file-upload/prepare with no authorization should return statuscode 401 Unauthorized")
  void shouldReturnUnauthorizedWhenPrepareWithoutAuthorization() {
    var identifier =
        PUBLICATION_FACTORY
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
  @DisplayName("file-upload/prepare with non-existing identifier")
  @Description(
      "Calling file-upload/prepare with non-existing identifier should return statuscode 404 Not"
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
  @DisplayName("file-upload/prepare without file-upload/create")
  @Description(
      "Calling file-upload/prepare without calling file-upload/create should return statuscode 400"
          + " Bad Request")
  void shouldReturnBadRequestWhenPrepareFileWithoutCreate() {
    var identifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);
    var preparePayload = Map.of(NUMBER, "1", UPLOAD_ID, UPLOAD_ID, KEY, KEY, BODY, fileAsString);

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(preparePayload)
        .when()
        .post(fileUploadPreparePath(identifier))
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("file-upload/prepare with wrong uploadId")
  @Description(
      "Calling file-upload/prepare wrong uploadId should return statuscode 400 Bad Request")
  void shouldReturnBadRequestWhenPrepareFileWithWrongUploadId() {
    var identifier =
        PUBLICATION_FACTORY
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
  @DisplayName("file-upload/prepare with missing uploadId")
  @Description(
      "Calling file-upload/prepare missing uploadId should return statuscode 400 Bad Request")
  void shouldReturnBadRequestWhenPrepareFileWithMissingUploadId() {
    var identifier =
        PUBLICATION_FACTORY
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
  @DisplayName("file-upload/prepare with wrong key")
  @Description("Calling file-upload/prepare wrong key should return statuscode 400 Bad Request")
  void shouldReturnBadRequestWhenPrepareFileWithWrongKey() {
    var identifier =
        PUBLICATION_FACTORY
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
    var identifier =
        PUBLICATION_FACTORY
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

  @Test
  @DisplayName("Presigned url")
  @Description("Calling presigned url should return ETag and statuscode 200")
  void shouldReturnEtagInHeaderWhenPostingToPresignedUrl() {
    var identifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);
    var uploadUrl = createAndPrepareFileUpload(identifier);
    var presignedPayload = Map.of("data", fileAsString);

    givenUnauthenticatedJsonRequest()
        .body(presignedPayload)
        .when()
        .put(uploadUrl)
        .then()
        .statusCode(200)
        .header("ETag", notNullValue());
  }

  private String fileUploadCompletePath(String identifier) {
    return PUBLICATION_PATH + identifier + "/file-upload/complete";
  }

  private Map<String, Object> completePayload(String uploadId, String key, String eTag) {
    var parts = Map.of("etag", eTag, "partNumber", "1");
    return Map.of(
        UPLOAD_ID, uploadId, KEY, key, TYPE, "InternalCompleteUpload", PARTS, List.of(parts));
  }

  @Test
  @DisplayName("file-upload/complete returns file metadata")
  @Description("Calling file-upload/complete should return file metadata and statuscode 200")
  void shouldReturnFileMetaDataWhenCompleteUpload() {
    var identifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);
    var createResponse = createFileUpload(identifier);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);
    var eTag = prepareAndUpload(identifier, uploadId, key);

    var today =
        LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(completePayload(uploadId, key, eTag))
        .when()
        .post(fileUploadCompletePath(identifier))
        .then()
        .statusCode(200)
        .body(TYPE, equalTo("UploadedFile"))
        .body("identifier", notNullValue())
        .body("name", equalTo(EXAMPLE_FILE))
        .body("mimeType", equalTo(TEXT_PLAIN))
        .appendRootPath("rightsRetentionStrategy")
        .body(TYPE, equalTo("NullRightsRetentionStrategy"))
        .body("configuredType", equalTo("NullRightsRetentionStrategy"))
        .detachRootPath("rightsRetentionStrategy")
        .appendRootPath("uploadDetails")
        .body(TYPE, equalTo("UserUploadDetails"))
        .body("uploadedBy", equalTo(UserFixtures.UIB_CREATOR.cristinId()))
        .body("uploadedDate", startsWith(today));
  }

  @Test
  @DisplayName("file-upload/complete with no authorization")
  @Description(
      "Calling file-upload/complete with no authorization should return statuscode 401"
          + " Unauthorized")
  void shouldReturnUnauthorizedWhenCompleteWithoutAuthorization() {
    var identifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);
    var createResponse = createFileUpload(identifier);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);
    var eTag = prepareAndUpload(identifier, uploadId, key);

    givenUnauthenticatedJsonRequest()
        .body(completePayload(uploadId, key, eTag))
        .when()
        .post(fileUploadCompletePath(identifier))
        .then()
        .statusCode(401);
  }

  @Test
  @DisplayName("file-upload/complete with missing ETag")
  @Description("Calling file-upload/complete with missing ETag should return 401 Unauthorized")
  void shouldReturnUnauthorizedWhenCompleteWithMissingETag() {
    var identifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);
    var createResponse = createFileUpload(identifier);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);
    prepareAndUpload(identifier, uploadId, key);

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(completePayload(uploadId, key, ""))
        .when()
        .post(fileUploadCompletePath(identifier))
        .then()
        .statusCode(401);
  }

  // TODO needed for delete tests
  @SuppressWarnings("unused")
  private Response completeUpload(String identifier, String uploadId, String key, String eTag) {
    Map<String, Object> parts = Map.of("etag", eTag, "partNumber", "1");
    Map<String, Object> completePayload =
        Map.of(
            UPLOAD_ID,
            uploadId,
            KEY,
            key,
            TYPE,
            "InternalCompleteUpload",
            PARTS,
            new Object[] {parts});

    return givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(completePayload)
        .when()
        .post(fileUploadCompletePath(identifier))
        .then()
        .statusCode(200)
        .extract()
        .response();
  }

  private Response uploadToPresignedUrl(String uploadUrl) {
    Map<String, Object> presignedPayload = Map.of("data", fileAsString);
    return given()
        .accept(ContentType.TEXT)
        .contentType(ContentType.JSON)
        .body(presignedPayload)
        .when()
        .put(uploadUrl)
        .then()
        .statusCode(200)
        .extract()
        .response();
  }

  private String prepareFileUpload(String identifier, String uploadId, String key) {
    var preparePayload = Map.of(NUMBER, "1", UPLOAD_ID, uploadId, KEY, key, BODY, fileAsString);
    var url =
        givenAuthenticatedJsonRequest(creatorAccessToken)
            .body(preparePayload)
            .when()
            .post(fileUploadPreparePath(identifier))
            .then()
            .statusCode(200)
            .extract()
            .response()
            .jsonPath()
            .getString(URL);

    return URLDecoder.decode(url, StandardCharsets.UTF_8);
  }

  private String createAndPrepareFileUpload(String identifier) {

    var createResponse = createFileUpload(identifier);
    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);

    return prepareFileUpload(identifier, uploadId, key);
  }

  private String prepareAndUpload(String identifier, String uploadId, String key) {

    var url = prepareFileUpload(identifier, uploadId, key);

    return uploadToPresignedUrl(url).headers().getValue("ETag");
  }

  private Response createFileUpload(String identifier) {

    return givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(CREATE_PAYLOAD)
        .when()
        .post(fileUploadCreatePath(identifier))
        .then()
        .statusCode(200)
        .extract()
        .response();
  }

  // Removes attachments from Allure report so as to not expose headers
  @AfterEach
  void removeAttachments() {
    Allure.getLifecycle()
        .updateTestCase(testResult -> testResult.setAttachments(new ArrayList<>()));
  }
}
