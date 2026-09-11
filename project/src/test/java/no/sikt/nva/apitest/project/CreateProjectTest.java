package no.sikt.nva.apitest.project;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_DATE;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.getCurrentDate;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.project.ProjectFactory.BASE_PROJECT_PATH;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class CreateProjectTest extends ProjectTestBase {

  private static final String TYPE = "type";

  /** Create project returns project metadata and {@code 201 Created} */
  @Test
  @DisplayName("Create new project")
  @Description(useJavaDoc = true)
  void shouldCreateProject(SoftAssertions softly) {

    var projectTitle = "Cristin API test project " + UUID.randomUUID();
    var coordinatingInstitution = UIB;
    var contributors = List.of(ProjectContributor.asProjectManager(UIB_CREATOR));
    var payload =
        ProjectFactory.createProjectPayload(projectTitle, coordinatingInstitution, contributors);

    JsonPath jsonPath =
        givenAuthenticatedJsonRequestAsUser(UIB_CREATOR)
            .body(payload)
            .when()
            .post(BASE_PROJECT_PATH)
            .then()
            .statusCode(HTTP_CREATED)
            .extract()
            .jsonPath();

    assertProjectResponse(softly, projectTitle, jsonPath);
  }

  /** Create project when unauthenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Create new project")
  @Description(useJavaDoc = true)
  void shouldRetunUnauthorizedWhenUnauthenticated() {
    givenUnauthenticatedJsonRequest()
        .when()
        .post(BASE_PROJECT_PATH)
        .then()
        .statusCode(HTTP_UNAUTHORIZED);
  }

  private void assertProjectResponse(
      SoftAssertions softly, String projectTitle, JsonPath jsonPath) {
    var cristinId = List.of(jsonPath.getString("id").split("/")).getLast();

    assertMetaData(softly, projectTitle, jsonPath, cristinId);

    assertCoordinatingInstitution(softly, jsonPath);

    assertContributors(softly, jsonPath);

    jsonPath.setRootPath("");
    softly.assertThat(jsonPath.getString("status")).isEqualTo("ACTIVE");

    assertCreatedDates(softly, jsonPath);

    assertCreator(softly, jsonPath);
  }

  private void assertMetaData(
      SoftAssertions softly, String projectTitle, JsonPath jsonPath, String cristinId) {
    softly.assertThat(jsonPath.getString("id")).as("id exist").isNotEmpty();
    softly.assertThat(jsonPath.getString(TYPE)).as("type is 'Project'").isEqualTo("Project");
    softly.assertThat(jsonPath.getList("identifiers")).as("identifier is not empty").isNotEmpty();
    softly
        .assertThat(jsonPath.getString("identifiers[0].type"))
        .as("identifier.type is 'CristinIdentifier'")
        .isEqualTo("CristinIdentifier");
    softly
        .assertThat(jsonPath.getString("identifiers[0].value"))
        .as("identifier.value is %s", cristinId)
        .isEqualTo(cristinId);
    softly
        .assertThat(jsonPath.getString("title"))
        .as("title is %s", projectTitle)
        .isEqualTo(projectTitle);
    softly
        .assertThat(jsonPath.getString("language"))
        .as("language is 'nob'")
        .isEqualTo("http://lexvo.org/id/iso639-3/nob");
    softly
        .assertThat(jsonPath.getString("startDate"))
        .as("startDate is %s", CURRENT_DATE)
        .startsWith(CURRENT_DATE);
    softly
        .assertThat(jsonPath.getString("endDate"))
        .as("endDate is %s", getCurrentDate().plusYears(1).toString())
        .startsWith(getCurrentDate().plusYears(1).toString());
    softly.assertThat(jsonPath.getList("funding")).as("funding is an emptly list").isEmpty();
  }

  private void assertCreator(SoftAssertions softly, JsonPath jsonPath) {
    jsonPath.setRootPath("creator");
    softly.assertThat(jsonPath.getMap("")).as("creator is not empty").isNotEmpty();
    softly.assertThat(jsonPath.getMap("identity")).as("creator.identity is not empty").isNotEmpty();
    jsonPath.setRootPath("creator.identity");
    softly
        .assertThat(jsonPath.getString(TYPE))
        .as("creator.identity.type is 'Person'")
        .isEqualTo("Person");
    softly
        .assertThat(jsonPath.getString("id"))
        .as("creator.identity.id is %s", UIB_CREATOR.cristinUri())
        .isEqualTo(UIB_CREATOR.cristinUri());
    softly
        .assertThat(jsonPath.getString("firstName"))
        .as("creator.identity.firstName is %s", UIB_CREATOR.firstName())
        .isEqualTo(UIB_CREATOR.firstName());
    softly
        .assertThat(jsonPath.getString("lastName"))
        .as("creator.identity.lastName is %s", UIB_CREATOR.lastName())
        .isEqualTo(UIB_CREATOR.lastName());
    jsonPath.setRootPath("creator.roles");
    softly.assertThat(jsonPath.getList("")).as("creator.roles is not empty").isNotEmpty();
    jsonPath.setRootPath("creator.roles[0]");
    softly
        .assertThat(jsonPath.getString(TYPE))
        .as("creator.roles.type is 'ProjectCreator'")
        .isEqualTo("ProjectCreator");
    softly
        .assertThat(jsonPath.getMap("affiliation"))
        .as("creator.roles.affiliation is not empty")
        .isNotEmpty();
    jsonPath.setRootPath("creator.roles[0].affiliation");
    softly
        .assertThat(jsonPath.getString(TYPE))
        .as("creator.roles.affiliation.type is 'Organization'")
        .isEqualTo("Organization");
    String affiliation =
        "%s/cristin/organization/%s".formatted(RestAssured.baseURI, UIB.getCristinId());
    softly
        .assertThat(jsonPath.getString("id"))
        .as("creator.roles.affiliation.id is %s", affiliation)
        .isEqualTo(affiliation);
  }

  private void assertCreatedDates(SoftAssertions softly, JsonPath jsonPath) {
    softly.assertThat(jsonPath.getMap("created")).as("created is not empty").isNotEmpty();
    softly
        .assertThat(jsonPath.getString("created.sourceShortName"))
        .as("created.shortName is 'NVA'")
        .isEqualTo("NVA");
    softly
        .assertThat(jsonPath.getString("created.date"))
        .as("create.date is %s", CURRENT_DATE)
        .startsWith(CURRENT_DATE);

    softly.assertThat(jsonPath.getMap("lastModified")).as("lastModified is not empty").isNotEmpty();
    softly
        .assertThat(jsonPath.getString("lastModified.sourceShortName"))
        .as("lastModified.shortName is 'NVA'")
        .isEqualTo("NVA");
    softly
        .assertThat(jsonPath.getString("lastModified.date"))
        .as("lastMoified.date is %s", CURRENT_DATE)
        .startsWith(CURRENT_DATE);
  }

  private void assertContributors(SoftAssertions softly, JsonPath jsonPath) {
    jsonPath.setRootPath("contributors");
    softly.assertThat(jsonPath.getList("")).as("contributors is not empty").isNotEmpty();
    softly
        .assertThat(jsonPath.getMap("[0].identity"))
        .as("contributors.identity is not empty")
        .isNotEmpty();
    jsonPath.setRootPath("contributors[0].identity");
    softly
        .assertThat(jsonPath.getString(TYPE))
        .as("contributors.identity.type is 'Person'")
        .isEqualTo("Person");
    softly
        .assertThat(jsonPath.getString("id"))
        .as("contributors.identity.id is %s", UIB_CREATOR.cristinUri())
        .isEqualTo(UIB_CREATOR.cristinUri());
    softly
        .assertThat(jsonPath.getString("firstName"))
        .as("contributors.identity.firstName is %s", UIB_CREATOR.firstName())
        .isEqualTo(UIB_CREATOR.firstName());
    softly
        .assertThat(jsonPath.getString("lastName"))
        .as("contributors.identity.lastName is %s", UIB_CREATOR.lastName())
        .isEqualTo(UIB_CREATOR.lastName());
    jsonPath.setRootPath("contributors[0].roles");
    softly.assertThat(jsonPath.getList("")).as("contributors.roles is not empty").isNotEmpty();
    jsonPath.setRootPath("contributors[0].roles[0]");
    softly
        .assertThat(jsonPath.getString(TYPE))
        .as("contributors.roles.type is ProjectManager")
        .isEqualTo("ProjectManager");
    softly
        .assertThat(jsonPath.getMap("affiliation"))
        .as("contributors.roles.affiliation is not empty")
        .isNotEmpty();
    jsonPath.setRootPath("contributors[0].roles[0].affiliation");
    softly
        .assertThat(jsonPath.getString(TYPE))
        .as("contributors.roles.affiliation.type is 'Organization'")
        .isEqualTo("Organization");
    softly
        .assertThat(jsonPath.getString("id"))
        .as("contributors.roles.affiliation.id is %s", UIB.getValue())
        .isEqualTo(UIB.getValue());
    softly
        .assertThat(jsonPath.getString("labels.en"))
        .as("contributors.roles.affiliation.labels.en is 'University of Bergen'")
        .isEqualTo("University of Bergen");
  }

  private void assertCoordinatingInstitution(SoftAssertions softly, JsonPath jsonPath) {
    jsonPath.setRootPath("coordinatingInstitution");
    softly
        .assertThat(jsonPath.getString(""))
        .as("coordinatingInstitution is not empty")
        .isNotEmpty();
    softly
        .assertThat(jsonPath.getString(TYPE))
        .as("coordinatingInstitution.type is 'Organization")
        .isEqualTo("Organization");
    softly
        .assertThat(jsonPath.getString("id"))
        .as("coordinatingInstitution.id is %s", UIB.getValue())
        .isEqualTo(UIB.getValue());
    softly
        .assertThat(jsonPath.getString("labels.en"))
        .as("coordinatingInstitution.labels.en is 'University of Bergen'")
        .isEqualTo("University of Bergen");
  }
}
