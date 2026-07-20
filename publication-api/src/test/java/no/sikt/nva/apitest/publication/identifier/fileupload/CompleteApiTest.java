package no.sikt.nva.apitest.publication.identifier.fileupload;

import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadCompletePath;

import io.qameta.allure.Description;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class CompleteApiTest extends FileUploadTestBase {

  private static final String EXAMPLE_FILE = "example.txt";

  private Map<String, Object> completePayload(String uploadId, String key, String eTag) {
    var parts = Map.of("etag", eTag, "partNumber", "1");
    return Map.of(
        UPLOAD_ID, uploadId, KEY, key, TYPE, "InternalCompleteUpload", PARTS, List.of(parts));
  }

  /** Calling file-upload/complete should return file metadata and status {@code 200 OK}. */
  @Test
  @DisplayName("file-upload/complete returns file metadata")
  @Description(useJavaDoc = true)
  void shouldReturnFileMetaDataWhenCompleteUpload(SoftAssertions softly) {
    var identifier = setupDraftPublication();
    var createResponse = createFileUpload(identifier);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);
    var eTag = prepareAndUpload(identifier, uploadId, key);

    var today =
        LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    var response =
        givenAuthenticatedJsonRequest(getCreatorAccessToken())
            .body(completePayload(uploadId, key, eTag))
            .when()
            .post(fileUploadCompletePath(identifier))
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString(TYPE)).isEqualTo("UploadedFile");
    softly.assertThat(response.getString(IDENTIFIER_FIELD)).isNotNull();
    softly.assertThat(response.getString("name")).isEqualTo(EXAMPLE_FILE);
    softly.assertThat(response.getString("mimeType")).isEqualTo(TEXT_PLAIN);
    softly
        .assertThat(response.getString("rightsRetentionStrategy.type"))
        .isEqualTo("NullRightsRetentionStrategy");
    softly
        .assertThat(response.getString("rightsRetentionStrategy.configuredType"))
        .isEqualTo("NullRightsRetentionStrategy");
    softly.assertThat(response.getString("uploadDetails.type")).isEqualTo("UserUploadDetails");
    softly
        .assertThat(response.getString("uploadDetails.uploadedBy"))
        .isEqualTo(UIB_CREATOR.cristinId());
    softly.assertThat(response.getString("uploadDetails.uploadedDate")).startsWith(today);
  }

  /**
   * Calling file-upload/complete with no authorization should return status {@code 401
   * Unauthorized}.
   */
  @Test
  @DisplayName("file-upload/complete with no authorization")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenCompleteWithoutAuthorization() {
    var identifier = setupDraftPublication();
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

  /**
   * Calling file-upload/complete with missing ETag should return status {@code 400 Bad Request}.
   */
  @Test
  @Disabled // TODO: Fix bug NP-51214
  @DisplayName("file-upload/complete with missing ETag")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenCompleteWithMissingETag() {
    var identifier = setupDraftPublication();
    var createResponse = createFileUpload(identifier);

    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);
    prepareAndUpload(identifier, uploadId, key);

    givenAuthenticatedJsonRequest(getCreatorAccessToken())
        .body(completePayload(uploadId, key, ""))
        .when()
        .post(fileUploadCompletePath(identifier))
        .then()
        .statusCode(400);
  }
}
