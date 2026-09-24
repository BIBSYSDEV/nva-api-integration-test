package no.sikt.nva.apitest.project;

import static io.restassured.http.Method.PATCH;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_DOI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_EDITOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_SUPPORT_CURATOR;
import static no.sikt.nva.apitest.project.ProjectFactory.PROJECT_PATH;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import no.sikt.nva.apitest.base.User;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
class UpdateProjectTest extends ProjectTestBase {

  private static String testProjectIdentifier = "";
  private static final String TITLE = "title";

  @BeforeAll
  static void setUp() {
    var projectTitle = "Cristin API test project " + UUID.randomUUID();
    var project = PROJECT_FACTORY.createProject(UIB_CREATOR, projectTitle);

    testProjectIdentifier = project.projectIdentifier();
  }

  /** Update project when owner returns {@code 204 No Content} */
  @Test
  @DisplayName("Update project")
  @Description(useJavaDoc = true)
  void shouldReturnUpdatedProject(SoftAssertions softly) {

    var projectTitle = "Cristin API test project " + UUID.randomUUID();
    var project = PROJECT_FACTORY.createProject(UIB_CREATOR, projectTitle);

    softly.assertThat(project.payload().get(TITLE)).isEqualTo(projectTitle);

    var identifier = project.projectIdentifier();
    var payload = project.payload();
    var updatedTitle = "Updated API test project " + UUID.randomUUID();
    payload.put(TITLE, projectTitle);

    givenAuthenticatedRequestAsUser(UIB_CREATOR)
        .body(payload)
        .when()
        .patch(PROJECT_PATH, identifier)
        .then()
        .statusCode(HTTP_NO_CONTENT)
        .extract()
        .jsonPath();

    var jsonPathGet = getProject(identifier);

    softly.assertThat(jsonPathGet.getString(TITLE)).isEqualTo(updatedTitle);
  }

  private JsonPath getProject(String identifier) {
    return givenUnauthenticatedJsonRequest()
        .when()
        .get(PROJECT_PATH, identifier)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath();
  }

  /** Update project when unauthenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Unauthenticated request returns Unauthorized when updating project")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenNotAuthenticated() {
    requestShouldReturnUnauthorized(PATCH, PROJECT_PATH, testProjectIdentifier);
  }

  /** Update project when not owner or project manager returns {@code 403 Forbidden} */
  @ParameterizedTest
  @MethodSource("userByRoleProvider")
  @DisplayName("Update returns Forbidden when user is not owner or project manager")
  @Description(useJavaDoc = true)
  void shouldReturnForbiddenWhenNotOwnerOrProjectManager(User user) {
    requestShouldReturnForbidden(PATCH, user, PROJECT_PATH, testProjectIdentifier);
  }

  private static Stream<Arguments> userByRoleProvider() {
    return Stream.of(
        argumentSet("Registrar", UIB_CONTRIBUTOR),
        argumentSet("Nvi-curator", UIB_NVI_CURATOR),
        argumentSet("DOI-curator", UIB_DOI_CURATOR),
        argumentSet("Publishing-curator", UIB_PUBLISHING_CURATOR),
        argumentSet("Support curator", UIB_SUPPORT_CURATOR),
        argumentSet("Editor", UIB_EDITOR));
  }

  /** Update project when not owner but project manager returns {@code 204 No Content} */
  @Test
  @DisplayName("Update project when project manager")
  @Description(useJavaDoc = true)
  void shouldUpdateProjectWhenProjectManager(SoftAssertions softly) {
    var projectTitle = "API test project " + UUID.randomUUID();
    var project = PROJECT_FACTORY.createProject(UIB_CREATOR, projectTitle);
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

    var contributor = jsonPathGet.getList("contributors").getLast();
    var identity = (Map<String, String>) ((Map<String, Object>) contributor).get("identity");
    softly.assertThat(identity.get("id")).isEqualTo(projectManager.cristinUri());

    var payload = jsonPathGet.getMap("");
    var updatedTitle = "Updated API test project" + UUID.randomUUID();
    payload.put(TITLE, updatedTitle);
    givenAuthenticatedRequestAsUser(projectManager)
        .body(payload)
        .when()
        .patch(PROJECT_PATH, project.projectIdentifier())
        .then()
        .statusCode(HTTP_NO_CONTENT);

    var jsonPath = getProject(project.projectIdentifier());

    softly.assertThat(jsonPath.getString(TITLE)).isEqualTo(updatedTitle);
  }

  private List<Map<String, Object>> createProjectManagerPayload(User projectManager) {
    var projectManagerPayload =
        Map.of(
            "identity", Map.of("type", "Person", "id", projectManager.cristinUri()),
            "roles",
                List.of(
                    Map.of(
                        "type",
                        "ProjectManager",
                        "affiliation",
                        Map.of("type", "Organization", "id", UIB.getValue()))));

    return List.of(projectManagerPayload);
  }
}
