package no.sikt.nva.apitest.publication.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The file metadata returned by file-upload/complete. A freshly uploaded file carries no license
 * and is not awaiting anyone's approval until the uploader gives it the metadata that makes it
 * publishable.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UploadedFile(String identifier, String name, String mimeType, long size) {

  public PendingOpenFile awaitingApprovalUnder(String license) {
    return PendingOpenFile.from(this, license);
  }
}
