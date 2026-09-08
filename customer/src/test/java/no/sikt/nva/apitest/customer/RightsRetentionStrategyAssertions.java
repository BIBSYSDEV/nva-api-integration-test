package no.sikt.nva.apitest.customer;

import static no.sikt.nva.apitest.customer.CustomerRequests.DEPRECATED_ID_FIELD;
import static no.sikt.nva.apitest.customer.CustomerRequests.POLICY_URI_FIELD;
import static no.sikt.nva.apitest.customer.CustomerRequests.TYPE_FIELD;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;

/**
 * Shared assertions on a customer's rights retention strategy (RRS). The list and the single
 * customer expose the same object, so both endpoint tests assert through these helpers.
 */
public final class RightsRetentionStrategyAssertions {

  private static final String NULL_RIGHTS_RETENTION_STRATEGY = "NullRightsRetentionStrategy";
  private static final String RIGHTS_RETENTION_STRATEGY = "RightsRetentionStrategy";
  private static final String OVERRIDABLE_RIGHTS_RETENTION_STRATEGY =
      "OverridableRightsRetentionStrategy";

  /** Same vocabulary as {@code rightsRetentionStrategy.configuredType} on files. */
  private static final Set<String> RIGHTS_RETENTION_STRATEGY_TYPES =
      Set.of(
          NULL_RIGHTS_RETENTION_STRATEGY,
          RIGHTS_RETENTION_STRATEGY,
          OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);

  private static final Set<String> ENABLED_RIGHTS_RETENTION_STRATEGY_TYPES =
      Set.of(RIGHTS_RETENTION_STRATEGY, OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);

  private static final String RIGHTS_RETENTION_STRATEGY_OF = "rightsRetentionStrategy of %s";
  private static final String TYPE_OF = "rightsRetentionStrategy.type of %s";
  private static final String POLICY_URI_OF = "rightsRetentionStrategy.policyUri of %s";

  private RightsRetentionStrategyAssertions() {}

  /** The RRS is present with a type from the three known values, also when RRS is switched off. */
  public static void assertTypeInVocabulary(
      SoftAssertions softly, Map<String, Object> rightsRetentionStrategy, String customer) {
    softly
        .assertThat(rightsRetentionStrategy)
        .as(RIGHTS_RETENTION_STRATEGY_OF, customer)
        .isNotEmpty();
    softly
        .assertThat(rightsRetentionStrategy.get(TYPE_FIELD))
        .as(TYPE_OF, customer)
        .isIn(RIGHTS_RETENTION_STRATEGY_TYPES);
  }

  /** RRS switched on: an enabled type and an absolute URI to the policy page. */
  public static void assertEnabled(
      SoftAssertions softly, Map<String, Object> rightsRetentionStrategy, String customer) {
    var policyUri = rightsRetentionStrategy.get(POLICY_URI_FIELD);
    softly
        .assertThat(rightsRetentionStrategy.get(TYPE_FIELD))
        .as(TYPE_OF, customer)
        .isIn(ENABLED_RIGHTS_RETENTION_STRATEGY_TYPES);
    softly.assertThat(policyUri).as(POLICY_URI_OF, customer).isNotNull();
    softly
        .assertThat(isAbsoluteUri(policyUri))
        .as(POLICY_URI_OF + " is an absolute URI: %s", customer, policyUri)
        .isTrue();
  }

  /** RRS switched off: NullRightsRetentionStrategy and no policy page under either name. */
  public static void assertDisabled(
      SoftAssertions softly, Map<String, Object> rightsRetentionStrategy, String customer) {
    softly
        .assertThat(rightsRetentionStrategy)
        .as(RIGHTS_RETENTION_STRATEGY_OF, customer)
        .containsEntry(TYPE_FIELD, NULL_RIGHTS_RETENTION_STRATEGY)
        .doesNotContainKey(POLICY_URI_FIELD)
        .doesNotContainKey(DEPRECATED_ID_FIELD);
  }

  /** The open list carries the RRS, but never the deprecated id. */
  public static void assertWithoutDeprecatedId(
      SoftAssertions softly, Map<String, Object> rightsRetentionStrategy, String customer) {
    softly
        .assertThat(rightsRetentionStrategy)
        .as(RIGHTS_RETENTION_STRATEGY_OF, customer)
        .containsKey(TYPE_FIELD)
        .doesNotContainKey(DEPRECATED_ID_FIELD);
  }

  /**
   * The authenticated representation carries the deprecated id with the same value as policyUri.
   */
  public static void assertDeprecatedIdEqualsPolicyUri(
      SoftAssertions softly, Map<String, Object> rightsRetentionStrategy, String customer) {
    softly
        .assertThat(rightsRetentionStrategy.get(DEPRECATED_ID_FIELD))
        .as("rightsRetentionStrategy.id of %s equals policyUri", customer)
        .isNotNull()
        .isEqualTo(rightsRetentionStrategy.get(POLICY_URI_FIELD));
  }

  private static boolean isAbsoluteUri(Object value) {
    boolean absolute;
    try {
      absolute = value instanceof String uri && new URI(uri).isAbsolute();
    } catch (URISyntaxException malformed) {
      absolute = false;
    }
    return absolute;
  }
}
