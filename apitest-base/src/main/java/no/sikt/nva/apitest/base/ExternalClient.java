package no.sikt.nva.apitest.base;

import io.restassured.path.json.JsonPath;
import java.net.URI;

/**
 * An external API client, as stored by the seeding in NVA-end-to-end-testing. The fields are those
 * the identity service returns when the client is created, so the whole response can be stored as
 * the secret verbatim.
 */
public record ExternalClient(String clientId, String clientSecret, URI tokenUri, URI customer) {

  private static final String CLIENT_ID_FIELD = "clientId";
  private static final String CLIENT_SECRET_FIELD = "clientSecret";
  private static final String CLIENT_URL_FIELD = "clientUrl";
  private static final String CUSTOMER_FIELD = "customer";

  public static ExternalClient fromSecret(String secretString) {
    var secret = JsonPath.from(secretString);
    return new ExternalClient(
        secret.getString(CLIENT_ID_FIELD),
        secret.getString(CLIENT_SECRET_FIELD),
        URI.create(secret.getString(CLIENT_URL_FIELD)),
        URI.create(secret.getString(CUSTOMER_FIELD)));
  }

  /** Deliberately omits the client secret, so it cannot reach a log or an assertion message. */
  @Override
  public String toString() {
    return "ExternalClient[clientId=%s, customer=%s]".formatted(clientId, customer);
  }
}
