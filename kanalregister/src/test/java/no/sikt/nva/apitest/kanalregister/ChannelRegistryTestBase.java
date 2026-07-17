package no.sikt.nva.apitest.kanalregister;

import io.qameta.allure.Allure;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import java.util.concurrent.locks.ReentrantLock;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Runs each subclass once per {@link ChannelRegistryEnvironment} and configures RestAssured once.
 * Unlike apitest-base's IntegrationTestBase it sets no baseURI from SSM: this module uses absolute
 * URLs and can run without AWS credentials.
 */
@ParameterizedClass(name = "{0}")
@EnumSource(ChannelRegistryEnvironment.class)
@ExtendWith(SoftAssertionsExtension.class)
public abstract class ChannelRegistryTestBase {

  // The upstream API can be slow (5-10 s per search); a timeout is itself signal, so no retries.
  private static final int REQUEST_TIMEOUT_MILLIS = 30_000;

  private static final ReentrantLock CONFIGURATION_LOCK = new ReentrantLock();
  private static boolean restAssuredConfigured;

  @Parameter protected ChannelRegistryEnvironment environment;

  @BeforeAll
  protected static void configureRestAssured() {
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

  // The parent suite groups all module suites under one node in the report; the environment
  // parameter keeps separate report history per environment invocation.
  @BeforeEach
  protected void labelAllureResult() {
    Allure.label("parentSuite", "Kanalregister");
    Allure.parameter("environment", environment);
  }
}
