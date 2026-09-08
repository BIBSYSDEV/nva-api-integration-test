package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_IDENTIFIER_NAME;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.APPROVAL_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.BASE_PATH;
import static no.sikt.nva.apitest.approvals.Approvals.approvalPayload;
import static no.sikt.nva.apitest.approvals.Approvals.createApproval;
import static no.sikt.nva.apitest.approvals.Approvals.fetchApproval;
import static no.sikt.nva.apitest.approvals.Approvals.uniqueValue;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("GET " + BASE_PATH + " by query")
class FetchApprovalByQueryTest extends IntegrationTestBase {

  private static final String JSON_MEDIA_TYPE = "application/json";
  private static final String HANDLE_PARAMETER = "handle";
  private static final String NAME_PARAMETER = "name";
  private static final String VALUE_PARAMETER = "value";
  private static final String ID_FIELD = "id";
  private static final String HANDLE_FIELD = "handle";
  private static final String UNKNOWN_HANDLE = "https://hdl.handle.net/11250.1/00000000";
  private static final Duration APPROVAL_AVAILABLE_TIMEOUT = Duration.ofSeconds(30);

  private static String approvalLocation;
  private static String identifierValue;
  private static String handle;

  /**
   * One approval is created for the whole class, since every test only reads it. Its handle is
   * minted on creation, so it can only be read back rather than predicted.
   */
  @BeforeAll
  static void createApprovalToQueryFor() {
    identifierValue = uniqueValue();
    approvalLocation =
        createApproval(UIB_CLIENT_SECRET, approvalPayload(UIB_IDENTIFIER_NAME, identifierValue));
    var approvalIdentifier = approvalLocation.substring(approvalLocation.lastIndexOf('/') + 1);

    var approval =
        pollUntil(
                APPROVAL_AVAILABLE_TIMEOUT,
                () -> fetchApproval(approvalIdentifier, JSON_MEDIA_TYPE),
                response -> response.statusCode() == HTTP_OK)
            .jsonPath();
    handle = approval.getString(HANDLE_FIELD);
  }

  /** A handle is the identifier the outside world quotes, so it resolves to its approval. */
  @Test
  @DisplayName("Get approval by handle")
  @Description(useJavaDoc = true)
  void shouldReturnApprovalByHandle() {
    var approval =
        givenUnauthenticatedJsonRequest()
            .queryParam(HANDLE_PARAMETER, handle)
            .get(BASE_PATH)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    assertThat(approval.getString(ID_FIELD)).isEqualTo(approvalLocation);
  }

  /** A source system knows its own identifier, and looks the approval up by name and value. */
  @Test
  @DisplayName("Get approval by named identifier")
  @Description(useJavaDoc = true)
  void shouldReturnApprovalByNamedIdentifier() {
    var approval =
        givenUnauthenticatedJsonRequest()
            .queryParam(NAME_PARAMETER, UIB_IDENTIFIER_NAME)
            .queryParam(VALUE_PARAMETER, identifierValue)
            .get(BASE_PATH)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    assertThat(approval.getString(ID_FIELD)).isEqualTo(approvalLocation);
  }

  private static Stream<Arguments> incompleteQueries() {
    return Stream.of(
        argumentSet("No parameters", Map.of()),
        argumentSet("Name without value", Map.of(NAME_PARAMETER, UIB_IDENTIFIER_NAME)),
        argumentSet("Value without name", Map.of(VALUE_PARAMETER, "some-value")),
        argumentSet("Handle that is not a handle", Map.of(HANDLE_PARAMETER, "not-a-handle")),
        argumentSet(
            "Handle on the wrong host", Map.of(HANDLE_PARAMETER, "https://example.org/11250.1/1")),
        argumentSet(
            "Handle without a suffix", Map.of(HANDLE_PARAMETER, "https://hdl.handle.net/11250.1")),
        argumentSet(
            "Handle with more than a prefix and a suffix",
            Map.of(HANDLE_PARAMETER, "https://hdl.handle.net/11250.1/1/extra")));
  }

  /**
   * Looking an approval up takes either a handle or both parts of a named identifier. Anything less
   * is a bad request rather than an empty result, so a caller is told what to fix.
   */
  @ParameterizedTest
  @MethodSource("incompleteQueries")
  @DisplayName("Get approval with an incomplete query")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenQueryIsIncomplete(Map<String, String> queryParameters) {
    givenUnauthenticatedJsonRequest()
        .queryParams(queryParameters)
        .get(BASE_PATH)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Addressing an approval two ways at once is ambiguous, so it is refused outright. */
  @Test
  @DisplayName("Get approval by both path and query")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenAddressedBothWays() {
    givenUnauthenticatedJsonRequest()
        .queryParam(NAME_PARAMETER, UIB_IDENTIFIER_NAME)
        .queryParam(VALUE_PARAMETER, identifierValue)
        .get(APPROVAL_PATH, UUID.randomUUID())
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** A well formed handle that was never minted here belongs to no approval. */
  @Test
  @DisplayName("Get approval by unknown handle")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundForUnknownHandle() {
    givenUnauthenticatedJsonRequest()
        .queryParam(HANDLE_PARAMETER, UNKNOWN_HANDLE)
        .get(BASE_PATH)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** An identifier name that is in use, paired with a value that is not, finds nothing. */
  @Test
  @DisplayName("Get approval by unknown named identifier")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundForUnknownNamedIdentifier() {
    givenUnauthenticatedJsonRequest()
        .queryParam(NAME_PARAMETER, UIB_IDENTIFIER_NAME)
        .queryParam(VALUE_PARAMETER, uniqueValue())
        .get(BASE_PATH)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }
}
