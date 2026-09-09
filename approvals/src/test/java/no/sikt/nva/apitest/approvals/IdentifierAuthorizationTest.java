package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_IDENTIFIER_NAME;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIS_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIS_IDENTIFIER_NAME;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.APPROVAL_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.BASE_PATH;
import static no.sikt.nva.apitest.approvals.Approvals.approvalPayload;
import static no.sikt.nva.apitest.approvals.Approvals.createApproval;
import static no.sikt.nva.apitest.approvals.Approvals.uniqueValue;
import static no.sikt.nva.apitest.approvals.Approvals.updatePayload;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsClient;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import java.util.UUID;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Identifier authorization")
class IdentifierAuthorizationTest extends IntegrationTestBase {

  private static final String DETAIL_FIELD = "detail";

  /**
   * A client may only write the identifier names its own customer is registered for. The UiS client
   * writing the name belonging to UiB is the case the whole per-customer policy exists to stop.
   */
  @Test
  @DisplayName("Create approval with another customer's identifier name")
  @Description(useJavaDoc = true)
  void shouldForbidCreatingWithAnotherCustomersIdentifierName() {
    var problem =
        givenAuthenticatedJsonRequestAsClient(UIS_CLIENT_SECRET)
            .body(approvalPayload(UIB_IDENTIFIER_NAME, uniqueValue()))
            .when()
            .post(BASE_PATH)
            .then()
            .statusCode(HTTP_FORBIDDEN)
            .extract()
            .jsonPath();

    assertThat(problem.getString(DETAIL_FIELD)).contains(UIB_IDENTIFIER_NAME);
  }

  /**
   * The same policy applies when changing an approval, not only when creating one. The approval id
   * here belongs to nothing, which is the point: authorization is decided from the request alone,
   * before the approval is looked up, so the answer is 403 rather than 404.
   */
  @Test
  @DisplayName("Update approval with another customer's identifier name")
  @Description(useJavaDoc = true)
  void shouldForbidUpdatingWithAnotherCustomersIdentifierName() {
    var problem =
        givenAuthenticatedJsonRequestAsClient(UIS_CLIENT_SECRET)
            .body(updatePayload(UIB_IDENTIFIER_NAME, uniqueValue()))
            .when()
            .put(APPROVAL_PATH, UUID.randomUUID())
            .then()
            .statusCode(HTTP_FORBIDDEN)
            .extract()
            .jsonPath();

    assertThat(problem.getString(DETAIL_FIELD)).contains(UIB_IDENTIFIER_NAME);
  }

  /**
   * The mirror of the rejection above, and the reason it means anything: the UiS client is not
   * broken, it is simply confined to its own customer's identifier names.
   */
  @Test
  @DisplayName("Create approval with the client's own identifier name")
  @Description(useJavaDoc = true)
  void shouldAllowCreatingWithOwnIdentifierName() {
    var location =
        createApproval(UIS_CLIENT_SECRET, approvalPayload(UIS_IDENTIFIER_NAME, uniqueValue()));

    assertThat(location).contains(BASE_PATH);
  }

  /** Each client is confined to its own names, so the rule is not one customer being privileged. */
  @Test
  @DisplayName("Create approval with the other client's own identifier name")
  @Description(useJavaDoc = true)
  void shouldForbidUibClientFromUsingTheUisIdentifierName() {
    givenAuthenticatedJsonRequestAsClient(UIB_CLIENT_SECRET)
        .body(approvalPayload(UIS_IDENTIFIER_NAME, uniqueValue()))
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(HTTP_FORBIDDEN);
  }

  /** A client writing its own name is accepted, which is what makes the rejections meaningful. */
  @Test
  @DisplayName("Update approval with the client's own identifier name")
  @Description(useJavaDoc = true)
  void shouldAllowUpdatingWithOwnIdentifierName() {
    var location =
        createApproval(UIS_CLIENT_SECRET, approvalPayload(UIS_IDENTIFIER_NAME, uniqueValue()));
    var approvalIdentifier = location.substring(location.lastIndexOf('/') + 1);

    givenAuthenticatedJsonRequestAsClient(UIS_CLIENT_SECRET)
        .body(updatePayload(UIS_IDENTIFIER_NAME, uniqueValue()))
        .when()
        .put(APPROVAL_PATH, approvalIdentifier)
        .then()
        .statusCode(HTTP_ACCEPTED);
  }
}
