package no.sikt.nva.apitest.base;

import static no.sikt.nva.apitest.base.ApplicationConstants.getRegion;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import nva.commons.core.Environment;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.ResourceTagMapping;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.TagFilter;

/**
 * Direct Lambda invocation for testing handlers that are operated by hand through the AWS CLI or
 * SDK instead of being exposed through an API, such as the batch update handlers in
 * nva-publication-api.
 *
 * <p>The functions are looked up by their CloudFormation logical id rather than by name, because
 * the templates leave FunctionName unset and CloudFormation then generates a name that includes the
 * stack name and a random suffix. CloudFormation tags every resource it creates with its logical
 * id, so the tagging API resolves the logical id to exactly one function ARN per environment.
 *
 * <p>Prerequisites: the test runner role has lambda:InvokeFunction on the function and
 * tag:GetResources in the account.
 */
public final class LambdaFunctions {

  private static final String LOGICAL_ID_TAG = "aws:cloudformation:logical-id";
  private static final String LAMBDA_RESOURCE_TYPE = "lambda:function";
  private static final String NO_SINGLE_FUNCTION_MESSAGE =
      "Expected exactly one lambda function tagged with logical id %s, but found %d: %s";
  private static final int ONE_FUNCTION_PER_LOGICAL_ID = 1;

  // The handlers invoked here run batch jobs with a 15 minute Lambda timeout. The default socket
  // timeout of 30s would abort the connection long before a batch of any size completes, and the
  // invocation would then be retried against a function that is still running the first attempt.
  private static final Duration INVOCATION_TIMEOUT = Duration.ofMinutes(15);

  // Shared across parallel test classes for the lifetime of the JVM, so never closed.
  private static final LambdaClient LAMBDA_CLIENT =
      LambdaClient.builder()
          .region(getRegion(new Environment()))
          .httpClientBuilder(ApacheHttpClient.builder().socketTimeout(INVOCATION_TIMEOUT))
          .overrideConfiguration(configuration -> configuration.apiCallTimeout(INVOCATION_TIMEOUT))
          .build();

  private static final Map<String, String> FUNCTION_ARNS = new ConcurrentHashMap<>();

  private LambdaFunctions() {}

  /**
   * Invokes the function synchronously and returns its response. An exception thrown by the handler
   * is reported in the returned invocation rather than raised here, so that tests can assert on
   * rejected requests.
   */
  public static LambdaInvocation invoke(String logicalId, String payload) {
    var response =
        LAMBDA_CLIENT.invoke(
            request ->
                request
                    .functionName(functionArn(logicalId))
                    .payload(SdkBytes.fromUtf8String(payload)));
    return new LambdaInvocation(response.payload().asUtf8String(), response.functionError());
  }

  private static String functionArn(String logicalId) {
    return FUNCTION_ARNS.computeIfAbsent(logicalId, LambdaFunctions::lookUpFunctionArn);
  }

  private static String lookUpFunctionArn(String logicalId) {
    try (var taggingClient =
        ResourceGroupsTaggingApiClient.builder().region(getRegion(new Environment())).build()) {
      var functionArns =
          taggingClient
              .getResourcesPaginator(
                  request ->
                      request
                          .resourceTypeFilters(LAMBDA_RESOURCE_TYPE)
                          .tagFilters(logicalIdFilter(logicalId)))
              .resourceTagMappingList()
              .stream()
              .map(ResourceTagMapping::resourceARN)
              .toList();
      return singleFunctionArn(functionArns, logicalId);
    }
  }

  private static TagFilter logicalIdFilter(String logicalId) {
    return TagFilter.builder().key(LOGICAL_ID_TAG).values(logicalId).build();
  }

  private static String singleFunctionArn(List<String> functionArns, String logicalId) {
    if (functionArns.size() != ONE_FUNCTION_PER_LOGICAL_ID) {
      throw new IllegalStateException(
          NO_SINGLE_FUNCTION_MESSAGE.formatted(logicalId, functionArns.size(), functionArns));
    }
    return functionArns.getFirst();
  }
}
