package no.sikt.nva.apitest.publication.identifier.fileupload;

import static io.restassured.http.Method.POST;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_DATE;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;
import static no.sikt.nva.apitest.publication.PublicationFields.TYPE;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadCompletePath;
import static no.sikt.nva.apitest.publication.file.FileUploadFlow.createFileUpload;
import static no.sikt.nva.apitest.publication.file.FileUploadFlow.prepareFileUpload;
import static no.sikt.nva.apitest.publication.file.FileUploadFlow.uploadToPresignedUrl;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import no.sikt.nva.apitest.publication.file.CompleteUploadRequest;
import no.sikt.nva.apitest.publication.file.ExampleFile;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST /publication/{identifier}/file-upload/complete")
class CompleteApiTest extends PublicationTestBase {

  /** Calling file-upload/complete should return file metadata and status {@code 200 OK}. */
  @Test
  @DisplayName("file-upload/complete returns file metadata")
  @Description(useJavaDoc = true)
  void shouldReturnFileMetaDataWhenCompleteUpload(SoftAssertions softly) {
    var identifier = setupDraftPublication();
    var upload = createFileUpload(UIB_CREATOR, identifier);
    var eTag = uploadToPresignedUrl(prepareFileUpload(UIB_CREATOR, identifier, upload));

    var response =
        givenAuthenticatedJsonRequestAsUser(UIB_CREATOR)
            .body(CompleteUploadRequest.forSinglePart(upload, eTag))
            .when()
            .post(fileUploadCompletePath(identifier))
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString(TYPE)).isEqualTo("UploadedFile");
    softly.assertThat(response.getString(IDENTIFIER_FIELD)).isNotNull();
    softly.assertThat(response.getString("name")).isEqualTo(ExampleFile.NAME);
    softly.assertThat(response.getString("mimeType")).isEqualTo(ExampleFile.MIME_TYPE);
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
    softly.assertThat(response.getString("uploadDetails.uploadedDate")).startsWith(CURRENT_DATE);
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
    createFileUpload(UIB_CREATOR, identifier);

    requestShouldReturnUnauthorized(POST, fileUploadCompletePath(identifier));
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
    var upload = createFileUpload(UIB_CREATOR, identifier);
    uploadToPresignedUrl(prepareFileUpload(UIB_CREATOR, identifier, upload));

    givenAuthenticatedJsonRequestAsUser(UIB_CREATOR)
        .body(CompleteUploadRequest.forSinglePart(upload, ""))
        .when()
        .post(fileUploadCompletePath(identifier))
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }
}
