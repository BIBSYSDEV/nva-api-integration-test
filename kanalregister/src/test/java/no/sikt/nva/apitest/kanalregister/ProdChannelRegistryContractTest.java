package no.sikt.nva.apitest.kanalregister;

import org.junit.jupiter.api.DisplayName;

@DisplayName("Kanalregister prod (kr-nva.dataporten-api.no)")
@SuppressWarnings("PMD.TestClassWithoutTestCases") // Inherits all tests from the abstract class
class ProdChannelRegistryContractTest extends ChannelRegistryContractTest {

  private static final String PROD_HOST = "https://kr-nva.dataporten-api.no/nva-api";
  private static final boolean X_CHANNEL_FIXTURE_HAS_LEVEL_DATA = true;

  ProdChannelRegistryContractTest() {
    super(PROD_HOST, X_CHANNEL_FIXTURE_HAS_LEVEL_DATA);
  }
}
