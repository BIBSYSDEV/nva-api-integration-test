package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNSUPPORTED_TYPE;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_IDENTIFIER_NAME;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIS_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.APPROVAL_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.EVENTS_PATH;
import static no.sikt.nva.apitest.approvals.Approvals.DETAIL_FIELD;
import static no.sikt.nva.apitest.approvals.Approvals.SOURCE_FIELD;
import static no.sikt.nva.apitest.approvals.Approvals.approvalPayload;
import static no.sikt.nva.apitest.approvals.Approvals.createApproval;
import static no.sikt.nva.apitest.approvals.Approvals.fetchApproval;
import static no.sikt.nva.apitest.approvals.Approvals.fetchChanges;
import static no.sikt.nva.apitest.approvals.Approvals.identifierOf;
import static no.sikt.nva.apitest.approvals.Approvals.replacementSource;
import static no.sikt.nva.apitest.approvals.Approvals.source;
import static no.sikt.nva.apitest.approvals.Approvals.uniqueValue;
import static no.sikt.nva.apitest.approvals.Approvals.updatePayload;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsClient;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("POST " + EVENTS_PATH)
class ReceiveEventTest extends IntegrationTestBase {

  private static final String CLOUD_EVENTS_MEDIA_TYPE = "application/cloudevents+json";
  private static final String JSON_MEDIA_TYPE = "application/json";
  private static final String SPEC_VERSION = "1.0";
  private static final String SOURCE_CHANGED_TYPE = "no.sikt.nva.approval.source.changed";
  private static final String SPEC_VERSION_FIELD = "specversion";
  private static final String EVENT_ID_FIELD = "id";
  private static final String EVENT_SOURCE_FIELD = "source";
  private static final String EVENT_TYPE_FIELD = "type";
  private static final String SUBJECT_FIELD = "subject";
  private static final String TIME_FIELD = "time";
  private static final String UNKNOWN_HANDLE_FORMAT = "%s/%s";
  private static final String HANDLE_FIELD = "handle";
  private static final String SNAPSHOT_SOURCES_PATH =
      "changes.findAll { it.type == 'SourceSnapshot' }.specializationOf";
  private static final String SOURCE_MISMATCH_MESSAGE =
      "Source provided in request does not match requested approval source";
  private static final String CUSTOMER_MISMATCH_MESSAGE =
      "Customer id does not match requested approval customer id";
  private static final Duration SNAPSHOT_TIMEOUT = Duration.ofMinutes(2);

  /**
   * A CloudEvent may be delivered more than once, and a redelivery produces no second snapshot.
   * That no snapshot was taken can only be seen once a later one has been, so the test replaces the
   * source and waits for the snapshot that revision produces. An event still in flight by then is
   * skipped, since the approval no longer points at its source.
   */
  @Test
  @DisplayName("Notify twice with the same CloudEvent")
  @Description
  void shouldSnapshotRedeliveredEventOnce() {
    var approval = createApprovalWithHandle();
    var event = sourceChanged(source(), approval.handle());

    sendEvent(UIB_CLIENT_SECRET, event).then().statusCode(HTTP_ACCEPTED);
    sendEvent(UIB_CLIENT_SECRET, event).then().statusCode(HTTP_ACCEPTED);

    assertThat(snapshotSourcesAfterSourceReplaced(approval))
        .containsExactly(replacementSource(), source());
  }

  /**
   * A snapshot records a change at the source, so a notification about a source whose content is
   * the same as in the latest snapshot, here the one taken when the approval was created, records
   * nothing. As above, the test ends with the snapshot of a replaced source.
   */
  @Test
  @DisplayName("Notify about a source that has not changed since the latest snapshot")
  @Description
  void shouldNotSnapshotUnchangedSource() {
    var approval = createApprovalWithHandle();

    sendEvent(UIB_CLIENT_SECRET, sourceChanged(source(), approval.handle()))
        .then()
        .statusCode(HTTP_ACCEPTED);

    assertThat(snapshotSourcesAfterSourceReplaced(approval))
        .containsExactly(replacementSource(), source());
  }

  /** A notification is only about the source the approval points at, not any other source. */
  @Test
  @DisplayName("Notify about a source the approval does not point at")
  @Description
  void shouldForbidEventForAnotherSource() {
    var approval = createApprovalWithHandle();

    var problem =
        sendEvent(UIB_CLIENT_SECRET, sourceChanged(replacementSource(), approval.handle()))
            .then()
            .statusCode(HTTP_FORBIDDEN)
            .extract()
            .jsonPath();

    assertThat(problem.getString(DETAIL_FIELD)).isEqualTo(SOURCE_MISMATCH_MESSAGE);
  }

