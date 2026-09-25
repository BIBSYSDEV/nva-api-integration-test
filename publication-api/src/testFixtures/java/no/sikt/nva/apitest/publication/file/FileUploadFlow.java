package no.sikt.nva.apitest.publication.file;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadCompletePath;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadCreatePath;
import static no.sikt.nva.apitest.publication.PublicationPaths.fileUploadPreparePath;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import no.sikt.nva.apitest.base.User;

/**
 * Uploads the example file to a publication through the multipart file upload flow: create an
 * upload, prepare a presigned url for its single part, upload the part to S3 and complete the
 * upload. Every step takes the user it runs as, so tests can upload files on behalf of any
 * institution.
 */
public final class FileUploadFlow {

  private static final String ETAG_HEADER = "ETag";

  private FileUploadFlow() {}

  /** Runs the whole upload flow and returns the file it attached to the publication. */
  public static UploadedFile uploadExampleFile(User uploader, String publicationIdentifier) {
    var upload = createFileUpload(uploader, publicationIdentifier);
    var presignedUrl = prepareFileUpload(uploader, publicationIdentifier, upload);
    var eTag = uploadToPresignedUrl(presignedUrl);

    return completeUpload(uploader, publicationIdentifier, upload, eTag).as(UploadedFile.class);
  }

  public static MultipartUpload createFileUpload(User user, String publicationIdentifier) {
    return givenAuthenticatedJsonRequestAsUser(user)
        .body(CreateUploadRequest.forExampleFile())
        .when()
        .post(fileUploadCreatePath(publicationIdentifier))
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .as(MultipartUpload.class);
  }

  /** Returns the presigned url to upload the single part of the upload to. */
  public static String prepareFileUpload(
      User user, String publicationIdentifier, MultipartUpload upload) {
    var url =
        givenAuthenticatedJsonRequestAsUser(user)
            .body(PrepareUploadRequest.forSinglePart(upload))
            .when()
            .post(fileUploadPreparePath(publicationIdentifier))
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath()
            .getString("url");

    return URLDecoder.decode(url, StandardCharsets.UTF_8);
  }

  /** Uploads the example file to the presigned url and returns the ETag S3 gave the part. */
  public static String uploadToPresignedUrl(String presignedUrl) {
    return given()
        .accept(ContentType.TEXT)
        .contentType(ContentType.JSON)
        .body(Map.of("data", ExampleFile.content()))
        .when()
        .put(presignedUrl)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .header(ETAG_HEADER);
  }

  public static Response completeUpload(
      User user, String publicationIdentifier, MultipartUpload upload, String eTag) {
    return givenAuthenticatedJsonRequestAsUser(user)
        .body(CompleteUploadRequest.forSinglePart(upload, eTag))
        .when()
        .post(fileUploadCompletePath(publicationIdentifier))
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .response();
  }
}
