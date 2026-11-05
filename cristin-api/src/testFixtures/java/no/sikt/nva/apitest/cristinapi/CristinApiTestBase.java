package no.sikt.nva.apitest.cristinapi;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import io.restassured.RestAssured;
import java.nio.file.Path;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;

public class CristinApiTestBase extends IntegrationTestBase {

  @BeforeAll
  static void initCristinApiTests() {
    RestAssured.baseURI = "https://api.cristin-test.uio.no/v2/";
  }

  public String readResourceFileAsString(String filename) {
    return stringFromResources(Path.of(filename));
  }
}
