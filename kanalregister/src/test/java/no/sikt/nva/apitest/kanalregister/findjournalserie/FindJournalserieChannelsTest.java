package no.sikt.nva.apitest.kanalregister.findjournalserie;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertCountingLevelAndXMark;
import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelForYear;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.ACP;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.ACP_EISSN;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.JCM;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.SEARCH_HITS_PATH;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.hitByPid;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.searchChannels;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findjournalserie/channels")
class FindJournalserieChannelsTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findjournalserie";

  @Test
  @DisplayName("Search by name returns level for the requested year")
  @Description("A name search returns hits with the level for the requested year")
  void shouldReturnLevelForRequestedYearWhenSearchingByName(SoftAssertions softly) {
    var hit =
        searchChannels(environment, RESOURCE, "name", ACP.name(), ACP.year())
            .setRootPath(hitByPid(ACP.pid()));

    assertLevelForYear(softly, hit, ACP);
  }

  @Test
  @DisplayName("ISSN search resolves to exactly one channel")
  @Description("An ISSN search resolves to exactly one channel")
  void shouldResolveIssnSearchToSingleChannel(SoftAssertions softly) {
    var response = searchChannels(environment, RESOURCE, "issn", ACP_EISSN, ACP.year());

    softly
        .assertThat(response.getInt("entityPageInformationDto.totalResults"))
        .as("totalResults")
        .isEqualTo(1);
    softly
        .assertThat(response.getList(SEARCH_HITS_PATH + ".pid", String.class))
        .containsExactly(ACP.pid());
  }

  @Test
  @DisplayName("X-channels carry counting level and X mark separately")
  @Description("A search hit for an X-channel has the counting level and the X mark separately")
  void shouldExposeCountingLevelAndXMarkSeparately(SoftAssertions softly) {
    assumeTrue(
        environment.hasXChannelLevelData(),
        "The X-channel fixture (Journal of Clinical Medicine) has no level data in kar-test");

    var hit =
        searchChannels(environment, RESOURCE, "name", JCM.name(), JCM.year())
            .setRootPath(hitByPid(JCM.pid()));

    assertCountingLevelAndXMark(softly, hit, JCM);
  }
}
