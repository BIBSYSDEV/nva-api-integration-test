package no.sikt.nva.apitest.project;

import java.util.List;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.getCurrentDate;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;

@ExtendWith(SoftAssertionsExtension.class)
class CreateProjectTest extends ProjectTestBase {

  private static final String TYPE = "type";

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
            .log()
            .all()
            .when()
            .post(ProjectFactory.PROJECT_PATH)
            .then()
            .log()
            .all()
            .statusCode(201)
            .extract()
            .jsonPath();

    assertProjectResponse(softly, projectTitle, jsonPath);
  }

  private void assertProjectResponse(SoftAssertions softly, String projectTitle, JsonPath jsonPath) {
    var cristinId = List.of(jsonPath.getString("id").split("/")).getLast();

    softly.assertThat(jsonPath.getString("id")).isNotEmpty();
    softly.assertThat(jsonPath.getString(TYPE)).isEqualTo("Project");
    softly.assertThat(jsonPath.getList("identifiers")).isNotEmpty();
    softly.assertThat(jsonPath.getString("identifiers[0].type")).isEqualTo("CristinIdentifier");
    softly.assertThat(jsonPath.getString("identifiers[0].value")).isEqualTo(cristinId);
    softly.assertThat(jsonPath.getString("title")).isEqualTo(projectTitle);
    softly.assertThat(jsonPath.getString("language")).isEqualTo("http://lexvo.org/id/iso639-3/nob");
    softly.assertThat(jsonPath.getString("startDate")).startsWith(getCurrentDate().toString());
    softly.assertThat(jsonPath.getString("endDate")).startsWith(getCurrentDate().plusYears(1).toString());
    softly.assertThat(jsonPath.getString("funding")).isNotEmpty();
    softly.assertThat(jsonPath.getString("coordinatingInstitution")).isNotEmpty();
    jsonPath.setRootPath("coordinatingInstitution");
    softly.assertThat(jsonPath.getString(TYPE)).isEqualTo("Organization");
    softly.assertThat(jsonPath.getString("id")).isEqualTo("%s/cristin/organization/%s".formatted(RestAssured.baseURI, UIB.getCristinId()));
    softly.assertThat(jsonPath.getMap("labels")).isNotEmpty();
    softly.assertThat(jsonPath.getString("labels.en")).isEqualTo("University of Bergen");
    jsonPath.setRootPath("contributors");
    softly.assertThat(jsonPath.getList("")).isNotEmpty();
    softly.assertThat(jsonPath.getMap("[0].identity")).isNotEmpty();
    softly.assertThat(jsonPath.getString("[0].identity." + TYPE)).isEqualTo("Person");

  }
}
