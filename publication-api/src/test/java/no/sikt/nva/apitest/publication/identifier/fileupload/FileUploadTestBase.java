package no.sikt.nva.apitest.publication.identifier.fileupload;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadCompletePath;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadCreatePath;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadPreparePath;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.sikt.nva.PublicationFactory;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.junit.jupiter.api.BeforeAll;

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

  protected static String accessTokenFor(User user) {
    return CognitoLogin.loginUser(user).get("accessToken");
  }

  public static String getFileAsString() {
    return fileAsString;
  }

  @BeforeAll
  public static void initFileTest() {
    CREATE_PAYLOAD.putAll(createFilePayload());
    creatorAccessToken = CognitoLogin.login(UIB_CREATOR.userId()).get("accessToken");
  }

  protected static Map<String, Object> completePayload(FileUpload upload, String eTag) {
    var part = Map.of("etag", eTag, "partNumber", "1");
    return Map.of(
        UPLOAD_ID,
        upload.uploadId(),
        KEY,
        upload.key(),
        TYPE,
        "InternalCompleteUpload",
        PARTS,
        List.of(part));
  }

  public Response completeUpload(FileUpload upload, String eTag) {
    return completeUpload(upload, eTag, creatorAccessToken);
  }

  public Response completeUpload(FileUpload upload, String eTag, User uploader) {
    return completeUpload(upload, eTag, accessTokenFor(uploader));
  }

  private Response completeUpload(FileUpload upload, String eTag, String accessToken) {
    return givenAuthenticatedJsonRequest(accessToken)
        .body(completePayload(upload, eTag))
        .when()
        .post(fileUploadCompletePath(upload.publicationIdentifier()))
        .then()
        .statusCode(HTTP_OK)
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
        .statusCode(HTTP_OK)
        .extract()
        .response();
  }

  public String prepareFileUpload(FileUpload upload) {
    return prepareFileUpload(upload, creatorAccessToken);
  }

  private String prepareFileUpload(FileUpload upload, String accessToken) {
    var preparePayload =
        Map.of(NUMBER, "1", UPLOAD_ID, upload.uploadId(), KEY, upload.key(), BODY, fileAsString);
    var url =
        givenAuthenticatedJsonRequest(accessToken)
            .body(preparePayload)
            .when()
            .post(fileUploadPreparePath(upload.publicationIdentifier()))
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .response()
            .jsonPath()
            .getString(URL);

    return URLDecoder.decode(url, StandardCharsets.UTF_8);
  }

  public String createAndPrepareFileUpload(String identifier) {
    return prepareFileUpload(createFileUpload(identifier));
  }

  public String prepareAndUpload(FileUpload upload) {
    return prepareAndUpload(upload, creatorAccessToken);
  }

  public String prepareAndUpload(FileUpload upload, User uploader) {
    return prepareAndUpload(upload, accessTokenFor(uploader));
  }

  private String prepareAndUpload(FileUpload upload, String accessToken) {
    var url = prepareFileUpload(upload, accessToken);
    return uploadToPresignedUrl(url).headers().getValue("ETag");
  }

  public final FileUpload createFileUpload(String identifier) {
    return createFileUpload(identifier, creatorAccessToken);
  }

  public final FileUpload createFileUpload(String identifier, User uploader) {
    return createFileUpload(identifier, accessTokenFor(uploader));
  }

  private FileUpload createFileUpload(String identifier, String accessToken) {
    var response =
        givenAuthenticatedJsonRequest(accessToken)
            .body(CREATE_PAYLOAD)
            .when()
            .post(fileUploadCreatePath(identifier))
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();
    return new FileUpload(identifier, response.getString(UPLOAD_ID), response.getString(KEY));
  }
}
