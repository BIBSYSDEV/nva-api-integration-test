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

  /** The X-channel fixture (Journal of Clinical Medicine) has level data in prod only. */
  public boolean hasXChannelLevelData() {
    return xChannelLevelDataAvailable;
  }
}
