package no.sikt.nva.apitest.scientificindex;

import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.CONTEXT_PATH;

import io.qameta.allure.Description;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + CONTEXT_PATH)
class FetchContextTest extends ScientificIndexTestBase {

  /**
   * Fetching the JSON-LD context returns a non-empty map with {@code @context} as the root node.
   */
  @Test
  @DisplayName("Get JSON-LD context")
  @Description(useJavaDoc = true)
  void shouldReturnJsonLdContext(SoftAssertions softly) {
    var response =
        givenUnauthenticatedJsonRequest()
            .get(CONTEXT_PATH)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();
    softly.assertThat(response.getMap("@context")).isNotEmpty();
  }
}
