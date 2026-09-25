package no.sikt.nva.apitest.publication.identifier.fileupload;

import static io.restassured.http.Method.POST;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_DATE;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UNIT_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadCompletePath;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import java.util.stream.Stream;
import no.sikt.nva.apitest.base.User;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST /publication/{identifier}/file-upload/complete")
class CompleteApiTest extends FileUploadTestBase {

  private static final String EXAMPLE_FILE = "example.txt";
  private static final String RRS_TYPE = "rightsRetentionStrategy.type";
  private static final String RRS_CONFIGURED_TYPE = "rightsRetentionStrategy.configuredType";
  private static final String UPLOADED_BY = "uploadDetails.uploadedBy";

  /** Calling file-upload/complete should return file metadata and status {@code 200 OK}. */
  @Test
  @DisplayName("file-upload/complete returns file metadata")
  @Description(useJavaDoc = true)
  void shouldReturnFileMetaDataWhenCompleteUpload(SoftAssertions softly) {
    var identifier = setupDraftPublication();
    var upload = createFileUpload(identifier);
    var eTag = prepareAndUpload(upload);

    var response =
        givenAuthenticatedJsonRequest(getCreatorAccessToken())
            .body(completePayload(upload, eTag))
            .when()
            .post(fileUploadCompletePath(identifier))
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString(TYPE)).isEqualTo("UploadedFile");
    softly.assertThat(response.getString(IDENTIFIER_FIELD)).isNotNull();
    softly.assertThat(response.getString("name")).isEqualTo(EXAMPLE_FILE);
    softly.assertThat(response.getString("mimeType")).isEqualTo(TEXT_PLAIN);
    softly.assertThat(response.getString("uploadDetails.type")).isEqualTo("UserUploadDetails");
    softly.assertThat(response.getString(UPLOADED_BY)).isEqualTo(UIB_CREATOR.cristinId());
    softly.assertThat(response.getString("uploadDetails.uploadedDate")).startsWith(CURRENT_DATE);
  }

  /**
   * A freshly uploaded file gets the customer's RRS type at upload time as {@code configuredType}.
   * UiB has RRS switched off, UNIT switched on (kept so by the Cypress e2e suite).
   */
  @ParameterizedTest
  @MethodSource("uploaders")
  @DisplayName("file-upload/complete stamps the file with the customer's rights retention strategy")
  @Description(useJavaDoc = true)
  void shouldStampUploadedFileWithCustomersCurrentRightsRetentionStrategy(
      User uploader, String customerRrsType, String fileRrsType, SoftAssertions softly) {
    var file = uploadFileAs(uploader);

    softly.assertThat(file.getString(RRS_CONFIGURED_TYPE)).isEqualTo(customerRrsType);
    softly.assertThat(file.getString(RRS_TYPE)).isEqualTo(fileRrsType);
    softly.assertThat(file.getString(UPLOADED_BY)).isEqualTo(uploader.cristinId());
  }

  private static Stream<Arguments> uploaders() {
    return Stream.of(
        argumentSet(
            "UiB, rights retention strategy switched off",
            UIB_CREATOR,
            "NullRightsRetentionStrategy",
            "NullRightsRetentionStrategy"),
        argumentSet(
            "UNIT, rights retention strategy switched on",
            UNIT_CREATOR,
            "RightsRetentionStrategy",
            "CustomerRightsRetentionStrategy"));
  }

  private JsonPath uploadFileAs(User uploader) {
    var identifier =
        PUBLICATION_FACTORY.createDraftPublication(uploader).jsonPath().getString(IDENTIFIER_FIELD);
    var upload = createFileUpload(identifier, uploader);
    var eTag = prepareAndUpload(upload, uploader);
    return completeUpload(upload, eTag, uploader).jsonPath();
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
    createFileUpload(identifier);

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
    var upload = createFileUpload(identifier);
    prepareAndUpload(upload);

    givenAuthenticatedJsonRequest(getCreatorAccessToken())
        .body(completePayload(upload, ""))
        .when()
        .post(fileUploadCompletePath(identifier))
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }
}
