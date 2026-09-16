package no.sikt.nva.apitest.customer;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Affiliation.UNIT;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.customer.CustomerPaths.CUSTOMER_PATH;
import static no.sikt.nva.apitest.customer.CustomerRequests.displayNameOf;
import static no.sikt.nva.apitest.customer.CustomerRequests.fetchCustomer;
import static no.sikt.nva.apitest.customer.CustomerRequests.identifierOf;
import static no.sikt.nva.apitest.customer.CustomerRequests.listCustomers;
import static no.sikt.nva.apitest.customer.CustomerRequests.rightsRetentionStrategyOf;
import static no.sikt.nva.apitest.customer.RightsRetentionStrategyAssertions.assertDeprecatedIdEqualsPolicyUri;
import static no.sikt.nva.apitest.customer.RightsRetentionStrategyAssertions.assertDisabled;
import static no.sikt.nva.apitest.customer.RightsRetentionStrategyAssertions.assertEnabled;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + CUSTOMER_PATH)
class FetchCustomerTest extends IntegrationTestBase {

  private static String unitIdentifier;
  private static String uibIdentifier;

  @BeforeAll
  static void resolveIdentifiers() {
    var customerList = listCustomers();
    unitIdentifier = identifierOf(customerList, UNIT);
    uibIdentifier = identifierOf(customerList, UIB);
  }

  /** Unlike the list, the full customer representation requires a login. */
  @Test
  @DisplayName("Fetch customer unauthenticated")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenUnauthenticated() {
    givenUnauthenticatedJsonRequest()
        .get(CUSTOMER_PATH, uibIdentifier)
        .then()
        .statusCode(HTTP_UNAUTHORIZED);
  }

  /**
   * While clients migrate, the authenticated representation links the policy page as both {@code
   * policyUri} and the deprecated {@code id}, with the same value. UNIT is the e2e customer with
   * RRS switched on, see {@code ListCustomersTest}.
   */
  @Test
  @DisplayName("Fetch customer with rights retention strategy enabled")
  @Description(useJavaDoc = true)
  void shouldReturnPolicyUriAndDeprecatedIdWhenRightsRetentionStrategyIsConfigured(
      SoftAssertions softly) {
    var customer = fetchCustomer(UIB_CREATOR, unitIdentifier);
    var rightsRetentionStrategy = rightsRetentionStrategyOf(customer);

    assertEnabled(softly, rightsRetentionStrategy, displayNameOf(customer));
    assertDeprecatedIdEqualsPolicyUri(softly, rightsRetentionStrategy, displayNameOf(customer));
  }

  /**
   * RRS switched off gives neither {@code policyUri} nor {@code id}, also when the editor saved an
   * empty link. UiB is expected to stay switched off, as the publication-api file upload tests rely
   * on.
   */
  @Test
  @DisplayName("Fetch customer with rights retention strategy disabled")
  @Description(useJavaDoc = true)
  void shouldReturnNullRightsRetentionStrategyWithoutPolicyUriWhenNotConfigured(
      SoftAssertions softly) {
    var customer = fetchCustomer(UIB_CREATOR, uibIdentifier);

    assertDisabled(softly, rightsRetentionStrategyOf(customer), displayNameOf(customer));
  }
}
