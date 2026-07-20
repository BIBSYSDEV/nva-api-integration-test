package no.sikt.nva.apitest.kanalregister.findjournal;

import static no.sikt.nva.apitest.kanalregister.ChannelAssertions.assertLevelIsForCurrentYearOrAbsent;
import static no.sikt.nva.apitest.kanalregister.ChannelFixtures.ACP;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.lookUpWithoutYear;
import static no.sikt.nva.apitest.kanalregister.ChannelSchemas.assertMatchesChannelSchema;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import no.sikt.nva.apitest.kanalregister.SharedResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /findjournal/{pid}")
class FindJournalByPidTest extends ChannelRegistryTestBase {

  private static final String RESOURCE = "findjournal";

  private static final SharedResponse ACP_LOOKUP =
      new SharedResponse(env -> lookUpWithoutYear(env, RESOURCE, ACP.pid()));

  /** A lookup without year returns the current year's level or null. */
  @Test
  @DisplayName("Lookup without year does not return the highest-year level")
  @Description(useJavaDoc = true)
  void shouldNotReturnHighestYearLevel(SoftAssertions softly) {
    assertLevelIsForCurrentYearOrAbsent(softly, ACP_LOOKUP.jsonPathForEnvironment(environment));
  }

  /** The response body matches the shared channel JSON Schema. */
  @Test
  @DisplayName("Response matches the channel contract")
  @Description(useJavaDoc = true)
  void shouldMatchChannelContract(SoftAssertions softly) {
    assertMatchesChannelSchema(softly, ACP_LOOKUP.bodyForEnvironment(environment));
  }
}
