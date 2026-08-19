package no.sikt.nva.apitest.scientificindex;

import static no.sikt.nva.apitest.base.ApplicationConstants.getRegion;

import java.util.Map;
import no.sikt.nva.apitest.base.CognitoLogin;
import nva.commons.core.Environment;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Direct DynamoDB access for removing NVI periods. The API deliberately offers no delete endpoint
 * for periods, but tests that create one need to be re-runnable, since creating a period for a
 * publishing year that already exists is rejected as a conflict.
 */
public final class NviPeriods {

  private static final String TABLE_NAME =
      CognitoLogin.getValueFromParameterStore("/test/NviTable");

  private static final String PARTITION_KEY_ATTRIBUTE = "PrimaryKeyHashKey";
  private static final String SORT_KEY_ATTRIBUTE = "PrimaryKeyRangeKey";
  private static final String PERIOD_TYPE = "PERIOD";
  private static final String KEY_DELIMITER = "#";

  private static final DynamoDbClient DYNAMO_DB_CLIENT =
      DynamoDbClient.builder().region(getRegion(new Environment())).build();

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
