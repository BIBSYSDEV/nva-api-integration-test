package no.sikt.nva.apitest.kanalregister.findseries;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelForYear;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.LNCS;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.hitByPid;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.searchChannels;
import static no.sikt.nva.apitest.kanalregister.ChannelSchemas.assertMatchesSearchResponseSchema;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import no.sikt.nva.apitest.kanalregister.SharedResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findseries/channels")
class FindSeriesChannelsTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findseries";

  private static final SharedResponse LNCS_SEARCH =
      new SharedResponse(env -> searchChannels(env, RESOURCE, "name", LNCS.name(), LNCS.year()));

  /** A name search returns hits with the level for the requested year. */
  @Test
  @DisplayName("Search by name returns level for the requested year")
  @Description(useJavaDoc = true)
  void shouldReturnLevelForRequestedYearWhenSearchingByName(SoftAssertions softly) {
    var hit = LNCS_SEARCH.jsonPathForEnvironment(environment).setRootPath(hitByPid(LNCS.pid()));

    assertLevelForYear(softly, hit, LNCS);
  }

  /** The response body matches the shared search response JSON Schema. */
  @Test
  @DisplayName("Response matches the search contract")
  @Description(useJavaDoc = true)
  void shouldMatchSearchContract(SoftAssertions softly) {
    assertMatchesSearchResponseSchema(softly, LNCS_SEARCH.bodyForEnvironment(environment));
  }
}
