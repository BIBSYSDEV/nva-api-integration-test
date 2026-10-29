package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_IDENTIFIER_NAME;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.APPROVAL_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.BASE_PATH;
import static no.sikt.nva.apitest.approvals.Approvals.approvalPayload;
import static no.sikt.nva.apitest.approvals.Approvals.uniqueValue;
import static no.sikt.nva.apitest.approvals.Approvals.updatePayload;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsClient;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import java.util.UUID;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Source authorization")
class SourceAuthorizationTest extends IntegrationTestBase {

  private static final String DETAIL_FIELD = "detail";
  private static final String FOREIGN_SOURCE_FORMAT = "https://example.org/%s";
  private static final String SOURCE_MISMATCH_MESSAGE = "matches no source config";

  /**
   * A client may only point an approval at a source under one of its customer's source configs, so
   * a source on a host the customer has not registered is rejected.
   */
  @Test
  @DisplayName("Create approval with a source outside the customer's source configs")
  @Description
  void shouldForbidCreatingWithForeignSource() {
    var source = foreignSource();
    var problem =
        givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
            .body(approvalPayload(UIB_IDENTIFIER_NAME, uniqueValue(), source))
            .when()
            .post(BASE_PATH)
            .then()
            .statusCode(HTTP_FORBIDDEN)
            .extract()
            .jsonPath();

    assertThat(problem.getString(DETAIL_FIELD)).contains(source, SOURCE_MISMATCH_MESSAGE);
  }

  /**
   * The source is authorized from the request alone, before the approval is looked up, so the
   * answer is 403 even though the approval id belongs to nothing.
   */
  @Test
  @DisplayName("Update approval with a source outside the customer's source configs")
  @Description
  void shouldForbidUpdatingWithForeignSource() {
    var source = foreignSource();
    var problem =
        givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
            .body(updatePayload(UIB_IDENTIFIER_NAME, uniqueValue(), source))
            .when()
            .put(APPROVAL_PATH, UUID.randomUUID())
            .then()
            .statusCode(HTTP_FORBIDDEN)
            .extract()
            .jsonPath();

    assertThat(problem.getString(DETAIL_FIELD)).contains(source, SOURCE_MISMATCH_MESSAGE);
  }

  private static String foreignSource() {
    return FOREIGN_SOURCE_FORMAT.formatted(uniqueValue());
  }
}
