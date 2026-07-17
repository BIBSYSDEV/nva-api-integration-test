package no.sikt.nva.apitest.kanalregister;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.BeforeAll;

/**
 * Configures RestAssured once for tests against Kanalregisteret's external hosts. Unlike
 * apitest-base's IntegrationTestBase it sets no baseURI from SSM: this module uses absolute URLs
 * and must run without AWS credentials.
 */
public abstract class KanalregisterTestBase {

  // The upstream API can be slow (5-10 s per search); a timeout is itself signal, so no retries.
  private static final int REQUEST_TIMEOUT_MILLIS = 30_000;

  private static final ReentrantLock CONFIGURATION_LOCK = new ReentrantLock();
  private static boolean restAssuredConfigured;

  @BeforeAll
  static void configureRestAssured() {
    CONFIGURATION_LOCK.lock();
    try {
      if (!restAssuredConfigured) {
        RestAssured.replaceFiltersWith(new AllureRestAssured());
        var logConfig = LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        var httpClientConfig =
            HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", REQUEST_TIMEOUT_MILLIS)
                .setParam("http.socket.timeout", REQUEST_TIMEOUT_MILLIS);
        RestAssured.config = RestAssured.config().logConfig(logConfig).httpClient(httpClientConfig);
        restAssuredConfigured = true;
      }
    } finally {
      CONFIGURATION_LOCK.unlock();
    }
  }
}
