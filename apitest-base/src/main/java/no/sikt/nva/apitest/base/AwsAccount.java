package no.sikt.nva.apitest.base;

import static no.sikt.nva.apitest.base.ApplicationConstants.getRegion;

import nva.commons.core.Environment;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

/**
 * The AWS account the test runner credentials belong to. Which account that is decides everything
 * the tests touch: the api domain and the test user password are read from it, and the buckets,
 * tables and lambdas they reach are the ones in it.
 */
public final class AwsAccount {

  private static final String UNRESOLVED_ACCOUNT_MESSAGE =
      """
      Could not resolve the AWS account from sts:GetCallerIdentity. Check that the session is \
      still valid (aws sso login --sso-session sikt) and that the role is allowed to call \
      sts:GetCallerIdentity.\
      """;

  private AwsAccount() {}

  /** Resolved once per JVM, since the credentials cannot change while the tests run. */
  public static String accountId() {
    return AccountId.VALUE;
  }

  private static final class AccountId {

    private static final String VALUE = fetchAccountId();

    /**
     * Failures here surface as an ExceptionInInitializerError from whichever test class was loaded
     * first, which says nothing about the cause, so the two likely ones are named explicitly:
     * expired credentials and a role without the permission.
     */
    private static String fetchAccountId() {
      try (var stsClient = StsClient.builder().region(getRegion(new Environment())).build()) {
        var callerIdentity =
            stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build());
        return callerIdentity.account();
      } catch (SdkException exception) {
        throw new IllegalStateException(UNRESOLVED_ACCOUNT_MESSAGE, exception);
      }
    }
  }
}
