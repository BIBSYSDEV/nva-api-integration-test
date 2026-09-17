package no.sikt.nva.apitest.project;

import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.project.ProjectFactory.PROJECT_PATH;

import io.qameta.allure.Description;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class UpdateProjectTest extends ProjectTestBase {

  @Test
  @DisplayName("Create new project")
  @Description(useJavaDoc = true)
  void shouldReturnUpdatedProject(SoftAssertions softly) {

    var projectTitle = "Cristin API test project " + UUID.randomUUID();
    var project = PROJECT_FACTORY.createProject(UIB_CREATOR, projectTitle);

    var identifier = project.projectIdentifier();
    var payload = project.payload();
    payload.put("title", projectTitle);

    givenAuthenticatedRequestAsUser(UIB_CREATOR)
        .body(payload)
        .when()
        .patch(PROJECT_PATH, identifier)
        .then()
        .statusCode(204)
        .extract()
        .jsonPath();

    var jsonPathGet =
        givenAuthenticatedRequestAsUser(UIB_CREATOR)
            .when()
            .get(PROJECT_PATH, identifier)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(jsonPathGet.getString("title")).isEqualTo(projectTitle);
  }
}
