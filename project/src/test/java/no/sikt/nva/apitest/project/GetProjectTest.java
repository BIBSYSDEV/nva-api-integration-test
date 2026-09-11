package no.sikt.nva.apitest.project;

import static java.net.HttpURLConnection.HTTP_OK;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.project.ProjectFactory.BASE_PROJECT_PATH;

@ExtendWith(SoftAssertionsExtension.class)
public class GetProjectTest extends ProjectTestBase{

  /** Get projects returns list of projects and status {@code 200 Ok} */
  @Test
  @DisplayName("Get project")
  @Description(useJavaDoc = true)
  void shouldReturnListOfProjects(SoftAssertions softly){
    
    var projectTitle = "Cristin API test project " + UUID.randomUUID().toString();
    PROJECT_FACTORY.createProject(UIB_CREATOR, projectTitle);

    var jsonPath = givenUnauthenticatedJsonRequest()
    .when()
    .queryParam("query", "API")
    .get(BASE_PROJECT_PATH)
    .then()
    .statusCode(HTTP_OK)
    .extract()
    .jsonPath();

    softly.assertThat(jsonPath.getString("id")).isNotEmpty();
    softly.assertThat(jsonPath.getInt("size")).isGreaterThan(0);
    softly.assertThat(jsonPath.getString("searchString")).contains("title=API");
    softly.assertThat(jsonPath.getInt("firstRecord")).isEqualTo(1);
    softly.assertThat(jsonPath.getString("previousResults")).isNull();
    softly.assertThat(jsonPath.getList("hits")).hasSizeGreaterThan(0);
    softly.assertThat(jsonPath.getString("hits[0].type")).isEqualTo("Project");
  }

  /** Get next page returns next results and status {@code 200 Ok} */
  @Test
  @DisplayName("Get project")
  @Description(useJavaDoc = true)
  void shouldReturnNextPageOfProjects(SoftAssertions softly){
    var projectTitle = "Cristin API test project " + UUID.randomUUID().toString();
    PROJECT_FACTORY.createProject(UIB_CREATOR, projectTitle);

    var firstPageJsonPath = givenUnauthenticatedJsonRequest()
    .when()
    .queryParam("query", "API")
    .get(BASE_PROJECT_PATH)
    .then()
    .statusCode(HTTP_OK)
    .extract()
    .jsonPath();

    var nextPageQuery = firstPageJsonPath.getString("nextResults").replace(RestAssured.baseURI, "");

    var nextPageJsonPath = givenUnauthenticatedJsonRequest()
    .when()
    .get(nextPageQuery)
    .then()
    .statusCode(HTTP_OK)
    .extract()
    .jsonPath();

    softly.assertThat(nextPageJsonPath.getInt("firstRecord")).isEqualTo(6);
    softly.assertThat(nextPageJsonPath.getString("previousResults")).contains("page=1&title=API&results=5");
  }
}
