package no.sikt.nva.apitest.publication.textextraction;

import static io.restassured.RestAssured.given;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.qameta.allure.Description;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import no.sikt.nva.apitest.base.S3Storage;
import no.sikt.nva.apitest.publication.identifier.fileupload.FileUploadTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Exercises the deployed text-extraction pipeline in nva-publication-api end to end: S3 event
 * trigger, seed lambda, extraction queue and worker lambda. The pipeline has no REST API. Its input
 * is a CSV of publication file keys uploaded to the seed bucket and its output is text objects in
 * the text storage bucket, so this test interacts with those buckets directly.
 *
 * <p>Per-format extraction logic is unit tested in nva-publication-api; the file formats here are
 * limited to plain text for the pipeline wiring and PDF for the binary-parser path, which depends
 * on the lambda runtime environment.
 *
 * <p>Prerequisites: the text-extraction stack is deployed to the environment, and the test runner
 * role has s3:PutObject on the seed bucket and s3:GetObject on the text storage bucket.
 */
class TextExtractionTest extends FileUploadTestBase {

  /**
   * The bucket names mirror the defaults of the TextExtractionSeedBucketName and
   * TextStorageBucketName parameters in the nva-publication-api template, which suffix the account
   * id.
   */
  private static final String SEED_BUCKET_PREFIX = "nva-text-extraction-seed-";

  private static final String TEXT_STORAGE_BUCKET_PREFIX = "nva-publication-text-";
  private static final String CSV_SUFFIX = ".csv";
  private static final String EXTRACTED_TEXT_SUFFIX = ".txt";
  private static final String ETAG_HEADER = "ETag";
  private static final Duration EXTRACTION_TIMEOUT = Duration.ofMinutes(5);

  private static final String SAMPLE_PDF_FILE = "sample.pdf";
  private static final String SAMPLE_PDF_SENTENCE =
      "Sample PDF for the NVA text extraction integration test.";

  @ParameterizedTest(name = "{0}")
  @MethodSource("seedableFiles")
  @DisplayName("Seeding a file key by CSV stores the extracted file text")
  @Description(
      "Uploading a CSV with the key of a publication file to the seed bucket should extract the"
          + " text of that file to the text storage bucket")
  void shouldStoreExtractedTextWhenFileKeyIsSeededByCsv(
      byte[] fileContent, String expectedSentence) {
    var publicationIdentifier = setupDraftPublication();
    var fileKey = uploadFile(publicationIdentifier, fileContent);

    S3Storage.putTextObject(seedBucketName(), fileKey + CSV_SUFFIX, fileKey);

    var extractedText =
        pollUntil(EXTRACTION_TIMEOUT, extractedTextRequest(fileKey), Optional::isPresent)
            .orElseThrow();

    assertThat(extractedText).contains(expectedSentence);
  }

  /**
   * The plain text content is generated per run, proving that the pipeline processed this run's
   * file. The PDF is a fixed resource since binary content cannot be generated inline; its key is
   * unique per run regardless.
   */
  private static Stream<Arguments> seedableFiles() {
    var uniqueSentence = "Distinctive text extraction sentence " + UUID.randomUUID();
    return Stream.of(
        arguments(
            named("plain text", uniqueSentence.getBytes(StandardCharsets.UTF_8)), uniqueSentence),
        arguments(named("PDF", readResource(SAMPLE_PDF_FILE)), SAMPLE_PDF_SENTENCE));
  }

  private static byte[] readResource(String filename) {
    try (var resourceStream = TextExtractionTest.class.getResourceAsStream("/" + filename)) {
      return resourceStream.readAllBytes();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private Callable<Optional<String>> extractedTextRequest(String fileKey) {
    return () -> S3Storage.findTextObject(textStorageBucketName(), fileKey + EXTRACTED_TEXT_SUFFIX);
  }

  private String uploadFile(String publicationIdentifier, byte[] content) {
    var createResponse = createFileUpload(publicationIdentifier);
    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var fileKey = createResponse.jsonPath().getString(KEY);
    var presignedUrl = prepareFileUpload(publicationIdentifier, uploadId, fileKey);
    var eTag = uploadBytesToPresignedUrl(presignedUrl, content);
    completeUpload(publicationIdentifier, uploadId, fileKey, eTag);
    return fileKey;
  }

  /**
   * Uploads the content bytes verbatim, unlike the inherited upload helper which wraps the content
   * in a JSON envelope, so that the extractor sees the actual file bytes.
   */
  private String uploadBytesToPresignedUrl(String presignedUrl, byte[] content) {
    return given()
        .contentType(ContentType.BINARY)
        .body(content)
        .put(presignedUrl)
        .then()
        .statusCode(200)
        .extract()
        .header(ETAG_HEADER);
  }

  private static String seedBucketName() {
    return SEED_BUCKET_PREFIX + S3Storage.accountId();
  }

  private static String textStorageBucketName() {
    return TEXT_STORAGE_BUCKET_PREFIX + S3Storage.accountId();
  }
}
