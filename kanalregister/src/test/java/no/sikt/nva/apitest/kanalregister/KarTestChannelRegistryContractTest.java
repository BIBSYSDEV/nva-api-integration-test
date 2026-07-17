package no.sikt.nva.apitest.kanalregister;

import org.junit.jupiter.api.DisplayName;

@DisplayName("Kanalregister test environment (kar-test.dataporten-api.no)")
@SuppressWarnings("PMD.TestClassWithoutTestCases") // Inherits all tests from the abstract class
class KarTestChannelRegistryContractTest extends ChannelRegistryContractTest {

  private static final String KAR_TEST_HOST = "https://kar-test.dataporten-api.no/nva-api";
  private static final boolean X_CHANNEL_FIXTURE_HAS_LEVEL_DATA = false;

  KarTestChannelRegistryContractTest() {
    super(KAR_TEST_HOST, X_CHANNEL_FIXTURE_HAS_LEVEL_DATA);
  }
}
