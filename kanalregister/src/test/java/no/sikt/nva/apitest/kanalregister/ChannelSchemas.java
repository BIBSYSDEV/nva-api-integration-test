package no.sikt.nva.apitest.kanalregister;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

import org.assertj.core.api.SoftAssertions;

/**
 * Schema assertions against the shared JSON Schemas. Every endpoint family validates against the
 * same two schema files, so the families cannot drift apart structurally without a red test.
 */
public final class ChannelSchemas {

  private static final String CHANNEL_SCHEMA = "schemas/channel.json";
  private static final String SEARCH_RESPONSE_SCHEMA = "schemas/search-response.json";

  private ChannelSchemas() {}

  /** The lookup endpoints, with and without year, return the single-channel DTO. */
  public static void assertMatchesChannelSchema(SoftAssertions softly, String responseBody) {
    assertMatchesSchema(softly, responseBody, CHANNEL_SCHEMA);
  }

  /** The channels endpoints return page information plus a page of partial channel DTOs. */
  public static void assertMatchesSearchResponseSchema(SoftAssertions softly, String responseBody) {
    assertMatchesSchema(softly, responseBody, SEARCH_RESPONSE_SCHEMA);
  }

  /** After a failed match, the validator's toString() is the full validation report. */
  private static void assertMatchesSchema(
      SoftAssertions softly, String responseBody, String schemaPath) {
    var schema = matchesJsonSchemaInClasspath(schemaPath);
    var matchesSchema = schema.matches(responseBody);
    softly.assertThat(matchesSchema).as("%s %s", schemaPath, schema).isTrue();
  }
}
