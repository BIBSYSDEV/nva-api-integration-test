package no.sikt.nva.apitest.base;

import java.util.Set;
import java.util.function.Supplier;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

/** Turns an IAM denial into a message that names the permission the test runner role lacks. */
final class AwsCalls {

  private static final Set<String> ACCESS_DENIED_ERROR_CODES =
      Set.of("AccessDenied", "AccessDeniedException");

  private AwsCalls() {}

  static <T> T withPermissionHint(String permissionHint, Supplier<T> awsCall) {
    try {
      return awsCall.get();
    } catch (AwsServiceException exception) {
      if (isAccessDenied(exception)) {
        throw new IllegalStateException(
            "%s AWS response: %s".formatted(permissionHint, exception.getMessage()), exception);
      }
      throw exception;
    }
  }

  private static boolean isAccessDenied(AwsServiceException exception) {
    return ACCESS_DENIED_ERROR_CODES.contains(exception.awsErrorDetails().errorCode());
  }
}
