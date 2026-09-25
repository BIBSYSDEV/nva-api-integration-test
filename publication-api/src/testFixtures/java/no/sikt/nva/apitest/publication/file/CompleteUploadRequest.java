package no.sikt.nva.apitest.publication.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * The body of a file-upload/complete request, which finishes an upload once its parts are in S3 and
 * attaches the file to the publication.
 */
public record CompleteUploadRequest(String type, String uploadId, String key, List<Part> parts) {

  private static final String INTERNAL_COMPLETE_UPLOAD = "InternalCompleteUpload";
  private static final String FIRST_PART = "1";

  /** Completes an upload whose only part was stored in S3 with the given ETag. */
  public static CompleteUploadRequest forSinglePart(MultipartUpload upload, String eTag) {
    return new CompleteUploadRequest(
        INTERNAL_COMPLETE_UPLOAD,
        upload.uploadId(),
        upload.key(),
        List.of(new Part(eTag, FIRST_PART)));
  }

  public record Part(@JsonProperty("etag") String eTag, String partNumber) {}
}
