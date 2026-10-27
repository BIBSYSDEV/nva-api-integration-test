package no.sikt.nva.apitest.cristinapi;

import org.junit.jupiter.api.BeforeAll;

import io.restassured.RestAssured;
import no.sikt.nva.apitest.base.IntegrationTestBase;

public class CristinApiTestBase extends IntegrationTestBase{

  @BeforeAll 
  static void initCristinApiTests() {
    RestAssured.baseURI = "https://api.cristin-test.uio.no/v2/";
  }

  
}
