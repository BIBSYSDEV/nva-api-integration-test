package no.sikt.nva.apitest.kanalregister;

import static java.util.Objects.nonNull;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Label;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import java.util.concurrent.locks.ReentrantLock;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

  /**
   * Rebuilds the report hierarchy as Kanalregister > environment > endpoint, replacing the suite
   * label allure-junit5 derives from the class. The environment parameter keeps separate report
   * history per environment invocation.
   */
  @BeforeEach
  protected void labelAllureResult() {
    var endpoint = endpointDisplayName();
    Allure.getLifecycle()
        .updateTestCase(
            result -> {
              result.getLabels().removeIf(label -> "suite".equals(label.getName()));
              result.getLabels().add(label("parentSuite", "Kanalregister"));
              result.getLabels().add(label("suite", environment.name()));
              result.getLabels().add(label("subSuite", endpoint));
            });
    Allure.parameter("environment", environment);
  }

  private String endpointDisplayName() {
    var displayName = getClass().getAnnotation(DisplayName.class);
    return nonNull(displayName) ? displayName.value() : getClass().getSimpleName();
  }

  private static Label label(String name, String value) {
    return new Label().setName(name).setValue(value);
  }
}
