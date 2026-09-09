package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_IDENTIFIER_NAME;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.APPROVAL_PATH;
import static no.sikt.nva.apitest.approvals.Approvals.IDENTIFIERS_FIELD;
import static no.sikt.nva.apitest.approvals.Approvals.LOCATION_HEADER;
import static no.sikt.nva.apitest.approvals.Approvals.approvalPayload;
import static no.sikt.nva.apitest.approvals.Approvals.createApproval;
import static no.sikt.nva.apitest.approvals.Approvals.fetchApproval;
import static no.sikt.nva.apitest.approvals.Approvals.identifier;
import static no.sikt.nva.apitest.approvals.Approvals.uniqueValue;
import static no.sikt.nva.apitest.approvals.Approvals.updatePayload;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsClient;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("PUT " + APPROVAL_PATH)
class UpdateApprovalTest extends IntegrationTestBase {

  private static final String JSON_MEDIA_TYPE = "application/json";
  private static final String CONFLICTING_KEYS_FIELD = "conflictingKeys";
  private static final String HANDLE_FIELD = "handle";
  private static final String SOURCE_FIELD = "source";
  private static final String NAME_PARAMETER = "name";
  private static final String VALUE_PARAMETER = "value";
  private static final String INVALID_IDENTIFIER = "not-a-uuid";
  private static final Duration VISIBLE_TIMEOUT = Duration.ofSeconds(30);

  /**
   * An update replaces the identifiers of an approval rather than adding to them, and leaves the
   * handle and source alone: the handle has already been published to the outside world, so it must
   * survive a change of identifiers.
   */
  @Test
  @DisplayName("Update approval identifiers")
  @Description(useJavaDoc = true)
  void shouldReplaceIdentifiers(SoftAssertions softly) {
    var originalValue = uniqueValue();
    var approvalIdentifier = createApprovalWith(originalValue);
    var beforeUpdate = fetchJson(approvalIdentifier);

    var replacementValue = uniqueValue();
    var location =
        givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
            .body(updatePayload(UIB_IDENTIFIER_NAME, replacementValue))
            .when()
            .put(APPROVAL_PATH, approvalIdentifier)
            .then()
            .statusCode(HTTP_ACCEPTED)
            .extract()
            .header(LOCATION_HEADER);

    softly.assertThat(location).endsWith(approvalIdentifier);

    var afterUpdate = awaitIdentifier(approvalIdentifier, replacementValue);
    softly
        .assertThat(afterUpdate.getList(IDENTIFIERS_FIELD, Map.class))
        .containsExactly(identifier(UIB_IDENTIFIER_NAME, replacementValue));
    softly
        .assertThat(afterUpdate.getString(HANDLE_FIELD))
        .isEqualTo(beforeUpdate.getString(HANDLE_FIELD));
    softly
        .assertThat(afterUpdate.getString(SOURCE_FIELD))
        .isEqualTo(beforeUpdate.getString(SOURCE_FIELD));
  }

  /** The identifier the approval no longer carries stops resolving to it. */
  @Test
  @DisplayName("Update approval releases the replaced identifier")
  @Description(useJavaDoc = true)
  void shouldStopResolvingTheReplacedIdentifier() {
    var originalValue = uniqueValue();
    var approvalIdentifier = createApprovalWith(originalValue);

    var replacementValue = uniqueValue();
    updateApproval(approvalIdentifier, replacementValue).then().statusCode(HTTP_ACCEPTED);
    awaitIdentifier(approvalIdentifier, replacementValue);

    givenUnauthenticatedJsonRequest()
        .queryParam(NAME_PARAMETER, UIB_IDENTIFIER_NAME)
        .queryParam(VALUE_PARAMETER, originalValue)
        .get(ApprovalPaths.BASE_PATH)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** An identifier belongs to one approval, so it cannot be moved to another by updating it. */
  @Test
  @DisplayName("Update approval with an identifier owned by another approval")
  @Description(useJavaDoc = true)
  void shouldReturnConflictWhenIdentifierBelongsToAnotherApproval() {
    var takenValue = uniqueValue();
    var otherApproval = createApprovalWith(takenValue);
    awaitIdentifier(otherApproval, takenValue);

    var approvalIdentifier = createApprovalWith(uniqueValue());

    var problem =
        updateApproval(approvalIdentifier, takenValue)
            .then()
            .statusCode(HTTP_CONFLICT)
            .extract()
            .jsonPath();

    assertThat(problem.getMap(CONFLICTING_KEYS_FIELD))
        .containsExactly(entry(UIB_IDENTIFIER_NAME, takenValue));
  }

  /** An approval must carry at least one identifier, so an update cannot empty it. */
  @Test
  @DisplayName("Update approval with no identifiers")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenIdentifiersAreEmpty() {
    var approvalIdentifier = createApprovalWith(uniqueValue());

    givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
        .body(Map.of(IDENTIFIERS_FIELD, List.of()))
        .when()
        .put(APPROVAL_PATH, approvalIdentifier)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** An approval identifier that is not a uuid is rejected before anything is looked up. */
  @Test
  @DisplayName("Update approval with malformed identifier")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenIdentifierIsMalformed() {
    givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
        .body(updatePayload(UIB_IDENTIFIER_NAME, uniqueValue()))
        .when()
        .put(APPROVAL_PATH, INVALID_IDENTIFIER)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Updating an approval that does not exist is a not found rather than a server error. */
  @Test
  @DisplayName("Update approval that does not exist")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenApprovalDoesNotExist() {
    givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
        .body(updatePayload(UIB_IDENTIFIER_NAME, uniqueValue()))
        .when()
        .put(APPROVAL_PATH, UUID.randomUUID())
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** Changing an approval is reserved for clients holding the approval-upsert scope. */
  @Test
  @DisplayName("Update approval unauthenticated")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenRequestIsUnauthenticated() {
    givenUnauthenticatedJsonRequest()
        .body(updatePayload(UIB_IDENTIFIER_NAME, uniqueValue()))
        .when()
        .put(APPROVAL_PATH, UUID.randomUUID())
        .then()
        .statusCode(HTTP_UNAUTHORIZED);
  }

  private static String createApprovalWith(String identifierValue) {
    var location =
        createApproval(UIB_CLIENT_SECRET, approvalPayload(UIB_IDENTIFIER_NAME, identifierValue));
    return location.substring(location.lastIndexOf('/') + 1);
  }

  private static Response updateApproval(String approvalIdentifier, String identifierValue) {
    return givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
        .body(updatePayload(UIB_IDENTIFIER_NAME, identifierValue))
        .when()
        .put(APPROVAL_PATH, approvalIdentifier);
  }

  /**
   * Both the update and the conflict check read through eventually consistent lookups, so a test
   * that acts immediately after a write can observe the state it replaced.
   */
  private static JsonPath awaitIdentifier(String approvalIdentifier, String identifierValue) {
    return pollUntil(
            VISIBLE_TIMEOUT,
            () -> fetchApproval(approvalIdentifier, JSON_MEDIA_TYPE),
            response ->
                response.statusCode() == HTTP_OK
                    && response
                        .jsonPath()
                        .getList(IDENTIFIERS_FIELD, Map.class)
                        .contains(identifier(UIB_IDENTIFIER_NAME, identifierValue)))
        .jsonPath();
  }

  private static JsonPath fetchJson(String approvalIdentifier) {
    return pollUntil(
            VISIBLE_TIMEOUT,
            () -> fetchApproval(approvalIdentifier, JSON_MEDIA_TYPE),
            response -> response.statusCode() == HTTP_OK)
        .jsonPath();
  }
}
