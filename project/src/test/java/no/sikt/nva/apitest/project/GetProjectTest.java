package no.sikt.nva.apitest.project;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.project.ProjectFactory.PROJECT_PATH;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class GetProjectTest extends ProjectTestBase {

  /** Get project by identifier returns project metadata and status {@code 200 Ok} */
  @Test
  @DisplayName("Fetch project")
  @Description(useJavaDoc = true)
  void shouldGetProject(SoftAssertions softly) {

    var projectTitle = "Cristin API test project " + UUID.randomUUID();
    var projectIdentifier =
        PROJECT_FACTORY.createProject(UIB_CREATOR, projectTitle).projectIdentifier();

    var jsonPath =
        givenUnauthenticatedJsonRequest()
            .when()
            .get(PROJECT_PATH, projectIdentifier)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(jsonPath.getString("title")).isEqualTo(projectTitle);
  }

  /** Get non-existing project status {@code 404 Not Found} */
  @Test
  @DisplayName("Fetch non existing project returns Not Found")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenFetchingNonExistingProject() {

    var logConfig = LogConfig.logConfig().blacklistHeaders(List.of("Authorization"));
    RestAssured.config = RestAssured.config().logConfig(logConfig);

    var projectIdentifier = 123_456;

    givenUnauthenticatedJsonRequest()
        .when()
        .get(PROJECT_PATH, projectIdentifier)
        .then()
        .log()
        .all()
        .statusCode(HTTP_NOT_FOUND);
  }
}
