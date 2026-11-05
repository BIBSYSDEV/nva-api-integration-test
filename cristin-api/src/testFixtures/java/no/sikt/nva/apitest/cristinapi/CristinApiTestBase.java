package no.sikt.nva.apitest.cristinapi;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;

import io.restassured.RestAssured;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

public class CristinApiTestBase extends IntegrationTestBase {

  @BeforeAll
  static void initCristinApiTests() {
    RestAssured.baseURI = "https://api.cristin-test.uio.no/v2/";
  }

  public String readResourceFileAsString(String filename) {
    return stringFromResources(Path.of(filename));
  }
}
