package no.sikt.nva.apitest.cristinapi.project;

import static java.net.HttpURLConnection.HTTP_OK;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.qameta.allure.Description;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PROJECT_CATEGORIES_PATH;

@DisplayName("GET " + PROJECT_CATEGORIES_PATH)
class GetProjectCategoriesApiTest extends CristinApiTestBase {

  /** Fetch project categories returns a list of categories and status {@code 200 Ok} */
  @ParameterizedTest
  @ValueSource (strings = {"nb", "en"})
  @DisplayName("Fetch project categories returns a list of categories")
  @Description
  void shouldReturnListOfCategories(String languageCode) {
    var categoriesResponse = givenUnauthenticatedJsonRequest()
    .param("lang", languageCode)
    .when()
    .get(PROJECT_CATEGORIES_PATH)
    .then()
    .statusCode(HTTP_OK)
    .extract()
    .body()
    .asString();

    var expectedResponse = readResourceFileAsString("projects/categories_%s.json".formatted(languageCode));
    assertThatJson(categoriesResponse).when(IGNORING_ARRAY_ORDER, IGNORING_EXTRA_ARRAY_ITEMS).isEqualTo(expectedResponse);
  }
}
