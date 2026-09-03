package no.sikt.nva.apitest.publication.batch;

import no.sikt.nva.apitest.base.LambdaFunctions;
import no.sikt.nva.apitest.base.LambdaInvocation;

/**
 * Invokes the ManuallyUpdatePublicationsHandler lambda in nva-publication-api, which has no API of
 * its own and is run by hand through the AWS CLI or SDK.
 */
public final class ManuallyUpdatePublications {

  public static final String CONTRIBUTOR_AFFILIATION = "CONTRIBUTOR_AFFILIATION";
  private static final String FUNCTION_LOGICAL_ID = "ManuallyUpdatePublicationsHandler";
  private static final String REJECTED_REQUEST_MESSAGE =
      "The handler rejected the request: %s (%s)";

  private ManuallyUpdatePublications() {}

  /** Runs the update and returns its report, failing if the handler rejected the request. */
  public static ManualUpdateReport run(ManualUpdateRequest request) {
    var invocation = invoke(request);
    if (invocation.failed()) {
      throw new IllegalStateException(
          REJECTED_REQUEST_MESSAGE.formatted(invocation.errorMessage(), invocation.errorType()));
    }
    return ManualUpdateReport.fromJson(invocation.payload());
  }

  /**
   * Runs the update and returns the raw invocation, for tests that assert on requests the handler
   * is expected to reject.
   */
  public static LambdaInvocation invoke(ManualUpdateRequest request) {
    return LambdaFunctions.invoke(FUNCTION_LOGICAL_ID, request.toJson());
  }
}
