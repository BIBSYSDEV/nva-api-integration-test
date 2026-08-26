package no.sikt.nva.apitest.base;

import static no.sikt.nva.apitest.base.ApplicationConstants.getRegion;

import nva.commons.core.Environment;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

/**
 * The AWS account the test runner credentials belong to. Which account that is decides everything
 * the tests touch: the api domain and the test user password are read from it, and the buckets,
 * tables and lambdas they reach are the ones in it.
 */
public final class AwsAccount {

  private AwsAccount() {}

  /** Resolved once per JVM, since the credentials cannot change while the tests run. */
  public static String accountId() {
    return AccountId.VALUE;
  }

  private static final class AccountId {

    private static final String VALUE = fetchAccountId();

    private static String fetchAccountId() {
      try (var stsClient = StsClient.builder().region(getRegion(new Environment())).build()) {
        var callerIdentity =
            stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build());
        return callerIdentity.account();
      }
    }
  }
}
