package no.sikt.nva.apitest.publication.identifier.fileupload;

import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PresignedUrlApiTest extends FileUploadTestBase {

  @Test
  @DisplayName("Presigned url")
  @Description("Calling presigned url should return ETag and statuscode 200")
  void shouldReturnEtagInHeaderWhenPostingToPresignedUrl() {
    var identifier = setupDraftPublication();
    var uploadUrl = createAndPrepareFileUpload(identifier);
    var presignedPayload = Map.of("data", getFileAsString());

    var etag =
        givenUnauthenticatedJsonRequest()
            .body(presignedPayload)
            .when()
            .put(uploadUrl)
            .then()
            .statusCode(200)
            .extract()
            .header("ETag");

    assertThat(etag).isNotNull();
  }
}
