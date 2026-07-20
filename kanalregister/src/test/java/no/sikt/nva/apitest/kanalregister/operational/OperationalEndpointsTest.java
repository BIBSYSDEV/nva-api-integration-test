package no.sikt.nva.apitest.kanalregister.operational;

import static io.restassured.RestAssured.given;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /health, /checkdatabaseconnection and /")
class OperationalEndpointsTest extends ChannelRegistryTestBase {

  @Test
  @DisplayName("Health endpoint responds successfully")
  @Description("GET /health responds with a success status")
  void shouldRespondSuccessfullyOnHealthEndpoint(SoftAssertions softly) {
    var statusCode = given().get(environment.getApiHost() + "/health").statusCode();

    softly.assertThat(statusCode).as("/health status code").isBetween(200, 299);
  }

  @Test
  @DisplayName("Database connection check responds")
  @Description("GET /checkdatabaseconnection returns 200 and confirms the connection")
  void shouldConfirmDatabaseConnection(SoftAssertions softly) {
    var body =
        given()
            .get(environment.getApiHost() + "/checkdatabaseconnection")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();

    softly.assertThat(body).startsWith("Connected to the database");
  }

  @Test
  @DisplayName("API root responds with service information")
  @Description("GET / returns 200 and identifies the service")
  void shouldRespondWithServiceInformationOnApiRoot(SoftAssertions softly) {
    var body =
        given()
            .get(environment.getApiHost() + "/")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();

    softly.assertThat(body).contains("Kanalregister NVA api");
  }
}
