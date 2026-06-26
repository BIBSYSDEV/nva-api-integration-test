package no.sikt.nva.apitest.publication.identifier.fileupload;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import no.sikt.nva.PublicationFactory;
import no.sikt.nva.apitest.base.CognitoLogin;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadCompletePath;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadCreatePath;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadPreparePath;
import no.sikt.nva.apitest.publication.PublicationTestBase;

public class FileUploadTestBase extends PublicationTestBase {

  protected static final String URL = "url";
  protected static final String PARTS = "parts";
  protected static final String BODY = "body";
  protected static final String NUMBER = "number";
  private static final String FILE_NAME = "filename";
  protected static final String TEXT_PLAIN = "text/plain";
  private static final String MIMETYPE = "mimetype";
  protected static final String UPLOAD_ID = "uploadId";
  protected static final String KEY = "key";
  protected static final String TYPE = "type";

  private static final String EXAMPLE_FILE = "example.txt";

  private static String creatorAccessToken;
  private static String fileAsString;

  public static final Map<String, Object> CREATE_PAYLOAD = new HashMap<>();

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

  public static String getCreatorAccessToken() {
    return creatorAccessToken;
  }

  public static String getFileAsString() {
    return fileAsString;
  }

  @BeforeAll
  public static void initFileTest() {
    CREATE_PAYLOAD.putAll(createFilePayload());
    creatorAccessToken = CognitoLogin.login(UIB_CREATOR.userId()).get("accessToken");
  }

  // TODO needed for delete tests
  @SuppressWarnings("unused")
  public Response completeUpload(String identifier, String uploadId, String key, String eTag) {
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

  public Response uploadToPresignedUrl(String uploadUrl) {
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

  public String prepareFileUpload(String identifier, String uploadId, String key) {
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

  public String createAndPrepareFileUpload(String identifier) {

    var createResponse = createFileUpload(identifier);
    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);

    return prepareFileUpload(identifier, uploadId, key);
  }

  public String prepareAndUpload(String identifier, String uploadId, String key) {

    var url = prepareFileUpload(identifier, uploadId, key);

    return uploadToPresignedUrl(url).headers().getValue("ETag");
  }

  public final Response createFileUpload(String identifier) {

    return givenAuthenticatedJsonRequest(creatorAccessToken)
        .body(CREATE_PAYLOAD)
        .when()
        .post(fileUploadCreatePath(identifier))
        .then()
        .statusCode(200)
        .extract()
        .response();
  }
}
