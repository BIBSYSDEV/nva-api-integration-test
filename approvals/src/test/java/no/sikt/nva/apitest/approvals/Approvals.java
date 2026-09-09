package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.APPROVAL_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.BASE_PATH;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsClient;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.time.Duration;
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

  private static final String APPROVAL_ID_PARAMETER = "approvalId";
  private static final String JSON_MEDIA_TYPE = "application/json";
  private static final Duration READABLE_TIMEOUT = Duration.ofSeconds(10);

  private static final String NAME_FIELD = "name";
  private static final String VALUE_FIELD = "value";
  private static final String IDENTIFIER_TYPE = "Identifier";

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

  /** An update replaces the identifiers of an existing approval, and carries nothing else. */
  public static Map<String, Object> updatePayload(String identifierName, String identifierValue) {
    return Map.of(IDENTIFIERS_FIELD, List.of(identifier(identifierName, identifierValue)));
  }

  public static Map<String, String> identifier(String name, String value) {
    return Map.of(TYPE_FIELD, IDENTIFIER_TYPE, NAME_FIELD, name, VALUE_FIELD, value);
  }

  public static String uniqueValue() {
    return UUID.randomUUID().toString();
  }

  public static String uniqueSource() {
    return "https://example.org/apitest/%s".formatted(UUID.randomUUID());
  }

  /**
   * Creates an approval as test setup rather than as the thing under test: the conflict tests need
   * an approval to collide with, and the update tests need one to change. Returns its location, so
   * a caller can address the approval it just created.
   *
   * <p>Waits until the approval can be read back before returning. An approval is looked up through
   * a secondary index, which is eventually consistent, so acting on one the moment it was created
   * can be answered as if it did not exist. Doing it here means no caller has to know which lookup
   * the endpoint it is about to call reads through.
   */
  public static String createApproval(String clientSecret, Map<String, Object> payload) {
    var location =
        givenAuthenticatedJsonRequestAsClient(clientSecret)
            .body(payload)
            .when()
            .post(BASE_PATH)
            .then()
            .statusCode(HTTP_ACCEPTED)
            .extract()
            .header(LOCATION_HEADER);

    var approvalIdentifier = location.substring(location.lastIndexOf('/') + 1);
    pollUntil(
        READABLE_TIMEOUT,
        () -> fetchApproval(approvalIdentifier, JSON_MEDIA_TYPE),
        response -> response.statusCode() == HTTP_OK);
    return location;
  }

  /**
   * Fetches an approval by identifier in the requested media type. The endpoint negotiates the
   * representation, so callers state which one they want rather than relying on a default.
   */
  public static Response fetchApproval(String approvalIdentifier, String mediaType) {
    return RestAssured.given()
        .accept(mediaType)
        .pathParam(APPROVAL_ID_PARAMETER, approvalIdentifier)
        .get(APPROVAL_PATH);
  }
}
