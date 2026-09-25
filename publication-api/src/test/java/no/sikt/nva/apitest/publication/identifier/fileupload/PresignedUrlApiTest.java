package no.sikt.nva.apitest.publication.identifier.fileupload;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.file.FileUploadFlow.createFileUpload;
import static no.sikt.nva.apitest.publication.file.FileUploadFlow.prepareFileUpload;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import java.util.Map;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import no.sikt.nva.apitest.publication.file.ExampleFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PUT {presignedUrl} (S3)")
class PresignedUrlApiTest extends PublicationTestBase {

  /** Calling presigned url should return ETag and status {@code 200 OK}. */
  @Test
  @DisplayName("Presigned url")
  @Description(useJavaDoc = true)
  void shouldReturnEtagInHeaderWhenPostingToPresignedUrl() {
    var identifier = setupDraftPublication();
    var uploadUrl =
        prepareFileUpload(UIB_CREATOR, identifier, createFileUpload(UIB_CREATOR, identifier));
    var presignedPayload = Map.of("data", ExampleFile.content());

    var etag =
        givenUnauthenticatedJsonRequest()
            .body(presignedPayload)
            .when()
            .put(uploadUrl)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .header("ETag");

    assertThat(etag).isNotNull();
  }
}
