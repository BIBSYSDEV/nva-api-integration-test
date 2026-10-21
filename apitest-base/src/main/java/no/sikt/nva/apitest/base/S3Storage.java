package no.sikt.nva.apitest.base;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.util.Objects.nonNull;

import java.util.Optional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Direct S3 access for testing pipelines that have no REST API, such as the text-extraction lambdas
 * in nva-publication-api that are driven by S3 uploads and observed through an output bucket.
 * Requires the test runner credentials to have access to the buckets involved.
 */
public final class S3Storage {

  private static final String MISSING_PUT_PERMISSION_MESSAGE =
      "The test runner role is not allowed to upload to bucket %s. It needs s3:PutObject on"
          + " arn:aws:s3:::%<s/*.";

  private static final String REGION =
      nonNull(System.getenv("AWS_REGION")) ? System.getenv("AWS_REGION") : "eu-west-1";

  // Shared across parallel test classes for the lifetime of the JVM, so never closed.
  private static final S3Client S3_CLIENT = S3Client.builder().region(Region.of(REGION)).build();

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

  public static void putBytesObject(String bucketName, String key, byte[] content) {
    putObject(bucketName, key, RequestBody.fromBytes(content));
  }

  /**
   * A denied upload is rethrown naming the permission and bucket, because the SDK only reports
   * "Access Denied" and the bucket is otherwise easy to lose among the other buckets a test
   * touches.
   */
  private static void putObject(String bucketName, String key, RequestBody content) {
    try {
      S3_CLIENT.putObject(request -> request.bucket(bucketName).key(key), content);
    } catch (S3Exception exception) {
      if (exception.statusCode() == HTTP_FORBIDDEN) {
        throw new IllegalStateException(
            MISSING_PUT_PERMISSION_MESSAGE.formatted(bucketName), exception);
      }
      throw exception;
    }
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
