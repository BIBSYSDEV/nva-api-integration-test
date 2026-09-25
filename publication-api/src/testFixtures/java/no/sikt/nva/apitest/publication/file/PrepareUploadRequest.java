package no.sikt.nva.apitest.publication.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The body of a file-upload/prepare request, which asks for a presigned url for one part of an
 * upload. Fields that are null are left out of the request, so tests can send one that lacks a
 * field.
 */
@JsonInclude(Include.NON_NULL)
public record PrepareUploadRequest(String number, String uploadId, String key, String body) {

  private static final String FIRST_PART = "1";

  /** Prepares the only part of an upload of the example file. */
  public static PrepareUploadRequest forSinglePart(MultipartUpload upload) {
    return new PrepareUploadRequest(
        FIRST_PART, upload.uploadId(), upload.key(), ExampleFile.content());
  }
}
