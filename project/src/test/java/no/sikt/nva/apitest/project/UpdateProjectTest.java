package no.sikt.nva.apitest.project;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import static io.restassured.http.Method.PATCH;
import io.restassured.path.json.JsonPath;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import no.sikt.nva.apitest.base.User;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.project.ProjectFactory.PROJECT_PATH;

@ExtendWith(SoftAssertionsExtension.class)
class UpdateProjectTest extends ProjectTestBase {

  private static String testProjectIdentifier = "";

  @BeforeAll
  static void setUp() {
    var projectTitle = "Cristin API test project " + UUID.randomUUID();
    var project = PROJECT_FACTORY.createProject(UIB_CREATOR, projectTitle);

    testProjectIdentifier = project.projectIdentifier();
  }

  @Test
  @DisplayName("Update project")
  @Description(useJavaDoc = true)
  void shouldReturnUpdatedProject(SoftAssertions softly) {

    var projectTitle = "Cristin API test project " + UUID.randomUUID();
    var project = PROJECT_FACTORY.createProject(UIB_CREATOR, projectTitle);

    softly.assertThat(project.payload().get("title")).isEqualTo(projectTitle);

    var identifier = project.projectIdentifier();
    var payload = project.payload();
    payload.put("title", projectTitle);

    givenAuthenticatedRequestAsUser(UIB_CREATOR)
        .body(payload)
        .when()
        .patch(PROJECT_PATH, identifier)
        .then()
        .statusCode(HTTP_NO_CONTENT)
        .extract()
        .jsonPath();

    var jsonPathGet = getProject(identifier);

    softly.assertThat(jsonPathGet.getString("title")).isEqualTo(projectTitle);
  }

  private JsonPath getProject(String identifier) {
    var jsonPathGet =
        givenUnauthenticatedJsonRequest()
            .when()
            .get(PROJECT_PATH, identifier)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();
    return jsonPathGet;
  }

  @Test
  @DisplayName("Unauthorized user returns Unauthorized when updating project")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenNotAuthenticatet(SoftAssertions softly) {
    requestShouldReturnUnauthorized(PATCH, PROJECT_PATH, testProjectIdentifier);
  }

  @Test
  @DisplayName("Update returns Forbidden when user is not owner or project manager")
  @Description(useJavaDoc = true)
  void shouldReturnForbiddenWhenNotOwnerOrProjectManager() {
    requestShouldReturnForbidden(PATCH, UIB_CONTRIBUTOR, PROJECT_PATH, testProjectIdentifier);
  }

  @Test
  @DisplayName("Update returns Forbidden when user is not owner or project manager")
  @Description(useJavaDoc = true)
  void shouldUpdateProjectWhenProjectManager(SoftAssertions softly) {
    var project = PROJECT_FACTORY.createProject(UIB_CREATOR, PROJECT_PATH);
    var projectManager = UIB_CONTRIBUTOR;

    var contributors = createProjectManagerPayload(projectManager);
    project.payload().put("contributors", contributors);

    givenAuthenticatedRequestAsUser(UIB_CREATOR)
    .body(project.payload())
    .when()
    .patch(PROJECT_PATH, project.projectIdentifier())
    .then()
    .statusCode(HTTP_NO_CONTENT);

    var jsonPathGet = getProject(project.projectIdentifier());
    String identifier = List.of(jsonPathGet.getString("id").split("/")).getLast();

    var contributor = jsonPathGet.getList("contributors").getLast();
    var identity = (Map<String, String>) ((Map<String, Object>)contributor).get("identity");
    softly.assertThat(identity.get("id")).isEqualTo(UIB_CONTRIBUTOR.cristinUri());

    var payload = jsonPathGet.getMap("");
    String updatedTitle = "Updated API test project" + UUID.randomUUID();
    payload.put("title", updatedTitle);
    givenAuthenticatedRequestAsUser(UIB_CONTRIBUTOR)
    .body(payload)
    .when()
    .patch(PROJECT_PATH, identifier)
    .then()
    .statusCode(HTTP_NO_CONTENT)
    .extract()
    .jsonPath();

    var jsonPath = getProject(identifier);

    softly.assertThat(jsonPath.getString("title")).isEqualTo(updatedTitle);
  }

  private List<Map<String, Object>> createProjectManagerPayload(User projectManager) {
    var projectManagerPayload = Map.of(
      "identity", Map.of(
        "type", "Person",
        "id", projectManager.cristinUri()
      ),
      "roles", List.of(
        Map.of(
          "type", "ProjectManager",
          "affiliation", Map.of(
            "type", "Organization",
            "id", UIB.getValue()
          )
        )
      )
    );

    var updatedContributors = List.of(projectManagerPayload);
    return updatedContributors;
  }
}
