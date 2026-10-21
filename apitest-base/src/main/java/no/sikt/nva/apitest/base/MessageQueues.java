package no.sikt.nva.apitest.base;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.util.stream.Collectors.toMap;
import static no.sikt.nva.apitest.base.ApplicationConstants.getRegion;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import nva.commons.core.Environment;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SqsException;

/**
 * Direct SQS access for testing pipelines that are driven by queue messages instead of an API, such
 * as the Scopus import in nva-publication-api.
 *
 * <p>The queues are looked up by their CloudFormation logical id, because the templates leave
 * QueueName unset and CloudFormation then generates {@code <stack name>-<logical id>-<suffix>}.
 * Unlike the lambdas in {@link LambdaFunctions}, the queues carry no aws:cloudformation tags, so
 * the lookup lists the queues and matches the {@code -<logical id>-} segment of the name. The stack
 * name part is truncated by CloudFormation and differs between environments, so it is not matched.
 *
 * <p>Prerequisites: the test runner role has sqs:ListQueues in the account and sqs:SendMessage on
 * the queue.
 */
public final class MessageQueues {

  private static final String STRING_DATA_TYPE = "String";
  private static final String QUEUE_NAME_SEGMENT = "-%s-";
  private static final String NO_SINGLE_QUEUE_MESSAGE =
      "Expected exactly one queue whose name contains %s, but found %d: %s";
  private static final String MISSING_LIST_PERMISSION_MESSAGE =
      "The test runner role is not allowed to list queues. It needs sqs:ListQueues.";
  private static final String MISSING_SEND_PERMISSION_MESSAGE =
      "The test runner role is not allowed to send to queue %s. It needs sqs:SendMessage on it.";
  private static final String ACCESS_DENIED_ERROR_CODE = "AccessDenied";
  private static final int ONE_QUEUE_PER_LOGICAL_ID = 1;

  // Shared across parallel test classes for the lifetime of the JVM, so never closed.
  private static final SqsClient SQS_CLIENT =
      SqsClient.builder().region(getRegion(new Environment())).build();

  private static final Map<String, String> QUEUE_URLS = new ConcurrentHashMap<>();

  private MessageQueues() {}

  /** Sends a message with string attributes to the queue with the given logical id. */
  public static void sendMessage(
      String logicalId, String messageBody, Map<String, String> stringAttributes) {
    var queueUrl = queueUrl(logicalId);
    withPermissionMessage(
        MISSING_SEND_PERMISSION_MESSAGE.formatted(queueUrl),
        () ->
            SQS_CLIENT.sendMessage(
                request ->
                    request
                        .queueUrl(queueUrl)
                        .messageBody(messageBody)
                        .messageAttributes(toMessageAttributes(stringAttributes))));
  }

  private static Map<String, MessageAttributeValue> toMessageAttributes(
      Map<String, String> stringAttributes) {
    return stringAttributes.entrySet().stream()
        .collect(toMap(Map.Entry::getKey, entry -> stringAttribute(entry.getValue())));
  }

  private static MessageAttributeValue stringAttribute(String value) {
    return MessageAttributeValue.builder().dataType(STRING_DATA_TYPE).stringValue(value).build();
  }

  private static String queueUrl(String logicalId) {
    return QUEUE_URLS.computeIfAbsent(logicalId, MessageQueues::lookUpQueueUrl);
  }

  private static String lookUpQueueUrl(String logicalId) {
    var nameSegment = QUEUE_NAME_SEGMENT.formatted(logicalId);
    var queueUrls =
        withPermissionMessage(
            MISSING_LIST_PERMISSION_MESSAGE,
            () ->
                SQS_CLIENT.listQueuesPaginator(request -> {}).queueUrls().stream()
                    .filter(queueUrl -> queueName(queueUrl).contains(nameSegment))
                    .toList());
    return singleQueueUrl(queueUrls, nameSegment);
  }

  private static String queueName(String queueUrl) {
    return queueUrl.substring(queueUrl.lastIndexOf('/') + 1);
  }

  private static String singleQueueUrl(List<String> queueUrls, String nameSegment) {
    if (queueUrls.size() != ONE_QUEUE_PER_LOGICAL_ID) {
      throw new IllegalStateException(
          NO_SINGLE_QUEUE_MESSAGE.formatted(nameSegment, queueUrls.size(), queueUrls));
    }
    return queueUrls.getFirst();
  }

  /**
   * A denied call is rethrown naming the missing permission, because the SDK only reports that
   * access was denied.
   */
  private static <T> T withPermissionMessage(String permissionMessage, Supplier<T> sqsCall) {
    try {
      return sqsCall.get();
    } catch (SqsException exception) {
      if (isAccessDenied(exception)) {
        throw new IllegalStateException(permissionMessage, exception);
      }
      throw exception;
    }
  }

  private static boolean isAccessDenied(SqsException exception) {
    return exception.statusCode() == HTTP_FORBIDDEN
        || ACCESS_DENIED_ERROR_CODE.equals(exception.awsErrorDetails().errorCode());
  }
}
