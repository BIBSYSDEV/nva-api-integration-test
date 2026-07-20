package no.sikt.nva.apitest.kanalregister.findjournalserie;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertCountingLevelAndXMark;
import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelForYear;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.ACP;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.ACP_EISSN;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.JCM;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.SEARCH_HITS_PATH;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.hitByPid;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.searchChannels;
import static no.sikt.nva.apitest.kanalregister.ChannelSchemas.assertMatchesSearchResponseSchema;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import no.sikt.nva.apitest.kanalregister.SharedResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findjournalserie/channels")
class FindJournalserieChannelsTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findjournalserie";

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

  /** An ISSN search resolves to exactly one channel. */
  @Test
  @DisplayName("ISSN search resolves to exactly one channel")
  @Description(useJavaDoc = true)
  void shouldResolveIssnSearchToSingleChannel(SoftAssertions softly) {
    var response = searchChannels(environment, RESOURCE, "issn", ACP_EISSN, ACP.year()).jsonPath();

    softly
        .assertThat(response.getInt("entityPageInformationDto.totalResults"))
        .as("totalResults")
        .isEqualTo(1);
    softly
        .assertThat(response.getList(SEARCH_HITS_PATH + ".pid", String.class))
        .containsExactly(ACP.pid());
  }

  /** A search hit for an X-channel has the counting level and the X mark separately. */
  @Test
  @DisplayName("X-channels carry counting level and X mark separately")
  @Description(useJavaDoc = true)
  void shouldExposeCountingLevelAndXMarkSeparately(SoftAssertions softly) {
    assumeTrue(
        environment.hasXChannelLevelData(),
        "The X-channel fixture (Journal of Clinical Medicine) has no level data in kar-test");

    var hit =
        searchChannels(environment, RESOURCE, "name", JCM.name(), JCM.year())
            .jsonPath()
            .setRootPath(hitByPid(JCM.pid()));

    assertCountingLevelAndXMark(softly, hit, JCM);
  }

  /** The response body matches the shared search response JSON Schema. */
  @Test
  @DisplayName("Response matches the search contract")
  @Description(useJavaDoc = true)
  void shouldMatchSearchContract(SoftAssertions softly) {
    assertMatchesSearchResponseSchema(softly, ACP_SEARCH.bodyForEnvironment(environment));
  }
}
