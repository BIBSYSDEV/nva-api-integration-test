package no.sikt.nva.apitest.base;

import static java.util.stream.Collectors.toMap;
import static no.sikt.nva.apitest.base.ApplicationConstants.getRegion;
import static no.sikt.nva.apitest.base.AwsCalls.withPermissionHint;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

/**
 * Queues are found by the {@code -<logical id>-} part of their generated name, since they have no
 * CloudFormation tags to look them up by, unlike the lambdas in {@link LambdaFunctions}.
 */
public final class MessageQueues {

  private static final String STRING_DATA_TYPE = "String";
  private static final String NO_SINGLE_QUEUE_MESSAGE =
      "Expected exactly one queue whose name contains %s, but found %d: %s";
  private static final String MISSING_LIST_PERMISSION_MESSAGE =
      "The test runner role is not allowed to list queues. It needs sqs:ListQueues.";
  private static final String MISSING_SEND_PERMISSION_MESSAGE =
      "The test runner role is not allowed to send to queue %s. It needs sqs:SendMessage on it.";
  private static final int ONE_QUEUE_PER_LOGICAL_ID = 1;

  // Shared across parallel test classes for the lifetime of the JVM, so never closed.
  private static final SqsClient SQS_CLIENT =
      SqsClient.builder().region(getRegion(new Environment())).build();

  private static final Map<String, String> QUEUE_URLS = new ConcurrentHashMap<>();

  private MessageQueues() {}

  public static void sendMessage(
      String logicalId, String messageBody, Map<String, String> stringAttributes) {
    var queueUrl = queueUrl(logicalId);
    withPermissionHint(
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
    var nameSegment = "-" + logicalId + "-";
    var queueUrls =
        withPermissionHint(
            MISSING_LIST_PERMISSION_MESSAGE,
            () ->
                SQS_CLIENT.listQueuesPaginator(request -> {}).queueUrls().stream()
                    .filter(queueUrl -> queueName(queueUrl).contains(nameSegment))
                    .toList());
    return singleQueueUrl(queueUrls, nameSegment);
  }

  private static String queueName(String queueUrl) {
    return UriWrapper.fromUri(queueUrl).getLastPathElement();
  }

  private static String singleQueueUrl(List<String> queueUrls, String nameSegment) {
    if (queueUrls.size() != ONE_QUEUE_PER_LOGICAL_ID) {
      throw new IllegalStateException(
          NO_SINGLE_QUEUE_MESSAGE.formatted(nameSegment, queueUrls.size(), queueUrls));
    }
    return queueUrls.getFirst();
  }
}
