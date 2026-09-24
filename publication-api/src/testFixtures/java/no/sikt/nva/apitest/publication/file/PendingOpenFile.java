package no.sikt.nva.apitest.publication.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An open file awaiting approval, as the update file endpoint accepts it. The type discriminator is
 * a component rather than a constant field so that it is serialized along with the rest of the
 * file.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PendingOpenFile(
    String type, String identifier, String name, String mimeType, long size, String license) {

  private static final String PENDING_OPEN_FILE = "PendingOpenFile";

  public static PendingOpenFile from(UploadedFile uploadedFile, String license) {
    return new PendingOpenFile(
        PENDING_OPEN_FILE,
        uploadedFile.identifier(),
        uploadedFile.name(),
        uploadedFile.mimeType(),
        uploadedFile.size(),
        license);
  }
}
