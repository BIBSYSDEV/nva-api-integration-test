package no.sikt.nva.apitest.publication.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An upload started by file-upload/create. The upload id and S3 key identify it in the prepare and
 * complete requests that follow.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MultipartUpload(String uploadId, String key) {}
