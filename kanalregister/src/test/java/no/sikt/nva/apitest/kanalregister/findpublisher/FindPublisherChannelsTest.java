package no.sikt.nva.apitest.kanalregister.findpublisher;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertDecisionTextsAreNotNullStrings;
import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelForYear;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.GYLDENDAL_UNDERVISNING;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.hitByPid;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.searchChannels;
import static no.sikt.nva.apitest.kanalregister.ChannelSchemas.assertMatchesSearchResponseSchema;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import io.restassured.path.json.JsonPath;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryEnvironment;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import no.sikt.nva.apitest.kanalregister.SharedResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findpublisher/channels")
class FindPublisherChannelsTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findpublisher";

  private static final SharedResponse GYLDENDAL_UNDERVISNING_SEARCH =
      new SharedResponse(
          env ->
              searchChannels(
                  env,
                  RESOURCE,
                  "name",
                  GYLDENDAL_UNDERVISNING.name(),
                  GYLDENDAL_UNDERVISNING.year()));

  /** A name search returns hits with the level for the requested year. */
  @Test
  @DisplayName("Search by name returns level for the requested year")
  @Description(useJavaDoc = true)
  void shouldReturnLevelForRequestedYearWhenSearchingByName(SoftAssertions softly) {
    assertLevelForYear(softly, gyldendalUndervisningHit(environment), GYLDENDAL_UNDERVISNING);
  }

  /** Fields without a value are JSON null, never the literal string "null". */
  @Test
  @DisplayName("Missing values are JSON null, not the string \"null\"")
  @Description(useJavaDoc = true)
  @Issue("NP-51484")
  void shouldRepresentMissingDecisionTextsAsNull(SoftAssertions softly) {
    assertDecisionTextsAreNotNullStrings(softly, gyldendalUndervisningHit(environment));
  }

  /** The response body matches the shared search response JSON Schema. */
  @Test
  @DisplayName("Response matches the search contract")
  @Description(useJavaDoc = true)
  void shouldMatchSearchContract(SoftAssertions softly) {
    assertMatchesSearchResponseSchema(
        softly, GYLDENDAL_UNDERVISNING_SEARCH.bodyForEnvironment(environment));
  }

  private static JsonPath gyldendalUndervisningHit(ChannelRegistryEnvironment environment) {
    return GYLDENDAL_UNDERVISNING_SEARCH
        .jsonPathForEnvironment(environment)
        .setRootPath(hitByPid(GYLDENDAL_UNDERVISNING.pid()));
  }
}
