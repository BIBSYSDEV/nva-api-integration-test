package no.sikt;

import static no.sikt.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.Requests.givenUnauthenticatedJsonRequest;
import static org.hamcrest.Matchers.notNullValue;

import io.qameta.allure.Description;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class FileCreateApiTest extends IntegrationTestBase {

  private static String creatorAccessToken;

  private static final Map<String, Object> CREATE_PAYLOAD = new HashMap<>();
  private static final PublicationFactory PUBLICATION_FACTORY = new PublicationFactory();
  private static final String PUBLICATION_PATH = "/publication/";

  private static final String IDENTIFIER = "identifier";
  private static final String UPLOAD_ID = "uploadId";
  private static final String KEY = "key";
  private static final String EXAMPLE_FILE = "example.txt";
  private static final String FILE_NAME = "filename";
  private static final String TEXT_PLAIN = "text/plain";
  private static final String MIMETYPE = "mimetype";

  private static Map<String, Object> createFilePayload() {
    try (var resourceStream = PublicationFactory.class.getResourceAsStream("/" + EXAMPLE_FILE)) {
      var fileSize = resourceStream.readAllBytes().length;
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
}
