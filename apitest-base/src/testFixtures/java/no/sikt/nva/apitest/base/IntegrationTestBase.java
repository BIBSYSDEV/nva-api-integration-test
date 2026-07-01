package no.sikt.nva.apitest.base;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;

public class IntegrationTestBase {

  private static boolean restAssuredConfigured = false;

  // Every test class inherits this @BeforeAll, but it only mutates global RestAssured state to the
  // same values. With test classes running concurrently, re-running it would race the config and
  // filter reassignment against in-flight requests from other classes, so it runs exactly once.
  @BeforeAll
  static synchronized void configureRestAssured() {
    if (!restAssuredConfigured) {
      RestAssured.baseURI = "https://" + CognitoLogin.getValueFromParameterStore("/NVA/ApiDomain");
      RestAssured.replaceFiltersWith(
          new AllureRestAssured()
              .setRequestTemplate("sanitized-http-request.ftl")
              .setResponseTemplate("sanitized-http-response.ftl"));
      var logConfig =
          LogConfig.logConfig()
              .enableLoggingOfRequestAndResponseIfValidationFails()
              .blacklistHeaders(List.of("Authorization"));
      RestAssured.config = RestAssured.config().logConfig(logConfig);
      restAssuredConfigured = true;
    }
  }
}
