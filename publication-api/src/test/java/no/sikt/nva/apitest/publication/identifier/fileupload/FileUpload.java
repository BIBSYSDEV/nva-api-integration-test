package no.sikt.nva.apitest.publication.identifier.fileupload;

/**
 * An upload started with file-upload/create: the publication it belongs to and the uploadId and key
 * the create call returned, which prepare and complete refer back to.
 */
public record FileUpload(String publicationIdentifier, String uploadId, String key) {}
