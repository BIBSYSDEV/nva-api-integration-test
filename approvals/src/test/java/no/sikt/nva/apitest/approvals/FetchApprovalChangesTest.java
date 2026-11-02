package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_IDENTIFIER_NAME;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.APPROVAL_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.CHANGES_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.CURRENT_CONTEXT_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.absolute;
import static no.sikt.nva.apitest.approvals.Approvals.approvalPayload;
import static no.sikt.nva.apitest.approvals.Approvals.createApproval;
import static no.sikt.nva.apitest.approvals.Approvals.fetchChanges;
import static no.sikt.nva.apitest.approvals.Approvals.identifierOf;
import static no.sikt.nva.apitest.approvals.Approvals.uniqueValue;
import static no.sikt.nva.apitest.approvals.Approvals.updatePayload;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsClient;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import java.time.Duration;
import java.util.UUID;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + CHANGES_PATH)
class FetchApprovalChangesTest extends IntegrationTestBase {

  private static final String CHANGE_LIST_TYPE = "ChangeList";
  private static final String REVISION_TYPE = "ApprovalRevision";
  private static final String CREATION_ACTIVITY = "ApprovalCreation";
  private static final String UPDATE_ACTIVITY = "ApprovalUpdate";
  private static final String CONTEXT_FIELD = "@context";
  private static final String TYPE_FIELD = "type";
  private static final String ID_FIELD = "id";
  private static final String ABOUT_ID_FIELD = "about.id";
  private static final String CHANGE_TYPES_FIELD = "changes.type";
  private static final String CHANGE_ACTIVITIES_FIELD = "changes.wasGeneratedBy.type";
  private static final String FIRST_CHANGE_ID_FIELD = "changes[0].id";
  private static final Duration VISIBLE_TIMEOUT = Duration.ofSeconds(30);

  /**
   * Creating an approval is itself recorded, so the change list of a new approval holds exactly the
   * revision its creation produced.
   */
  @Test
  @DisplayName("List changes of a new approval")
  @Description
  void shouldListCreationOfNewApproval(SoftAssertions softly) {
    var location = createApprovalLocation();
    var changeList = awaitChanges(identifierOf(location), 1);

    softly.assertThat(changeList.getString(TYPE_FIELD)).isEqualTo(CHANGE_LIST_TYPE);
    softly.assertThat(changeList.getString(ABOUT_ID_FIELD)).isEqualTo(location);
    softly.assertThat(changeList.getList(CHANGE_TYPES_FIELD)).containsExactly(REVISION_TYPE);
    softly
        .assertThat(changeList.getList(CHANGE_ACTIVITIES_FIELD))
        .containsExactly(CREATION_ACTIVITY);
  }

  /** Changes are listed newest first, so an update comes before the creation it followed. */
  @Test
  @DisplayName("List changes of an updated approval")
  @Description
  void shouldListUpdateBeforeCreation() {
    var approvalIdentifier = identifierOf(createApprovalLocation());
    givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
        .body(updatePayload(UIB_IDENTIFIER_NAME, uniqueValue()))
        .when()
        .put(APPROVAL_PATH, approvalIdentifier)
        .then()
        .statusCode(HTTP_ACCEPTED);

    var changeList = awaitChanges(approvalIdentifier, 2);

    assertThat(changeList.getList(CHANGE_ACTIVITIES_FIELD))
        .containsExactly(UPDATE_ACTIVITY, CREATION_ACTIVITY);
  }

  /** A listed change resolves at its own id, as a document referencing the current context. */
  @Test
  @DisplayName("Get a listed change")
  @Description
  void shouldFetchListedChange(SoftAssertions softly) {
    var changeList = awaitChanges(identifierOf(createApprovalLocation()), 1);
    var changeId = changeList.getString(FIRST_CHANGE_ID_FIELD);

    var change =
        givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
            .get(changeId)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(change.getString(ID_FIELD)).isEqualTo(changeId);
    softly.assertThat(change.getString(TYPE_FIELD)).isEqualTo(REVISION_TYPE);
    softly.assertThat(change.getString(CONTEXT_FIELD)).isEqualTo(absolute(CURRENT_CONTEXT_PATH));
  }

  /** Listing the changes of an approval that does not exist is a not found, not an empty list. */
  @Test
  @DisplayName("List changes of an approval that does not exist")
  @Description
  void shouldReturnNotFoundWhenApprovalDoesNotExist() {
    fetchChanges(UUID.randomUUID().toString()).then().statusCode(HTTP_NOT_FOUND);
  }

  /** The change history is reserved for clients holding the approval-upsert scope. */
  @Test
  @DisplayName("List changes unauthenticated")
  @Description
  void shouldReturnUnauthorizedWhenRequestIsUnauthenticated() {
    givenUnauthenticatedJsonRequest()
        .get(CHANGES_PATH, UUID.randomUUID())
        .then()
        .statusCode(HTTP_UNAUTHORIZED);
  }

  private static String createApprovalLocation() {
    return createApproval(UIB_CLIENT_SECRET, approvalPayload(UIB_IDENTIFIER_NAME));
  }

  /** Changes are read through an eventually consistent lookup, like the approval itself. */
  private static JsonPath awaitChanges(String approvalIdentifier, int expectedCount) {
    return pollUntil(
            VISIBLE_TIMEOUT,
            () -> fetchChanges(approvalIdentifier),
            response ->
                response.statusCode() == HTTP_OK
                    && response.jsonPath().getList(CHANGE_TYPES_FIELD).size() == expectedCount)
        .jsonPath();
  }
}
