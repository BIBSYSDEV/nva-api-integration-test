package no.sikt.nva.apitest.base;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.BeforeAll;

public abstract class IntegrationTestBase {

  private static final ReentrantLock CONFIGURATION_LOCK = new ReentrantLock();
  private static boolean restAssuredConfigured;

  // Every test class inherits this @BeforeAll, but it only mutates global RestAssured state to the
  // same values. With test classes running concurrently, re-running it would race the config and
  // filter reassignment against in-flight requests from other classes, so it runs exactly once.
  @BeforeAll
  static void configureRestAssured() {
    CONFIGURATION_LOCK.lock();
    try {
      if (!restAssuredConfigured) {
        RestAssured.baseURI =
            "https://" + CognitoLogin.getValueFromParameterStore("/NVA/ApiDomain");
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
    } finally {
      CONFIGURATION_LOCK.unlock();
    }
  }
}
