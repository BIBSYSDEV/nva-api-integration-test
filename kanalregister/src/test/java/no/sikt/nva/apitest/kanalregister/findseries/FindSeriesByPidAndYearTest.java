package no.sikt.nva.apitest.kanalregister.findseries;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelDisplayMatchesLevel;
import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelForYear;
import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelHistoryIncludesYear;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.LNCS;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.lookUp;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import no.sikt.nva.apitest.kanalregister.SharedResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findseries/{pid}/{year}")
class FindSeriesByPidAndYearTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findseries";

  private static final SharedResponse LNCS_LOOKUP =
      new SharedResponse(env -> lookUp(env, RESOURCE, LNCS.pid(), LNCS.year()));

  @Test
  @DisplayName("Lookup returns level for the requested year")
  @Description("A lookup returns the level for the requested year, like search does")
  void shouldReturnLevelForRequestedYear(SoftAssertions softly) {
    assertLevelForYear(softly, LNCS_LOOKUP.forEnvironment(environment), LNCS);
  }

  @Test
  @DisplayName("Lookup exposes levelDisplay alongside level")
  @Description("A lookup exposes levelDisplay, without which X-channels cannot be distinguished")
  void shouldExposeLevelDisplay(SoftAssertions softly) {
    assertLevelDisplayMatchesLevel(softly, LNCS_LOOKUP.forEnvironment(environment), LNCS);
  }

  @Test
  @DisplayName("Level history includes the requested year")
  @Description("A lookup's levelHistories includes the requested year")
  void shouldIncludeRequestedYearInLevelHistory(SoftAssertions softly) {
    assertLevelHistoryIncludesYear(softly, LNCS_LOOKUP.forEnvironment(environment), LNCS);
  }
}
