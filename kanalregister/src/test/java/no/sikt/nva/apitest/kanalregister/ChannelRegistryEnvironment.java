package no.sikt.nva.apitest.kanalregister;

/** The two Kanalregister environments; every test runs identically against both. */
public enum ChannelRegistryEnvironment {
  PROD("https://kr-nva.dataporten-api.no/nva-api", true),
  KAR_TEST("https://kar-test.dataporten-api.no/nva-api", false);

  private final String apiHost;
  private final boolean xChannelLevelDataAvailable;

  ChannelRegistryEnvironment(String apiHost, boolean xChannelLevelDataAvailable) {
    this.apiHost = apiHost;
    this.xChannelLevelDataAvailable = xChannelLevelDataAvailable;
  }

  public String getApiHost() {
    return apiHost;
  }

  /**
   * TODO: As of 2026-07-20, there is no known channel in the test environment with "Level X", so we
   * only test that property in the production environment. We should find/add a channel in the test
   * environment with the same properties so that we can test it there too.
   */
  public boolean hasXChannelLevelData() {
    return xChannelLevelDataAvailable;
  }
}
