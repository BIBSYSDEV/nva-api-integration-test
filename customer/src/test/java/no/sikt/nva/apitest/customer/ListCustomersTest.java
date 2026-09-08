package no.sikt.nva.apitest.customer;

import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Affiliation.UNIT;
import static no.sikt.nva.apitest.customer.CustomerPaths.BASE_PATH;
import static no.sikt.nva.apitest.customer.CustomerRequests.customerIn;
import static no.sikt.nva.apitest.customer.CustomerRequests.customersIn;
import static no.sikt.nva.apitest.customer.CustomerRequests.displayNameOf;
import static no.sikt.nva.apitest.customer.CustomerRequests.listCustomers;
import static no.sikt.nva.apitest.customer.CustomerRequests.rightsRetentionStrategyOf;
import static no.sikt.nva.apitest.customer.RightsRetentionStrategyAssertions.assertDisabled;
import static no.sikt.nva.apitest.customer.RightsRetentionStrategyAssertions.assertEnabled;
import static no.sikt.nva.apitest.customer.RightsRetentionStrategyAssertions.assertTypeInVocabulary;
import static no.sikt.nva.apitest.customer.RightsRetentionStrategyAssertions.assertWithoutDeprecatedId;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + BASE_PATH)
class ListCustomersTest extends IntegrationTestBase {

  /**
   * Every customer carries its current RRS with a type from the file vocabulary. RRS switched off
   * is {@code NullRightsRetentionStrategy}, not an absent field.
   */
  @Test
  @DisplayName("List customers with rights retention strategy type")
  @Description(useJavaDoc = true)
  void shouldIncludeRightsRetentionStrategyTypeForEveryCustomer(SoftAssertions softly) {
    var customers = customersIn(listCustomers());

    softly.assertThat(customers).isNotEmpty();
    customers.forEach(
        customer ->
            assertTypeInVocabulary(
                softly, rightsRetentionStrategyOf(customer), displayNameOf(customer)));
  }

  /**
   * The open list links the policy page as {@code policyUri} only. The deprecated {@code id}, which
   * JSON-LD reads as the node's identity, stays in the authenticated representation.
   */
  @Test
  @DisplayName("List customers without deprecated id in rights retention strategy")
  @Description(useJavaDoc = true)
  void shouldNotIncludeDeprecatedIdInRightsRetentionStrategy(SoftAssertions softly) {
    var customers = customersIn(listCustomers());

    softly.assertThat(customers).isNotEmpty();
    customers.forEach(
        customer ->
            assertWithoutDeprecatedId(
                softly, rightsRetentionStrategyOf(customer), displayNameOf(customer)));
  }

  /**
   * UNIT keeps RRS switched on through the Cypress feature {@code rights_retention_strategy}, so
   * its entry links to a policy page.
   */
  @Test
  @DisplayName("List customer with rights retention strategy enabled")
  @Description(useJavaDoc = true)
  void shouldIncludePolicyUriWhenRightsRetentionStrategyIsConfigured(SoftAssertions softly) {
    var unit = customerIn(listCustomers(), UNIT);
    var rightsRetentionStrategy = rightsRetentionStrategyOf(unit);

    assertEnabled(softly, rightsRetentionStrategy, displayNameOf(unit));
    assertWithoutDeprecatedId(softly, rightsRetentionStrategy, displayNameOf(unit));
  }

  /** UiB has RRS switched off, which the publication-api file upload tests also rely on. */
  @Test
  @DisplayName("List customer with rights retention strategy disabled")
  @Description(useJavaDoc = true)
  void shouldReturnNullRightsRetentionStrategyWhenNotConfigured(SoftAssertions softly) {
    var uib = customerIn(listCustomers(), UIB);

    assertDisabled(softly, rightsRetentionStrategyOf(uib), displayNameOf(uib));
  }
}
