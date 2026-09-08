package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_IDENTIFIER_NAME;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.APPROVAL_PATH;
import static no.sikt.nva.apitest.approvals.Approvals.IDENTIFIERS_FIELD;
import static no.sikt.nva.apitest.approvals.Approvals.SOURCE_FIELD;
import static no.sikt.nva.apitest.approvals.Approvals.approvalPayload;
import static no.sikt.nva.apitest.approvals.Approvals.createApproval;
import static no.sikt.nva.apitest.approvals.Approvals.identifier;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + APPROVAL_PATH)
class FetchApprovalTest extends IntegrationTestBase {

  private static final String JSON_MEDIA_TYPE = "application/json";
  private static final String JSON_LD_MEDIA_TYPE = "application/ld+json";
  private static final String HTML_MEDIA_TYPE = "text/html";
  private static final String CONTEXT_FIELD = "@context";
  private static final String ID_FIELD = "id";
  private static final String IDENTIFIER_FIELD = "identifier";
  private static final String APPROVAL_ID_PARAMETER = "approvalId";
  private static final String INVALID_IDENTIFIER = "not-a-uuid";
  private static final Duration APPROVAL_AVAILABLE_TIMEOUT = Duration.ofSeconds(30);

  private static String approvalLocation;
  private static String approvalIdentifier;
  private static String identifierValue;
  private static String source;

  /**
   * One approval is created for the whole class, since every test here only reads it. Fetching by
   * approval identifier queries a secondary index, which is eventually consistent, so the tests
   * wait until it answers before the first of them runs.
   */
  @BeforeAll
  static void createApprovalToFetch() {
    identifierValue = Approvals.uniqueValue();
    var payload = approvalPayload(UIB_IDENTIFIER_NAME, identifierValue);
    source = (String) payload.get(SOURCE_FIELD);

    approvalLocation = createApproval(UIB_CLIENT_SECRET, payload);
    approvalIdentifier = approvalLocation.substring(approvalLocation.lastIndexOf('/') + 1);

    pollUntil(
        APPROVAL_AVAILABLE_TIMEOUT,
        () -> fetchApproval(JSON_MEDIA_TYPE),
        response -> response.statusCode() == HTTP_OK);
  }

  /** The approval is served as JSON, identifying itself by the URI it was fetched from. */
  @Test
  @DisplayName("Get approval as json")
  @Description(useJavaDoc = true)
  void shouldReturnApprovalAsJson(SoftAssertions softly) {
    var approval =
        fetchApproval(JSON_MEDIA_TYPE)
            .then()
            .statusCode(HTTP_OK)
            .contentType(JSON_MEDIA_TYPE)
            .extract()
            .jsonPath();

    softly.assertThat(approval.getString(ID_FIELD)).isEqualTo(approvalLocation);
    softly.assertThat(approval.getString(IDENTIFIER_FIELD)).isEqualTo(approvalIdentifier);
    softly
        .assertThat(approval.getList(IDENTIFIERS_FIELD, Map.class))
        .containsExactly(identifier(UIB_IDENTIFIER_NAME, identifierValue));
    softly.assertThat(approval.getString(SOURCE_FIELD)).isEqualTo(source);
  }

  /** JSON-LD clients get the same document, pointing at the context that defines its terms. */
  @Test
  @DisplayName("Get approval as json-ld")
  @Description(useJavaDoc = true)
  void shouldReturnApprovalAsJsonLd(SoftAssertions softly) {
    var approval =
        fetchApproval(JSON_LD_MEDIA_TYPE)
            .then()
            .statusCode(HTTP_OK)
            .contentType(JSON_LD_MEDIA_TYPE)
            .extract()
            .jsonPath();

    softly.assertThat(approval.getString(ID_FIELD)).isEqualTo(approvalLocation);
    softly.assertThat(approval.getString(CONTEXT_FIELD)).endsWith(ApprovalPaths.CONTEXT_PATH);
  }

  /**
   * A handle resolves to the approval in a browser, so the same URI must also serve a readable page
   * carrying the identifier it was looked up by.
   */
  @Test
  @DisplayName("Get approval as html")
  @Description(useJavaDoc = true)
  void shouldReturnApprovalAsHtml(SoftAssertions softly) {
    var page =
        fetchApproval(HTML_MEDIA_TYPE)
            .then()
            .statusCode(HTTP_OK)
            .contentType(HTML_MEDIA_TYPE)
            .extract()
            .asString();

    softly.assertThat(page).contains("<!DOCTYPE html>");
    softly.assertThat(page).contains(identifierValue);
  }

  /** Without an Accept header the approval is served as json rather than as the page. */
  @Test
  @DisplayName("Get approval without accept header")
  @Description(useJavaDoc = true)
  void shouldReturnJsonWhenAcceptHeaderIsMissing(SoftAssertions softly) {
    var response =
        RestAssured.given().get(APPROVAL_PATH, approvalIdentifier).then().statusCode(HTTP_OK);

    softly.assertThat(response.extract().contentType()).contains(JSON_MEDIA_TYPE);
    softly
        .assertThat(response.extract().jsonPath().getString(ID_FIELD))
        .isEqualTo(approvalLocation);
  }

  /** An approval identifier that is well formed but unknown is not found. */
  @Test
  @DisplayName("Get approval that does not exist")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenApprovalDoesNotExist() {
    givenUnauthenticatedJsonRequest()
        .get(APPROVAL_PATH, UUID.randomUUID())
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }

  /** An approval identifier that is not a uuid is rejected before anything is looked up. */
  @Test
  @DisplayName("Get approval with malformed identifier")
  @Description(useJavaDoc = true)
  void shouldReturnBadRequestWhenIdentifierIsMalformed() {
    givenUnauthenticatedJsonRequest()
        .get(APPROVAL_PATH, INVALID_IDENTIFIER)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  private static Response fetchApproval(String mediaType) {
    return RestAssured.given()
        .accept(mediaType)
        .pathParam(APPROVAL_ID_PARAMETER, approvalIdentifier)
        .get(APPROVAL_PATH);
  }
}
