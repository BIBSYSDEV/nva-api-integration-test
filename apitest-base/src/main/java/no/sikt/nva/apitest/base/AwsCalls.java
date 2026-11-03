package no.sikt.nva.apitest.base;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;

import java.util.function.Supplier;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

/** Turns an IAM denial into a message that names the permission the test runner role lacks. */
final class AwsCalls {

  private AwsCalls() {}

  static <T> T withPermissionHint(String permissionHint, Supplier<T> awsCall) {
    try {
      return awsCall.get();
    } catch (AwsServiceException exception) {
      if (exception.statusCode() == HTTP_FORBIDDEN) {
        throw new IllegalStateException(permissionHint, exception);
      }
      throw exception;
    }
  }
}
