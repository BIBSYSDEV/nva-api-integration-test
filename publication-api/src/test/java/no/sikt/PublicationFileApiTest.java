package no.sikt;

import static io.restassured.RestAssured.given;
import static no.sikt.Requests.givenAuthenticatedJsonRequest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

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
import org.junit.jupiter.api.Test;

import io.restassured.http.Header;

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

    String createUploadIdentifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .get(IDENTIFIER);
    IDENTIFIER_MAP.put(CREATE_UPLOAD_PUBLICATION_TITLE, createUploadIdentifier);

    String prepareIdentifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .get(IDENTIFIER);
    IDENTIFIER_MAP.put(PREPARE_PUBLICATION_TITLE, prepareIdentifier);

    String presignedUrlIdentifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .get(IDENTIFIER);
    IDENTIFIER_MAP.put(PRESIGNED_URL_PUBLICATION_TITLE, presignedUrlIdentifier);

    String completeIdentifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .get(IDENTIFIER);
    IDENTIFIER_MAP.put(COMPLETE_PUBLICATION_TITLE, completeIdentifier);
  }

  private Map<String, Object> createFilePayload(String fileName) {
    try (var resourceStream =
        PublicationFactory.class.getResourceAsStream(
            fileName.startsWith("/") ? fileName : "/" + fileName)) {
      var fileSize = resourceStream.readAllBytes().length;
      return Map.of("size", Integer.toString(fileSize), MIMETYPE, TEXT_PLAIN, FILE_NAME, fileName);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to read " + fileName, e);
    }
  }

  @Test
  void shouldReturnUploadIdAndKeyWhenCreatingFileUpload() {
    var identifier = IDENTIFIER_MAP.get(CREATE_UPLOAD_PUBLICATION_TITLE);
    var payload = createFilePayload(EXAMPLE_FILE);

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
    var createResponse = getCreateResponse(identifier, EXAMPLE_FILE);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);

    try (var resourceStream = PublicationFactory.class.getResourceAsStream("/" + EXAMPLE_FILE)) {
      var fileAsString = new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
      Map<String, Object> preparePayload =
          Map.of(NUMBER, "1", UPLOAD_ID, uploadId, KEY, key, BODY, fileAsString);

      givenAuthenticatedJsonRequest(creatorAccessToken)
          .body(preparePayload)
          .when()
          .post(PUBLICATION_PATH + identifier + "/file-upload/prepare")
          .then()
          .statusCode(200)
          .body(URL, notNullValue())
          .body(URL, startsWith("https://nva-resource-storage"));
    } catch (IOException e) {
      throw new IllegalArgumentException(FILE_ERROR_MESSAGE, e);
    }
  }

  @Test
  void shouldReturnEtagInHeaderWhenPostingToPresignedUrl() {
    var identifier = IDENTIFIER_MAP.get(COMPLETE_PUBLICATION_TITLE);
    var createResponse = getCreateResponse(identifier, EXAMPLE_FILE);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);

    try (var resourceStream = PublicationFactory.class.getResourceAsStream("/" + EXAMPLE_FILE)) {
      var fileAsString = new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
      var uploadResponse = getPrepareResponse(identifier, uploadId, key, fileAsString);

      var uploadUrl = uploadResponse.jsonPath().getString(URL);
      uploadUrl = URLDecoder.decode(uploadUrl, StandardCharsets.UTF_8);
      Map<String, Object> presignedPayload = Map.of("data", fileAsString);

      given()
          .accept(ContentType.TEXT)
          .contentType(ContentType.JSON)
          .body(presignedPayload)
          .when()
          .put(uploadUrl)
          .then()
          .statusCode(200)
          .header("ETag", notNullValue());

    } catch (IOException e) {
      throw new IllegalArgumentException(FILE_ERROR_MESSAGE, e);
    }
  }

  @Test
  void shouldReturnFileMetaDataWhenCompleteUpload() {
    var identifier = IDENTIFIER_MAP.get(COMPLETE_PUBLICATION_TITLE);
    var createResponse = getCreateResponse(identifier, EXAMPLE_FILE);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);

    try (var resourceStream = PublicationFactory.class.getResourceAsStream("/" + EXAMPLE_FILE)) {
      var fileAsString = new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
      var uploadResponse = getPrepareResponse(identifier, uploadId, key, fileAsString);

      var uploadUrl = uploadResponse.jsonPath().getString(URL);
      uploadUrl = URLDecoder.decode(uploadUrl, StandardCharsets.UTF_8);
      var presignedResponse = getPresignedResponse(fileAsString, uploadUrl);
      var eTag = presignedResponse.headers().getValue("ETag");

      var today =
          LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
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
    } catch (IOException e) {
      throw new IllegalArgumentException(FILE_ERROR_MESSAGE, e);
    }
  }

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
        .post(PUBLICATION_PATH + identifier + "/file-upload/complete")
        .then()
        .statusCode(200)
        .extract()
        .response();
  }

  private Response getPresignedResponse(String fileAsString, String uploadUrl) {
    var presignedPayload = Map.of("data", fileAsString);
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

  private Response getPrepareResponse(
      String identifier, String uploadId, String key, String fileAsString) {
    var preparePayload =
        Map.of(NUMBER, "1", UPLOAD_ID, uploadId, KEY, key, BODY, fileAsString);

    return givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(preparePayload)
        .when()
        .post(PUBLICATION_PATH + identifier + "/file-upload/prepare")
        .then()
        .statusCode(200)
        .extract()
        .response();
  }

  private Response getCreateResponse(String identifier, String fileName) {
    Map<String, Object> createPayload = createFilePayload(fileName);

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
