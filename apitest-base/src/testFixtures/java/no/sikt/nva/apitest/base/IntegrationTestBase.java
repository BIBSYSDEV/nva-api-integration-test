package no.sikt.nva.apitest.base;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;

public class IntegrationTestBase {

  @BeforeAll
  static void configureRestAssured() {
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
  }
}
