package no.sikt.nva.apitest.publication.file;

import com.fasterxml.jackson.annotation.JsonProperty;

/** The body of a file-upload/create request, which starts a multipart upload of one file. */
public record CreateUploadRequest(
    String filename, String size, @JsonProperty("mimetype") String mimeType) {

  public static CreateUploadRequest forExampleFile() {
    return new CreateUploadRequest(
        ExampleFile.NAME, Integer.toString(ExampleFile.sizeInBytes()), ExampleFile.MIME_TYPE);
  }
}
