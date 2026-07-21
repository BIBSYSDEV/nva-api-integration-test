package no.sikt.nva.apitest.kanalregister.operational;

import static io.restassured.RestAssured.given;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.kanalregister.ChannelRegistryTestBase;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET /health, /checkdatabaseconnection and /")
class OperationalEndpointsTest extends ChannelRegistryTestBase {

  /** GET /health responds with a success status. */
  @Test
  @DisplayName("Health endpoint responds successfully")
  @Description(useJavaDoc = true)
  void shouldRespondSuccessfullyOnHealthEndpoint(SoftAssertions softly) {
    var statusCode = given().get(environment.getApiHost() + "/health").statusCode();

    softly.assertThat(statusCode).as("/health status code").isBetween(200, 299);
  }

  /** GET /checkdatabaseconnection returns status {@code 200 OK} and confirms the connection. */
  @Test
  @DisplayName("Database connection check responds")
  @Description(useJavaDoc = true)
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

  /** GET / returns status {@code 200 OK} and identifies the service. */
  @Test
  @DisplayName("API root responds with service information")
  @Description(useJavaDoc = true)
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
