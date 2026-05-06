package no.sikt;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;

class IntegrationTestBase {

  @BeforeAll
  static void configureRestAssured() {
    new PublicationFactory().setBaseUriFromParameterStore();
    RestAssured.replaceFiltersWith(
        new AllureRestAssured()
            .setRequestTemplate("sanitized-http-request.ftl")
            .setResponseTemplate("sanitized-http-response.ftl"));
    var logConfig =
        LogConfig.logConfig()
            .enableLoggingOfRequestAndResponseIfValidationFails()
            .blacklistHeaders(List.of("Authorization"));
    RestAssured.config = RestAssured.config().logConfig(logConfig);
  }
}
