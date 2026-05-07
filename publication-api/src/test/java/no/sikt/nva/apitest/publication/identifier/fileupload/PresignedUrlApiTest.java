package no.sikt.nva.apitest.publication.identifier.fileupload;

import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static org.hamcrest.Matchers.notNullValue;

import io.qameta.allure.Description;
import java.util.Map;
import no.sikt.nva.apitest.base.UserFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class PresignedUrlApiTest extends FileUploadTestBase {

  private static final String IDENTIFIER = "identifier";

  @Test
  @DisplayName("Presigned url")
  @Description("Calling presigned url should return ETag and statuscode 200")
  void shouldReturnEtagInHeaderWhenPostingToPresignedUrl() {
    var identifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);
    var uploadUrl = createAndPrepareFileUpload(identifier);
    var presignedPayload = Map.of("data", getFileAsString());

    givenUnauthenticatedJsonRequest()
        .body(presignedPayload)
        .when()
        .put(uploadUrl)
        .then()
        .statusCode(200)
        .header("ETag", notNullValue());
  }
}
