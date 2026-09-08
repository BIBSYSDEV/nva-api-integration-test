package no.sikt.nva.apitest.customer;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.joining;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.customer.CustomerPaths.BASE_PATH;
import static no.sikt.nva.apitest.customer.CustomerPaths.CUSTOMER_PATH;

import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.base.User;

/** Request helpers and JSON field names for the customer API. */
public final class CustomerRequests {

  static final String RIGHTS_RETENTION_STRATEGY_FIELD = "rightsRetentionStrategy";
  static final String TYPE_FIELD = "type";
  static final String POLICY_URI_FIELD = "policyUri";

  /**
   * Deprecated alias of policyUri, kept in the authenticated representation while clients migrate.
   */
  static final String DEPRECATED_ID_FIELD = "id";

  private static final String CUSTOMERS_FIELD = "customers";
  private static final String CUSTOMER_ID_FIELD = "id";
  private static final String CRISTIN_ID_FIELD = "cristinId";
  private static final String DISPLAY_NAME_FIELD = "displayName";

  private CustomerRequests() {}

  public static JsonPath listCustomers() {
    return givenUnauthenticatedJsonRequest()
        .get(BASE_PATH)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath();
  }

  public static JsonPath fetchCustomer(User user, String identifier) {
    return givenAuthenticatedJsonRequestAsUser(user)
        .get(CUSTOMER_PATH, identifier)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath();
  }

  public static List<Map<String, Object>> customersIn(JsonPath customerList) {
    List<Map<String, Object>> customers = customerList.getList(CUSTOMERS_FIELD);
    return requireNonNullElse(customers, List.of());
  }

  public static Map<String, Object> customerIn(JsonPath customerList, Affiliation affiliation) {
    var customers = customersIn(customerList);
    return customers.stream()
        .filter(customer -> affiliation.getValue().equals(customer.get(CRISTIN_ID_FIELD)))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException(missingCustomer(affiliation, customers)));
  }

  /** The identifier is the last segment of the customer's id URI, so no UUID is hardcoded. */
  public static String identifierOf(JsonPath customerList, Affiliation affiliation) {
    var customerId =
        requireNonNull(
            (String) customerIn(customerList, affiliation).get(CUSTOMER_ID_FIELD),
            () -> "Customer %s has no id".formatted(affiliation));
    return customerId.substring(customerId.lastIndexOf('/') + 1);
  }

  /** Label for assertion messages: the display name, or the cristinId if it is missing. */
  public static String displayNameOf(Map<String, Object> customer) {
    return String.valueOf(
        requireNonNullElse(customer.get(DISPLAY_NAME_FIELD), customer.get(CRISTIN_ID_FIELD)));
  }

  public static String displayNameOf(JsonPath customer) {
    return requireNonNullElse(
        customer.getString(DISPLAY_NAME_FIELD), customer.getString(CRISTIN_ID_FIELD));
  }

  /** Empty when the field is missing, so assertions fail on the entry, not with an NPE. */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> rightsRetentionStrategyOf(Map<String, Object> customer) {
    var rightsRetentionStrategy =
        (Map<String, Object>) customer.get(RIGHTS_RETENTION_STRATEGY_FIELD);
    return requireNonNullElse(rightsRetentionStrategy, Map.of());
  }

  public static Map<String, Object> rightsRetentionStrategyOf(JsonPath customer) {
    Map<String, Object> rightsRetentionStrategy = customer.getMap(RIGHTS_RETENTION_STRATEGY_FIELD);
    return requireNonNullElse(rightsRetentionStrategy, Map.of());
  }

  private static String missingCustomer(
      Affiliation affiliation, List<Map<String, Object>> customers) {
    var cristinIds =
        customers.stream()
            .map(customer -> String.valueOf(customer.get(CRISTIN_ID_FIELD)))
            .collect(joining(", "));
    return "No customer with cristinId %s in GET %s, found: %s"
        .formatted(affiliation.getValue(), BASE_PATH, cristinIds);
  }
}
