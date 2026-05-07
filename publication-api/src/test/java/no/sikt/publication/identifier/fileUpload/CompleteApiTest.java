package no.sikt.publication.identifier.fileUpload;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import static no.sikt.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.Requests.givenUnauthenticatedJsonRequest;
import no.sikt.UserFixtures;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class CompleteApiTest extends FileUploadTestBase {

    private static final String PARTS = "parts";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String UPLOAD_ID = "uploadId";
    private static final String KEY = "key";
    private static final String TYPE = "type";
    private static final String IDENTIFIER = "identifier";
    private static final String EXAMPLE_FILE = "example.txt";

    private Map<String, Object> completePayload(String uploadId, String key, String eTag) {
        var parts = Map.of("etag", eTag, "partNumber", "1");
        return Map.of(
                UPLOAD_ID, uploadId, KEY, key, TYPE, "InternalCompleteUpload", PARTS, List.of(parts));
    }

    @Test
    @DisplayName("file-upload/complete returns file metadata")
    @Description("Calling file-upload/complete should return file metadata and statuscode 200")
    void shouldReturnFileMetaDataWhenCompleteUpload() {
        var identifier = PUBLICATION_FACTORY
                .createDraftPublication(UserFixtures.UIB_CREATOR)
                .jsonPath()
                .getString(IDENTIFIER);
        var createResponse = createFileUpload(identifier);

        var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
        var key = createResponse.jsonPath().getString(KEY);
        var eTag = prepareAndUpload(identifier, uploadId, key);

        var today = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        givenAuthenticatedJsonRequest(getCreatorAccessToken())
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
    @Description("Calling file-upload/complete with no authorization should return statuscode 401"
            + " Unauthorized")
    void shouldReturnUnauthorizedWhenCompleteWithoutAuthorization() {
        var identifier = PUBLICATION_FACTORY
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
    @Disabled // TODO: Fix bug NP-51214
    @DisplayName("file-upload/complete with missing ETag")
    @Description("Calling file-upload/complete with missing ETag should return 400 Bad Request")
    void shouldReturnUnauthorizedWhenCompleteWithMissingETag() {
        var identifier = PUBLICATION_FACTORY
                .createDraftPublication(UserFixtures.UIB_CREATOR)
                .jsonPath()
                .getString(IDENTIFIER);
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
