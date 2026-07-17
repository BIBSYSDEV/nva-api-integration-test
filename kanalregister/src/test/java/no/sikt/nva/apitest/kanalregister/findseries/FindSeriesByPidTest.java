package no.sikt.nva.apitest.kanalregister.findseries;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelIsForCurrentYearOrAbsent;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.LNCS;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.lookUpWithoutYear;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findseries/{pid}")
class FindSeriesByPidTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findseries";

  @Test
  @DisplayName("Lookup without year does not return the highest-year level")
  @Description("A lookup without year returns the current year's level or null")
  void shouldNotReturnHighestYearLevel(SoftAssertions softly) {
    var response = lookUpWithoutYear(environment, RESOURCE, LNCS.pid());

    assertLevelIsForCurrentYearOrAbsent(softly, response);
  }
}
