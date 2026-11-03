package no.sikt.nva.apitest.cristinapi;

import static java.net.HttpURLConnection.HTTP_OK;

import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinRequest;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Cristin project tests")
class ProjectApiTest extends CristinApiTestBase {

  private static final String PROJECT_PATH = "/projects/{id}";

  /** Fetch a Cristin project */
  @Test
  @DisplayName("Fetch a Cristin project")
  @Description
  void shouldReturnProject() {

    var projectIdentifier = "435010";

    givenAuthenticatedCristinRequest()
        .when()
        .get(PROJECT_PATH, projectIdentifier)
        .then()
        .statusCode(HTTP_OK);
    // TODO: check response contents
  }
}
