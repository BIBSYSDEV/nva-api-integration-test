package no.sikt.nva.apitest.cristinapi;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.restassured.RestAssured;
import java.io.IOException;
import java.io.UncheckedIOException;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;

public class CristinApiTestBase extends IntegrationTestBase {

  @BeforeAll
  static void initCristinApiTests() {
    RestAssured.baseURI = "https://api.cristin-test.uio.no/v2/";
  }

  public String readResourceFileAsString(String filename) {
    try (var resourceStream = CristinApiTestBase.class.getResourceAsStream("/" + filename)) {
      return new String(resourceStream.readAllBytes(), UTF_8);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
