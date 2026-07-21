package no.sikt.nva.apitest.kanalregister.findjournal;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelDisplayMatchesLevel;
import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelForYear;
import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelHistoryIncludesYear;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.ACP;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.lookUp;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import no.sikt.nva.apitest.kanalregister.SharedResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findjournal/{pid}/{year}")
class FindJournalByPidAndYearTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findjournal";

  private static final SharedResponse ACP_LOOKUP =
      new SharedResponse(env -> lookUp(env, RESOURCE, ACP.pid(), ACP.year()));

  /** A lookup returns the level for the requested year, like search does. */
  @Test
  @DisplayName("Lookup returns level for the requested year")
  @Description(useJavaDoc = true)
  @Issue("NP-51485")
  void shouldReturnLevelForRequestedYear(SoftAssertions softly) {
    assertLevelForYear(softly, ACP_LOOKUP.forEnvironment(environment), ACP);
  }

  /** A lookup exposes levelDisplay, without which X-channels cannot be distinguished. */
  @Test
  @DisplayName("Lookup exposes levelDisplay alongside level")
  @Description(useJavaDoc = true)
  @Issue("NP-51483")
  void shouldExposeLevelDisplay(SoftAssertions softly) {
    assertLevelDisplayMatchesLevel(softly, ACP_LOOKUP.forEnvironment(environment), ACP);
  }

  /** A lookup's levelHistories includes the requested year. */
  @Test
  @DisplayName("Level history includes the requested year")
  @Description(useJavaDoc = true)
  void shouldIncludeRequestedYearInLevelHistory(SoftAssertions softly) {
    assertLevelHistoryIncludesYear(softly, ACP_LOOKUP.forEnvironment(environment), ACP);
  }
}
