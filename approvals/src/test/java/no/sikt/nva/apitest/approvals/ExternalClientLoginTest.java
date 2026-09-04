package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIS_CLIENT_SECRET;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsClient;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import java.util.stream.Stream;
import no.sikt.nva.apitest.base.ClientCredentialsLogin;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + ExternalClientLoginTest.EXTERNAL_CLIENTS_PATH)
class ExternalClientLoginTest extends IntegrationTestBase {

  static final String EXTERNAL_CLIENTS_PATH = "/users-roles/external-clients";

  private static Stream<Arguments> approvalClients() {
    return Stream.of(
        argumentSet("UiB approval client", UIB_CLIENT_SECRET),
        argumentSet("UiS approval client", UIS_CLIENT_SECRET));
  }

  /**
   * Verifies the whole authentication chain the approval endpoints depend on: the seeded secret
   * yields a client credentials token, the token carries a scope the endpoint accepts, and the
   * identity service resolves it to the customer the client was created for. This is the same
   * lookup the approval API makes before authorizing identifier names, so a failure here explains
   * every later 401.
   */
  @ParameterizedTest
  @MethodSource("approvalClients")
  @DisplayName("Resolve customer for client")
  @Description(useJavaDoc = true)
  void shouldResolveCustomerForSeededClient(String secretName, SoftAssertions softly) {
    var seededClient = ClientCredentialsLogin.client(secretName);

    var response =
        givenAuthenticatedJsonRequestAsClient(secretName)
            .get(EXTERNAL_CLIENTS_PATH)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("clientId")).isEqualTo(seededClient.clientId());
    softly
        .assertThat(response.getString("customerUri"))
        .isEqualTo(seededClient.customer().toString());
  }
}
