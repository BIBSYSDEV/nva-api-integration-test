package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_IDENTIFIER_NAME;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.BASE_PATH;
import static no.sikt.nva.apitest.approvals.Approvals.APPROVAL_TYPE;
import static no.sikt.nva.apitest.approvals.Approvals.IDENTIFIERS_FIELD;
import static no.sikt.nva.apitest.approvals.Approvals.SOURCE_FIELD;
import static no.sikt.nva.apitest.approvals.Approvals.TYPE_FIELD;
import static no.sikt.nva.apitest.approvals.Approvals.approvalPayload;
import static no.sikt.nva.apitest.approvals.Approvals.identifier;
import static no.sikt.nva.apitest.approvals.Approvals.uniqueSource;
import static no.sikt.nva.apitest.approvals.Approvals.uniqueValue;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsClient;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST " + BASE_PATH)
class CreateApprovalTest extends IntegrationTestBase {

  private static final String LOCATION_HEADER = "Location";
  private static final String RETRY_AFTER_HEADER = "Retry-After";
  private static final String HANDLE_PREFIX = "https://hdl.handle.net/";
  private static final Duration APPROVAL_AVAILABLE_TIMEOUT = Duration.ofSeconds(30);

  /**
   * Creating an approval is accepted rather than completed, so the response points at the new
   * approval with Location and says when to look for it with Retry-After. Fetching that location
   * must return the identifiers and source that were sent, along with the handle the approval was
   * minted for, and identify itself by the same URI.
   */
  @Test
  @DisplayName("Create approval")
  @Description(useJavaDoc = true)
  void shouldAcceptApprovalAndExposeItAtLocation(SoftAssertions softly) {
    var identifierValue = uniqueValue();
    var payload = approvalPayload(UIB_IDENTIFIER_NAME, identifierValue);

    var acceptedResponse =
        givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
            .body(payload)
            .when()
            .post(BASE_PATH)
            .then()
            .statusCode(HTTP_ACCEPTED)
            .extract();

    var location = acceptedResponse.header(LOCATION_HEADER);
    softly.assertThat(location).startsWith(RestAssured.baseURI + BASE_PATH + "/");
    softly.assertThat(acceptedResponse.header(RETRY_AFTER_HEADER)).isNotBlank();

    var approval =
        pollUntil(
                APPROVAL_AVAILABLE_TIMEOUT,
                () -> fetchApproval(location),
                response -> response.statusCode() == HTTP_OK)
            .jsonPath();

    softly.assertThat(approval.getString("id")).isEqualTo(location);
    softly.assertThat(approval.getString("identifiers[0].name")).isEqualTo(UIB_IDENTIFIER_NAME);
    softly.assertThat(approval.getString("identifiers[0].value")).isEqualTo(identifierValue);
    softly.assertThat(approval.getString(SOURCE_FIELD)).isEqualTo(payload.get(SOURCE_FIELD));
    softly.assertThat(approval.getString("handle")).startsWith(HANDLE_PREFIX);
  }

  private static Stream<Arguments> incompletePayloads() {
    return Stream.of(
        argumentSet(
            "Missing identifiers", Map.of(TYPE_FIELD, APPROVAL_TYPE, SOURCE_FIELD, uniqueSource())),
        argumentSet(
            "Empty identifiers",
            Map.of(
                TYPE_FIELD,
                APPROVAL_TYPE,
                IDENTIFIERS_FIELD,
                List.of(),
                SOURCE_FIELD,
                uniqueSource())),
        argumentSet(
            "Missing source",
            Map.of(
                TYPE_FIELD,
                APPROVAL_TYPE,
                IDENTIFIERS_FIELD,
                List.of(identifier(UIB_IDENTIFIER_NAME, uniqueValue())))));
  }

  /** Both identifiers and source are mandatory, and at least one identifier must be present. */
  @ParameterizedTest
  @MethodSource("incompletePayloads")
  @DisplayName("Create approval with incomplete payload")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenPayloadIsIncomplete(Map<String, Object> payload) {
    givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
        .body(payload)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /** Writing an approval is reserved for clients holding the approval-upsert scope. */
  @Test
  @DisplayName("Create approval unauthenticated")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenRequestIsUnauthenticated() {
    givenUnauthenticatedJsonRequest()
        .body(approvalPayload(UIB_IDENTIFIER_NAME))
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(HTTP_UNAUTHORIZED);
  }

  private static Response fetchApproval(String location) {
    return givenUnauthenticatedJsonRequest().get(location);
  }
}
