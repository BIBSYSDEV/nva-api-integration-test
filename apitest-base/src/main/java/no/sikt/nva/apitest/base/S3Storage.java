package no.sikt.nva.apitest.base;

import static java.util.Objects.nonNull;

import java.util.Optional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

/**
 * Direct S3 access for testing pipelines that have no REST API, such as the text-extraction lambdas
 * in nva-publication-api that are driven by S3 uploads and observed through an output bucket.
 * Requires the test runner credentials to have access to the buckets involved.
 */
public final class S3Storage {

  private static final String REGION =
      nonNull(System.getenv("AWS_REGION")) ? System.getenv("AWS_REGION") : "eu-west-1";

  // Shared across parallel test classes for the lifetime of the JVM, so never closed.
  private static final S3Client S3_CLIENT = S3Client.builder().region(Region.of(REGION)).build();

  private S3Storage() {}

  /**
   * The account id of the test runner credentials, for constructing account-suffixed bucket names.
   */
  public static String accountId() {
    return AccountId.VALUE;
  }

  public static void putTextObject(String bucketName, String key, String content) {
    S3_CLIENT.putObject(
        request -> request.bucket(bucketName).key(key), RequestBody.fromString(content));
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

  private static final class AccountId {

    private static final String VALUE = fetchAccountId();

    private static String fetchAccountId() {
      try (var stsClient = StsClient.builder().region(Region.of(REGION)).build()) {
        var callerIdentity =
            stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build());
        return callerIdentity.account();
      }
    }
  }
}
