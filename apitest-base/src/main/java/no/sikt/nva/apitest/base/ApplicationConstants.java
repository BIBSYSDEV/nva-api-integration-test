package no.sikt.nva.apitest.base;

import nva.commons.core.Environment;
import software.amazon.awssdk.regions.Region;

public final class ApplicationConstants {

  private ApplicationConstants() {}

  public static Region getRegion(Environment environment) {
    return environment.readEnvOpt("AWS_REGION").map(Region::of).orElse(Region.EU_WEST_1);
  }
}
