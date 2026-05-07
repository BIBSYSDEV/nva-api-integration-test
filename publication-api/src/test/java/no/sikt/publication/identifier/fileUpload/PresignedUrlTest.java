package no.sikt.publication.identifier.fileUpload;

import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import static no.sikt.Requests.givenUnauthenticatedJsonRequest;
import no.sikt.UserFixtures;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class PresignedUrlTest extends FileUploadTestBase {

  private static final String IDENTIFIER = "identifier";

  @Test
  @DisplayName("Presigned url")
  @Description("Calling presigned url should return ETag and statuscode 200")
  void shouldReturnEtagInHeaderWhenPostingToPresignedUrl() {
    var identifier = PUBLICATION_FACTORY
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
