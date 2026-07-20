package no.sikt.nva.apitest.kanalregister.findseries;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelForYear;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.LNCS;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.hitByPid;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.searchChannels;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findseries/channels")
class FindSeriesChannelsTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findseries";

  @Test
  @DisplayName("Search by name returns level for the requested year")
  @Description("A name search returns hits with the level for the requested year")
  void shouldReturnLevelForRequestedYearWhenSearchingByName(SoftAssertions softly) {
    var hit =
        searchChannels(environment, RESOURCE, "name", LNCS.name(), LNCS.year())
            .setRootPath(hitByPid(LNCS.pid()));

    assertLevelForYear(softly, hit, LNCS);
  }
}
