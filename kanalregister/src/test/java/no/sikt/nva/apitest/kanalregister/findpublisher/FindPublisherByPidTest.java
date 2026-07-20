package no.sikt.nva.apitest.kanalregister.findpublisher;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelIsForCurrentYearOrAbsent;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.GYLDENDAL;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.lookUpWithoutYear;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findpublisher/{pid}")
class FindPublisherByPidTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findpublisher";

  /** A lookup without year returns the current year's level or null. */
  @Test
  @DisplayName("Lookup without year does not return the highest-year level")
  @Description(useJavaDoc = true)
  void shouldNotReturnHighestYearLevel(SoftAssertions softly) {
    var response = lookUpWithoutYear(environment, RESOURCE, GYLDENDAL.pid());

    assertLevelIsForCurrentYearOrAbsent(softly, response);
  }
}
