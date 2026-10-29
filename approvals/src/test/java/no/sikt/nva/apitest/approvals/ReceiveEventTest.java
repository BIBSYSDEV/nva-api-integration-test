package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNSUPPORTED_TYPE;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_IDENTIFIER_NAME;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIS_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.EVENTS_PATH;
import static no.sikt.nva.apitest.approvals.Approvals.DETAIL_FIELD;
import static no.sikt.nva.apitest.approvals.Approvals.approvalPayload;
import static no.sikt.nva.apitest.approvals.Approvals.createApproval;
import static no.sikt.nva.apitest.approvals.Approvals.fetchApproval;
import static no.sikt.nva.apitest.approvals.Approvals.fetchChanges;
import static no.sikt.nva.apitest.approvals.Approvals.replacementSource;
import static no.sikt.nva.apitest.approvals.Approvals.source;
import static no.sikt.nva.apitest.approvals.Approvals.uniqueValue;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsClient;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
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
  private static final String SNAPSHOT_PATH = "changes.find { it.type == 'SourceSnapshot' }";
  private static final String SPECIALIZATION_OF_FIELD = "specializationOf";
  private static final String MEDIA_TYPE_FIELD = "mediaType";
  private static final String SOURCE_MISMATCH_MESSAGE =
      "Source provided in request does not match requested approval source";
  private static final String CUSTOMER_MISMATCH_MESSAGE =
      "Customer id does not match requested approval customer id";
  private static final Duration SNAPSHOT_TIMEOUT = Duration.ofMinutes(2);

  /**
   * A source changed notification makes the API fetch the source again and record what it got, so a
   * snapshot of the source eventually appears among the changes of the approval.
   */
  @Test
  @DisplayName("Notify that the source of an approval has changed")
  @Description
  void shouldSnapshotSourceWhenSourceHasChanged(SoftAssertions softly) {
    var approval = createApprovalWithHandle();

    sendEvent(UIB_CLIENT_SECRET, sourceChanged(source(), approval.handle()))
        .then()
        .statusCode(HTTP_ACCEPTED);

    var snapshot = awaitSnapshot(approval.identifier());
    softly.assertThat(snapshot.get(SPECIALIZATION_OF_FIELD)).isEqualTo(source());
    softly.assertThat(snapshot.get(MEDIA_TYPE_FIELD)).isNotNull();
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
    var identifier = location.substring(location.lastIndexOf('/') + 1);
    var handle = fetchApproval(identifier, JSON_MEDIA_TYPE).jsonPath().getString(HANDLE_FIELD);
    return new CreatedApproval(identifier, handle);
  }

  /** The snapshot is taken asynchronously, after the notification has been accepted. */
  private static Map<String, Object> awaitSnapshot(String approvalIdentifier) {
    return pollUntil(
            SNAPSHOT_TIMEOUT,
            () -> fetchChanges(approvalIdentifier),
            response ->
                response.statusCode() == HTTP_OK
                    && response.jsonPath().getMap(SNAPSHOT_PATH) != null)
        .jsonPath()
        .getMap(SNAPSHOT_PATH);
  }

  private record CreatedApproval(String identifier, String handle) {}
}
