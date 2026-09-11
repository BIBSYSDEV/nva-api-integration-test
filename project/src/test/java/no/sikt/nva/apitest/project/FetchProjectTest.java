package no.sikt.nva.apitest.project;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.project.ProjectFactory.PROJECT_PATH;

@ExtendWith(SoftAssertionsExtension.class)
public class FetchProjectTest extends ProjectTestBase {


  /** Fetch project by identifier returns project metadata and status {@code 200 Ok} */
  @Test
  @DisplayName("Fetch project")
  @Description(useJavaDoc = true)
  void shouldFetchProject(SoftAssertions softly) {

    var projectTitle = "Cristin API test project " + UUID.randomUUID();
    var projectIdentifier = PROJECT_FACTORY.createProject(UIB_CREATOR, projectTitle).projectIdentifier();

    var jsonPath = givenUnauthenticatedJsonRequest()
    .when()
    .get(PROJECT_PATH, projectIdentifier)
    .then()
    .statusCode(HTTP_OK)
    .extract()
    .jsonPath();

    softly.assertThat(jsonPath.getString("title")).isEqualTo(projectTitle);

  }

  /** Fetch non-existing project  status {@code 404 Not Found} */
  @Test
  @DisplayName("Fetch non existing project returns Not Found")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenFetchingNonExistingProject() {

    var projectIdentifier = 1234567890;

    givenUnauthenticatedJsonRequest()
    .when()
    .get(PROJECT_PATH, projectIdentifier)
    .then()
    .statusCode(HTTP_NOT_FOUND);
  }
}