  /** Only a client of the customer owning the approval may notify about its source. */
  @Test
  @DisplayName("Notify about an approval owned by another customer")
  @Description
  void shouldForbidEventForAnotherCustomersApproval() {
    var approval = createApprovalWithHandle();

    var problem =
        sendEvent(UIS_CLIENT_SECRET, sourceChanged(source(), approval.handle()))
            .then()
            .statusCode(HTTP_FORBIDDEN)
            .extract()
            .jsonPath();

    assertThat(problem.getString(DETAIL_FIELD)).isEqualTo(CUSTOMER_MISMATCH_MESSAGE);
  }

  /** A notification about a handle no approval has is a not found. */
  @Test
  @DisplayName("Notify about a handle that belongs to no approval")
  @Description
  void shouldReturnNotFoundForUnknownHandle() {
    var handle = createApprovalWithHandle().handle();
    var unknownHandle =
        UNKNOWN_HANDLE_FORMAT.formatted(
            handle.substring(0, handle.lastIndexOf('/')), uniqueValue());

    sendEvent(UIB_CLIENT_SECRET, sourceChanged(source(), unknownHandle))
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** Notifications are CloudEvents in structured mode, so a plain JSON body is not accepted. */
  @Test
  @DisplayName("Notify with plain JSON instead of a CloudEvent")
  @Description
  void shouldRejectPlainJson() {
    var approval = createApprovalWithHandle();

    givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
        .contentType(JSON_MEDIA_TYPE)
        .body(sourceChanged(source(), approval.handle()))
        .when()
        .post(EVENTS_PATH)
        .then()
        .statusCode(HTTP_UNSUPPORTED_TYPE);
  }

  private static Response sendEvent(String clientSecret, Map<String, String> event) {
    return givenAuthenticatedJsonRequestAsClient(clientSecret)
        .contentType(CLOUD_EVENTS_MEDIA_TYPE)
        .accept(ContentType.JSON)
        .body(event)
        .when()
        .post(EVENTS_PATH);
  }

  private static Map<String, String> sourceChanged(String source, String handle) {
    return Map.of(
        SPEC_VERSION_FIELD, SPEC_VERSION,
        EVENT_ID_FIELD, uniqueValue(),
        EVENT_SOURCE_FIELD, source,
        EVENT_TYPE_FIELD, SOURCE_CHANGED_TYPE,
        SUBJECT_FIELD, handle,
        TIME_FIELD, Instant.now().toString());
  }

  private static CreatedApproval createApprovalWithHandle() {
    var location = createApproval(UIB_CLIENT_SECRET, approvalPayload(UIB_IDENTIFIER_NAME));
    var identifier = identifierOf(location);
    var handle = fetchApproval(identifier, JSON_MEDIA_TYPE).jsonPath().getString(HANDLE_FIELD);
    return new CreatedApproval(identifier, handle);
  }

  private static List<String> snapshotSourcesAfterSourceReplaced(CreatedApproval approval) {
    replaceSource(approval.identifier());
    return awaitSnapshotSources(approval.identifier(), replacementSource());
  }

  private static void replaceSource(String approvalIdentifier) {
    givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
        .body(updatePayload(UIB_IDENTIFIER_NAME, uniqueValue(), replacementSource()))
        .when()
        .put(APPROVAL_PATH, approvalIdentifier)
        .then()
        .statusCode(HTTP_ACCEPTED);
    pollUntil(
        SNAPSHOT_TIMEOUT,
        () -> fetchApproval(approvalIdentifier, JSON_MEDIA_TYPE),
        response ->
            response.statusCode() == HTTP_OK
                && replacementSource().equals(response.jsonPath().getString(SOURCE_FIELD)));
  }

  private static List<String> awaitSnapshotSources(String approvalIdentifier, String source) {
    return pollUntil(
            SNAPSHOT_TIMEOUT,
            () -> fetchChanges(approvalIdentifier),
            response ->
                response.statusCode() == HTTP_OK
                    && response.jsonPath().getList(SNAPSHOT_SOURCES_PATH).contains(source))
        .jsonPath()
        .getList(SNAPSHOT_SOURCES_PATH, String.class);
  }

  private record CreatedApproval(String identifier, String handle) {}
}
