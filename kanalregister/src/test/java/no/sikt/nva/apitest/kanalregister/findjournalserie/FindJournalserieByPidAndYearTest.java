package no.sikt.nva.apitest.kanalregister.findjournalserie;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertCountingLevelAndXMark;
import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelDisplayMatchesLevel;
import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelForYear;
import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelHistoryIncludesYear;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.ACP;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.JCM;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.lookUp;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import no.sikt.nva.apitest.kanalregister.SharedResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findjournalserie/{pid}/{year}")
class FindJournalserieByPidAndYearTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findjournalserie";

  private static final SharedResponse ACP_LOOKUP =
      new SharedResponse(env -> lookUp(env, RESOURCE, ACP.pid(), ACP.year()));

  @Test
  @DisplayName("Lookup returns level for the requested year")
  @Description("A lookup returns the level for the requested year, like search does")
  @Issue("NP-51485")
  void shouldReturnLevelForRequestedYear(SoftAssertions softly) {
    assertLevelForYear(softly, ACP_LOOKUP.forEnvironment(environment), ACP);
  }

  @Test
  @DisplayName("Lookup exposes levelDisplay alongside level")
  @Description("A lookup exposes levelDisplay, without which X-channels cannot be distinguished")
  @Issue("NP-51483")
  void shouldExposeLevelDisplay(SoftAssertions softly) {
    assertLevelDisplayMatchesLevel(softly, ACP_LOOKUP.forEnvironment(environment), ACP);
  }

  @Test
  @DisplayName("Level history includes the requested year")
  @Description("A lookup's levelHistories includes the requested year")
  void shouldIncludeRequestedYearInLevelHistory(SoftAssertions softly) {
    assertLevelHistoryIncludesYear(softly, ACP_LOOKUP.forEnvironment(environment), ACP);
  }

  @Test
  @DisplayName("X-channels carry counting level and X mark separately")
  @Description("A lookup on an X-channel has the counting level and the X mark separately")
  @Issue("NP-51486")
  void shouldExposeCountingLevelAndXMarkSeparately(SoftAssertions softly) {
    assumeTrue(
        environment.hasXChannelLevelData(),
        "The X-channel fixture (Journal of Clinical Medicine) has no level data in kar-test");

    var response = lookUp(environment, RESOURCE, JCM.pid(), JCM.year());

    assertCountingLevelAndXMark(softly, response, JCM);
  }
}
