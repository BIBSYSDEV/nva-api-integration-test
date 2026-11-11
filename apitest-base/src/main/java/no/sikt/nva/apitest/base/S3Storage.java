package no.sikt.nva.apitest.base;

import static no.sikt.nva.apitest.base.ApplicationConstants.getRegion;
import static no.sikt.nva.apitest.base.AwsCalls.withPermissionHint;

import java.util.List;
import java.util.Optional;
import nva.commons.core.Environment;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Direct S3 access for testing pipelines that have no REST API, such as the text-extraction lambdas
 * in nva-publication-api that are driven by S3 uploads and observed through an output bucket.
 * Requires the test runner credentials to have access to the buckets involved.
 */
public final class S3Storage {

  private static final String MISSING_PUT_PERMISSION_MESSAGE =
      "The test runner role is not allowed to upload to bucket %s. It needs s3:PutObject on"
          + " arn:aws:s3:::%<s/*.";
  private static final String MISSING_LIST_PERMISSION_MESSAGE =
      "The test runner role is not allowed to list bucket %s. It needs s3:ListBucket on"
          + " arn:aws:s3:::%<s.";

  // Shared across parallel test classes for the lifetime of the JVM, so never closed.
  private static final S3Client S3_CLIENT =
      S3Client.builder().region(getRegion(new Environment())).build();

  private S3Storage() {}

  /**
   * The account id of the test runner credentials, for constructing account-suffixed bucket names.
   */
  public static String accountId() {
    return AwsAccount.accountId();
  }

  public static void putTextObject(String bucketName, String key, String content) {
    putObject(bucketName, key, RequestBody.fromString(content));
  }

  /**
   * Stores the object under the given content type, for pipelines that read it back over HTTP and
   * act on what the response declares. Without it S3 serves the object as binary data.
   */
  public static void putTextObject(
      String bucketName, String key, String content, String contentType) {
    withPermissionHint(
        MISSING_PUT_PERMISSION_MESSAGE.formatted(bucketName),
        () ->
            S3_CLIENT.putObject(
                request -> request.bucket(bucketName).key(key).contentType(contentType),
                RequestBody.fromString(content)));
  }

  public static void putBytesObject(String bucketName, String key, byte[] content) {
    putObject(bucketName, key, RequestBody.fromBytes(content));
  }

  private static void putObject(String bucketName, String key, RequestBody content) {
    withPermissionHint(
        MISSING_PUT_PERMISSION_MESSAGE.formatted(bucketName),
        () -> S3_CLIENT.putObject(request -> request.bucket(bucketName).key(key), content));
  }

  public static List<String> listKeys(String bucketName, String prefix) {
    return withPermissionHint(
        MISSING_LIST_PERMISSION_MESSAGE.formatted(bucketName),
        () ->
            S3_CLIENT
                .listObjectsV2Paginator(request -> request.bucket(bucketName).prefix(prefix))
                .contents()
                .stream()
                .map(S3Object::key)
                .toList());
  }

  /** Reads the object as UTF-8 text, or empty if the key does not exist. */
  public static Optional<String> findTextObject(String bucketName, String key) {
    Optional<String> textObject;
    try {
      textObject =
          Optional.of(
              S3_CLIENT
                  .getObjectAsBytes(request -> request.bucket(bucketName).key(key))
                  .asUtf8String());
    } catch (NoSuchKeyException noSuchKeyException) {
      textObject = Optional.empty();
    }
    return textObject;
  }
}
