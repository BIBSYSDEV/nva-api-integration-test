package no.sikt.nva.apitest.project;

import java.util.List;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;

@ExtendWith(SoftAssertionsExtension.class)
class UpdateProjectTest extends ProjectTestBase {

  @Test
  @DisplayName("Create new project")
  @Description(useJavaDoc = true)
  void shouldReturnUpdatedProject(SoftAssertions softly) {

    
    
    var project = PROJECT_FACTORY.createProject(UIB_CREATOR);
    
    var identifier = List.of(project.projectIdentifier().split("/")).getLast();
    var payload = project.payload();
    var projectTitle = "Cristin API test project " + UUID.randomUUID();
    payload.put("title", projectTitle);


    var jsonPathUpdate = givenAuthenticatedRequestAsUser(UIB_CREATOR)
    .body(payload)
    .when()
    .patch("/cristin/project/{identifier}", identifier)
    .then()
    .statusCode(204)
    .extract()
    .jsonPath();

    var jsonPathGet = givenAuthenticatedRequestAsUser(UIB_CREATOR)
    .when()
    .get("/cristin/project/{identifier}", identifier)
    .then()
    .statusCode(200)
    .extract()
    .jsonPath();
    
    softly.assertThat(jsonPathGet.getString("title")).isEqualTo(projectTitle);
  }
}
