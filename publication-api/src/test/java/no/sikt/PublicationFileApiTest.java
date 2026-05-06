package no.sikt;

import static io.restassured.RestAssured.given;
import static no.sikt.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.Requests.givenUnauthenticatedJsonRequest;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import io.qameta.allure.Description;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class PublicationFileApiTest {

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

  private static final Map<String, String> IDENTIFIER_MAP = new HashMap<>();

  private static final String TITLE_ROOT = "Integration test publication ";
  private static final String CREATE_UPLOAD_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID();
  private static final String CREATE_UPLOAD_PUBLICATION_UNAUTHORIZED_TITLE =
      TITLE_ROOT + UUID.randomUUID();
  private static final String PREPARE_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID();
  private static final String PREPARE_PUBLICATION_NO_CREATE_TITLE = TITLE_ROOT + UUID.randomUUID();
  private static final String PREPARE_PUBLICATION_MISSING_PAYLOAD_TITLE =
      TITLE_ROOT + UUID.randomUUID();
  private static final String PREPARE_PUBLICATION_WRONG_PAYLOAD_TITLE =
      TITLE_ROOT + UUID.randomUUID();
  private static final String PRESIGNED_URL_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID();
  private static final String COMPLETE_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID();
  private static final String COMPLETE_PUBLICATION_UNAUTHORIZED_TITLE =
      TITLE_ROOT + UUID.randomUUID();
  private static final String COMPLETE_PUBLICATION_MISSING_ETAG_TITLE =
      TITLE_ROOT + UUID.randomUUID();

  private static final PublicationFactory PUBLICATION_FACTORY = new PublicationFactory();
  private static final String PUBLICATION_PATH = "/publication/";

  private static String creatorAccessToken;
  private static String fileAsString;
  private static int fileSize;

  private static final Map<String, Object> CREATE_PAYLOAD = new HashMap<>();

  private static void createDraftTestPublication(String title, User user) {
    var identifier =
        PUBLICATION_FACTORY.createDraftPublication(user).jsonPath().getString(IDENTIFIER);
    IDENTIFIER_MAP.put(title, identifier);
  }

  @BeforeAll
  static void init() {

    PUBLICATION_FACTORY.setBaseUriFromParameterStore();
    RestAssured.filters(new AllureRestAssured());
    var logConfig =
        LogConfig.logConfig()
            .enableLoggingOfRequestAndResponseIfValidationFails()
            .blacklistHeaders(List.of("Authorization"));
    RestAssured.config = RestAssured.config().logConfig(logConfig);

    creatorAccessToken = CognitoLogin.login(UserFixtures.UIB_CREATOR.userId()).get("accessToken");

    createDraftTestPublication(CREATE_UPLOAD_PUBLICATION_TITLE, UserFixtures.UIB_CREATOR);
    createDraftTestPublication(
        CREATE_UPLOAD_PUBLICATION_UNAUTHORIZED_TITLE, UserFixtures.UIB_CREATOR);
    createDraftTestPublication(PREPARE_PUBLICATION_TITLE, UserFixtures.UIB_CREATOR);
    createDraftTestPublication(PREPARE_PUBLICATION_NO_CREATE_TITLE, UserFixtures.UIB_CREATOR);
    createDraftTestPublication(PREPARE_PUBLICATION_MISSING_PAYLOAD_TITLE, UserFixtures.UIB_CREATOR);
    createDraftTestPublication(PREPARE_PUBLICATION_WRONG_PAYLOAD_TITLE, UserFixtures.UIB_CREATOR);
    createDraftTestPublication(PRESIGNED_URL_PUBLICATION_TITLE, UserFixtures.UIB_CREATOR);
    createDraftTestPublication(COMPLETE_PUBLICATION_TITLE, UserFixtures.UIB_CREATOR);
    createDraftTestPublication(COMPLETE_PUBLICATION_UNAUTHORIZED_TITLE, UserFixtures.UIB_CREATOR);
    createDraftTestPublication(COMPLETE_PUBLICATION_MISSING_ETAG_TITLE, UserFixtures.UIB_CREATOR);

    try (var resourceStream = PublicationFactory.class.getResourceAsStream("/" + EXAMPLE_FILE)) {
      var bytes = resourceStream.readAllBytes();
      fileSize = bytes.length;
      fileAsString = new String(bytes, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read file " + EXAMPLE_FILE, e);
    }

    CREATE_PAYLOAD.putAll(
        Map.of("size", Integer.toString(fileSize), MIMETYPE, TEXT_PLAIN, FILE_NAME, EXAMPLE_FILE));
  }

  private String fileUploadCreatePath(String identifier) {
    return PUBLICATION_PATH + identifier + "/file-upload/create";
  }

  @Test
  @DisplayName("file-upload/create returns uploadId and key")
  @Description("Calling file-upload/create should return uploadId and key with statuscode 200")
  void shouldReturnUploadIdAndKeyWhenCreatingFileUpload() {
    var identifier = IDENTIFIER_MAP.get(CREATE_UPLOAD_PUBLICATION_TITLE);

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
    var identifier = IDENTIFIER_MAP.get(CREATE_UPLOAD_PUBLICATION_UNAUTHORIZED_TITLE);

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
    var identifier = IDENTIFIER_MAP.get(PREPARE_PUBLICATION_TITLE);
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
    var identifier = IDENTIFIER_MAP.get(PREPARE_PUBLICATION_TITLE);
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
    var identifier = IDENTIFIER_MAP.get(PREPARE_PUBLICATION_NO_CREATE_TITLE);
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
    var identifier = IDENTIFIER_MAP.get(PREPARE_PUBLICATION_WRONG_PAYLOAD_TITLE);
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
    var identifier = IDENTIFIER_MAP.get(PREPARE_PUBLICATION_WRONG_PAYLOAD_TITLE);
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
    var identifier = IDENTIFIER_MAP.get(PREPARE_PUBLICATION_WRONG_PAYLOAD_TITLE);
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
    var identifier = IDENTIFIER_MAP.get(PREPARE_PUBLICATION_WRONG_PAYLOAD_TITLE);
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
    var identifier = IDENTIFIER_MAP.get(PRESIGNED_URL_PUBLICATION_TITLE);
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
    var identifier = IDENTIFIER_MAP.get(COMPLETE_PUBLICATION_TITLE);
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
    var identifier = IDENTIFIER_MAP.get(COMPLETE_PUBLICATION_UNAUTHORIZED_TITLE);
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
    var identifier = IDENTIFIER_MAP.get(COMPLETE_PUBLICATION_UNAUTHORIZED_TITLE);
    var createResponse = createFileUpload(identifier);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);
    prepareAndUpload(identifier, uploadId, key);

    givenUnauthenticatedJsonRequest()
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
    var createPayload =
        Map.of("size", Integer.toString(fileSize), MIMETYPE, TEXT_PLAIN, FILE_NAME, EXAMPLE_FILE);

    return givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(createPayload)
        .when()
        .post(fileUploadCreatePath(identifier))
        .then()
        .statusCode(200)
        .extract()
        .response();
  }
}
