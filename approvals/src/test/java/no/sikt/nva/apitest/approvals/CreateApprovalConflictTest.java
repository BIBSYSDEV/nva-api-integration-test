package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_IDENTIFIER_NAME;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.BASE_PATH;
import static no.sikt.nva.apitest.approvals.Approvals.APPROVAL_TYPE;
import static no.sikt.nva.apitest.approvals.Approvals.IDENTIFIERS_FIELD;
import static no.sikt.nva.apitest.approvals.Approvals.SOURCE_FIELD;
import static no.sikt.nva.apitest.approvals.Approvals.TYPE_FIELD;
import static no.sikt.nva.apitest.approvals.Approvals.approvalPayload;
import static no.sikt.nva.apitest.approvals.Approvals.createApproval;
import static no.sikt.nva.apitest.approvals.Approvals.identifier;
import static no.sikt.nva.apitest.approvals.Approvals.uniqueSource;
import static no.sikt.nva.apitest.approvals.Approvals.uniqueValue;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsClient;
import static org.assertj.core.api.Assertions.entry;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.Map;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST " + BASE_PATH + " with an identifier that is taken")
class CreateApprovalConflictTest extends IntegrationTestBase {

  private static final String CONFLICTING_KEYS_FIELD = "conflictingKeys";
  private static final String DETAIL_FIELD = "detail";

  /**
   * An identifier belongs to exactly one approval, so reusing one is rejected. The response names
   * the identifier that was already taken, so a client can tell what to change.
   */
  @Test
  @DisplayName("Create approval reusing an identifier")
  @Description(useJavaDoc = true)
  void shouldReturnConflictWhenIdentifierIsAlreadyTaken(SoftAssertions softly) {
    var takenValue = uniqueValue();
    createApproval(UIB_CLIENT_SECRET, approvalPayload(UIB_IDENTIFIER_NAME, takenValue));

    var problem = postExpectingConflict(approvalPayload(UIB_IDENTIFIER_NAME, takenValue));

    softly
        .assertThat(problem.getMap(CONFLICTING_KEYS_FIELD))
        .containsExactly(entry(UIB_IDENTIFIER_NAME, takenValue));
    softly.assertThat(problem.getString(DETAIL_FIELD)).contains(takenValue);
  }

  /**
   * A request is rejected as a whole when only one of its identifiers is taken, and the response
   * reports that one rather than every identifier in the request.
   */
  @Test
  @DisplayName("Create approval where one of several identifiers is taken")
  @Description(useJavaDoc = true)
  void shouldReportOnlyTheIdentifierThatWasTaken(SoftAssertions softly) {
    var takenValue = uniqueValue();
    var freeValue = uniqueValue();
    createApproval(UIB_CLIENT_SECRET, approvalPayload(UIB_IDENTIFIER_NAME, takenValue));

    var payload =
        Map.<String, Object>of(
            TYPE_FIELD,
            APPROVAL_TYPE,
            IDENTIFIERS_FIELD,
            List.of(
                identifier(UIB_IDENTIFIER_NAME, takenValue),
                identifier(UIB_IDENTIFIER_NAME, freeValue)),
            SOURCE_FIELD,
            uniqueSource());

    var problem = postExpectingConflict(payload);

    softly
        .assertThat(problem.getMap(CONFLICTING_KEYS_FIELD))
        .containsExactly(entry(UIB_IDENTIFIER_NAME, takenValue));
    softly.assertThat(problem.getString(DETAIL_FIELD)).doesNotContain(freeValue);
  }

  private static JsonPath postExpectingConflict(Map<String, Object> payload) {
    return givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
        .body(payload)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(HTTP_CONFLICT)
        .extract()
        .jsonPath();
  }
}
