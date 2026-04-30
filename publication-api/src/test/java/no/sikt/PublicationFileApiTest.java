package no.sikt;

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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import static no.sikt.Requests.givenAuthenticatedJsonRequest;

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
  private static final String FILE_ERROR_MESSAGE = "Unable to read " + EXAMPLE_FILE;

  private static final Map<String, String> IDENTIFIER_MAP = new HashMap<>();

  private static final String TITLE_ROOT = "Integration test publication ";
  private static final String CREATE_UPLOAD_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID();
  private static final String PREPARE_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID();
  private static final String PRESIGNED_URL_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID();
  private static final String COMPLETE_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID();

  private static final PublicationFactory PUBLICATION_FACTORY = new PublicationFactory();
  private static final String PUBLICATION_PATH = "/publication/";

  private static String creatorAccessToken;
  private static String fileAsString;
  private static int fileSize;

  @BeforeAll
  static void init() {

    PUBLICATION_FACTORY.setBaseUriFromParameterStore();
    RestAssured.filters(new AllureRestAssured());
    var logConfig = LogConfig.logConfig()
        .enableLoggingOfRequestAndResponseIfValidationFails()
        .blacklistHeaders(List.of("Authorization"));
    RestAssured.config = RestAssured.config().logConfig(logConfig);

    creatorAccessToken = CognitoLogin.login(UserFixtures.UIB_CREATOR.userId()).get("accessToken");

    var createUploadIdentifier = PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER);
    IDENTIFIER_MAP.put(CREATE_UPLOAD_PUBLICATION_TITLE, createUploadIdentifier);

    var prepareIdentifier = PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER);
    IDENTIFIER_MAP.put(PREPARE_PUBLICATION_TITLE, prepareIdentifier);

    var presignedUrlIdentifier = PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER);
    IDENTIFIER_MAP.put(PRESIGNED_URL_PUBLICATION_TITLE, presignedUrlIdentifier);

    var completeIdentifier = PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER);
    IDENTIFIER_MAP.put(COMPLETE_PUBLICATION_TITLE, completeIdentifier);

    try (var resourceStream = PublicationFactory.class.getResourceAsStream("/" + EXAMPLE_FILE)) {
      fileSize = resourceStream.readAllBytes().length;
      fileAsString = new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read file " + EXAMPLE_FILE);
    }
  }

  @Test
  void shouldReturnUploadIdAndKeyWhenCreatingFileUpload() {
    var identifier = IDENTIFIER_MAP.get(CREATE_UPLOAD_PUBLICATION_TITLE);
    var payload = Map.of("size", Integer.toString(fileSize), MIMETYPE, TEXT_PLAIN, FILE_NAME, EXAMPLE_FILE);

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(payload)
        .when()
        .post(PUBLICATION_PATH + identifier + "/file-upload/create")
        .then()
        .statusCode(200)
        .body(UPLOAD_ID, notNullValue())
        .body(KEY, notNullValue());
  }

  @Test
  void shouldReturnUploadUrlWhenPrepareFile() {
    var identifier = IDENTIFIER_MAP.get(PREPARE_PUBLICATION_TITLE);
    var createResponse = createFileUpload(identifier);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);

    var preparePayload = Map.of(NUMBER, "1", UPLOAD_ID, uploadId, KEY, key, BODY, fileAsString);

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(preparePayload)
        .when()
        .post(PUBLICATION_PATH + identifier + "/file-upload/prepare")
        .then()
        .statusCode(200)
        .body(URL, notNullValue())
        .body(URL, startsWith("https://nva-resource-storage"));
  }

  @Test
  void shouldReturnEtagInHeaderWhenPostingToPresignedUrl() {
    var identifier = IDENTIFIER_MAP.get(PRESIGNED_URL_PUBLICATION_TITLE);
    var uploadUrl = createToPrepareFileUpload(identifier);
    var presignedPayload = Map.of("data", fileAsString);

    given()
        .accept(ContentType.TEXT)
        .contentType(ContentType.JSON)
        .body(presignedPayload)
        .when()
        .put(uploadUrl)
        .then()
        .statusCode(200)
        .header("ETag", notNullValue());

  }

  @Test
  void shouldReturnFileMetaDataWhenCompleteUpload() {
    var identifier = IDENTIFIER_MAP.get(COMPLETE_PUBLICATION_TITLE);
    var createResponse = createFileUpload(identifier);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);

    var uploadUrl = createToPrepareFileUpload(identifier);

    var presignedResponse = uploadToPresignedUrl(uploadUrl);
    var eTag = presignedResponse.headers().getValue("ETag");

    var today = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    var parts = Map.of("etag", eTag, "partNumber", "1");
    var completePayload = Map.of(
        UPLOAD_ID, uploadId, KEY, key, TYPE, "InternalCompleteUpload", PARTS, List.of(parts));

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(completePayload)
        .when()
        .post(PUBLICATION_PATH + identifier + "/file-upload/complete")
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

  // TODO needed for delete tests
  @SuppressWarnings("unused")
  private Response completeUpload(String identifier, String uploadId, String key, String eTag) {
    Map<String, Object> parts = Map.of("etag", eTag, "partNumber", "1");
    Map<String, Object> completePayload = Map.of(
        UPLOAD_ID,
        uploadId,
        KEY,
        key,
        TYPE,
        "InternalCompleteUpload",
        PARTS,
        new Object[] { parts });

    return givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(completePayload)
        .when()
        .post(PUBLICATION_PATH + identifier + "/file-upload/complete")
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
    var url = givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(preparePayload)
        .when()
        .post(PUBLICATION_PATH + identifier + "/file-upload/prepare")
        .then()
        .statusCode(200)
        .extract()
        .response().jsonPath().getString(URL);

    return URLDecoder.decode(url, StandardCharsets.UTF_8);
  }

  private String createToPrepareFileUpload(String identifier) {

    var createResponse = createFileUpload(identifier);
    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);

    return prepareFileUpload(identifier, uploadId, key);

  }

  private Response createFileUpload(String identifier) {
    var createPayload = Map.of("size", Integer.toString(fileSize), MIMETYPE, TEXT_PLAIN, FILE_NAME, EXAMPLE_FILE);

    return givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(createPayload)
        .when()
        .post(PUBLICATION_PATH + identifier + "/file-upload/create")
        .then()
        .statusCode(200)
        .extract()
        .response();
  }
}
