package no.sikt.nva.apitest.kanalregister.findpublisher;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelDisplayMatchesLevel;
import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelForYear;
import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelHistoryIncludesYear;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.GYLDENDAL;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.lookUp;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import no.sikt.nva.apitest.kanalregister.SharedResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findpublisher/{pid}/{year}")
class FindPublisherByPidAndYearTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findpublisher";

  private static final SharedResponse GYLDENDAL_LOOKUP =
      new SharedResponse(env -> lookUp(env, RESOURCE, GYLDENDAL.pid(), GYLDENDAL.year()));

  @Test
  @DisplayName("Lookup returns level for the requested year")
  @Description("A lookup returns the level for the requested year, like search does")
  void shouldReturnLevelForRequestedYear(SoftAssertions softly) {
    assertLevelForYear(softly, GYLDENDAL_LOOKUP.forEnvironment(environment), GYLDENDAL);
  }

  @Test
  @DisplayName("Lookup exposes levelDisplay alongside level")
  @Description("A lookup exposes levelDisplay, without which X-channels cannot be distinguished")
  void shouldExposeLevelDisplay(SoftAssertions softly) {
    assertLevelDisplayMatchesLevel(softly, GYLDENDAL_LOOKUP.forEnvironment(environment), GYLDENDAL);
  }

  @Test
  @DisplayName("Level history includes the requested year")
  @Description("A lookup's levelHistories includes the requested year")
  @Issue("NP-51482")
  void shouldIncludeRequestedYearInLevelHistory(SoftAssertions softly) {
    assertLevelHistoryIncludesYear(softly, GYLDENDAL_LOOKUP.forEnvironment(environment), GYLDENDAL);
  }
}
