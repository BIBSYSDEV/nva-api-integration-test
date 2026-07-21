package no.sikt.nva.apitest.kanalregister.findjournal;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelForYear;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.ACP;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.hitByPid;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.searchChannels;
import static no.sikt.nva.apitest.kanalregister.ChannelSchemas.assertMatchesSearchResponseSchema;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import no.sikt.nva.apitest.kanalregister.SharedResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findjournal/channels")
class FindJournalChannelsTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findjournal";

  private static final SharedResponse ACP_SEARCH =
      new SharedResponse(env -> searchChannels(env, RESOURCE, "name", ACP.name(), ACP.year()));

  /** A name search returns hits with the level for the requested year. */
  @Test
  @DisplayName("Search by name returns level for the requested year")
  @Description(useJavaDoc = true)
  void shouldReturnLevelForRequestedYearWhenSearchingByName(SoftAssertions softly) {
    var hit = ACP_SEARCH.jsonPathForEnvironment(environment).setRootPath(hitByPid(ACP.pid()));

    assertLevelForYear(softly, hit, ACP);
  }

  /** The response body matches the shared search response JSON Schema. */
  @Test
  @DisplayName("Response matches the search contract")
  @Description(useJavaDoc = true)
  void shouldMatchSearchContract(SoftAssertions softly) {
    assertMatchesSearchResponseSchema(softly, ACP_SEARCH.bodyForEnvironment(environment));
  }
}
