package no.sikt.nva.apitest.cristinapi;

import io.restassured.RestAssured;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;

public class CristinApiTestBase extends IntegrationTestBase {

  @BeforeAll
  static void initCristinApiTests() {
    RestAssured.baseURI = "https://api.cristin-test.uio.no/v2/";
  }
}
