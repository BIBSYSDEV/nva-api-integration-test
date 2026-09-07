package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.BASE_PATH;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Payloads for the approval write endpoints. Identifier values are unique per call, because an
 * identifier may belong to only one approval and there is no way to delete one afterwards: reusing
 * a value would make every rerun collide with the approvals left by the previous one.
 */
public final class Approvals {

  public static final String TYPE_FIELD = "type";
  public static final String IDENTIFIERS_FIELD = "identifiers";
  public static final String SOURCE_FIELD = "source";
  public static final String APPROVAL_TYPE = "Approval";
  public static final String LOCATION_HEADER = "Location";

  private static final String NAME_FIELD = "name";
  private static final String VALUE_FIELD = "value";
  private static final String IDENTIFIER_TYPE = "Identifier";
  private static final String SOURCE_TEMPLATE = "https://example.org/apitest/%s";

  private Approvals() {}

  public static Map<String, Object> approvalPayload(String identifierName) {
    return approvalPayload(identifierName, uniqueValue());
  }

  public static Map<String, Object> approvalPayload(String identifierName, String identifierValue) {
    return Map.of(
        TYPE_FIELD, APPROVAL_TYPE,
        IDENTIFIERS_FIELD, List.of(identifier(identifierName, identifierValue)),
        SOURCE_FIELD, uniqueSource());
  }

  public static Map<String, String> identifier(String name, String value) {
    return Map.of(TYPE_FIELD, IDENTIFIER_TYPE, NAME_FIELD, name, VALUE_FIELD, value);
  }

  public static String uniqueValue() {
    return UUID.randomUUID().toString();
  }

  public static String uniqueSource() {
    return SOURCE_TEMPLATE.formatted(UUID.randomUUID());
  }

  /**
   * Creates an approval as test setup rather than as the thing under test: the conflict tests need
   * an approval to collide with, and the update tests need one to change. Returns its location, so
   * a caller can address the approval it just created.
   */
  public static String createApproval(String clientSecret, Map<String, Object> payload) {
    return givenAuthenticatedJsonRequestAsClient(clientSecret)
        .body(payload)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(HTTP_ACCEPTED)
        .extract()
        .header(LOCATION_HEADER);
  }
}
