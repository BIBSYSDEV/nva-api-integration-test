package no.sikt.nva.apitest.scientificindex;

import static java.util.Objects.nonNull;

import java.util.Map;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Direct DynamoDB access for removing NVI periods. The API deliberately offers no delete endpoint
 * for periods, but tests that create one need to be re-runnable, since creating a period for a
 * publishing year that already exists is rejected as a conflict.
 */
public final class NviPeriods {

  private static final String REGION =
      nonNull(System.getenv("AWS_REGION")) ? System.getenv("AWS_REGION") : "eu-west-1";

  private static final String DEFAULT_TABLE_NAME =
      "nva-nvi-master-pipelines-NvaNvi-1V33HP5I7F42I-nva-nvi";

  private static final String TABLE_NAME =
      nonNull(System.getenv("NVI_TABLE_NAME"))
          ? System.getenv("NVI_TABLE_NAME")
          : DEFAULT_TABLE_NAME;

  private static final String PARTITION_KEY_ATTRIBUTE = "PrimaryKeyHashKey";
  private static final String SORT_KEY_ATTRIBUTE = "PrimaryKeyRangeKey";
  private static final String PERIOD_TYPE = "PERIOD";
  private static final String KEY_DELIMITER = "#";

  private static final DynamoDbClient DYNAMO_DB_CLIENT =
      DynamoDbClient.builder().region(Region.of(REGION)).build();

  private NviPeriods() {}

  /**
   * Removes the period for the given publishing year, doing nothing if no such period exists. Safe
   * to call both before and after a test, so a crashed run cannot leave state that breaks the next
   * one.
   */
  public static void deletePeriod(String publishingYear) {
    DYNAMO_DB_CLIENT.deleteItem(
        request -> request.tableName(TABLE_NAME).key(periodKey(publishingYear)));
  }

  private static Map<String, AttributeValue> periodKey(String publishingYear) {
    return Map.of(
        PARTITION_KEY_ATTRIBUTE,
        AttributeValue.fromS(PERIOD_TYPE),
        SORT_KEY_ATTRIBUTE,
        AttributeValue.fromS(PERIOD_TYPE + KEY_DELIMITER + publishingYear));
  }
}
