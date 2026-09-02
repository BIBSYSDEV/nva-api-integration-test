package no.sikt.nva.apitest.approvals;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.CONTEXT_PATH;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + CONTEXT_PATH)
class FetchContextTest extends IntegrationTestBase {

  private static final String APPROVAL_VOCABULARY = "https://nva.unit.no/approval#";
  private static final String JSON_LD_MEDIA_TYPE = "application/ld+json";
  private static final String NODE_REFERENCE_TYPE = "@id";
  private static final String SET_CONTAINER = "@set";

  /**
   * The context defines the terms an approval document is interpreted with, so a consumer that
   * resolves it expects the vocabulary, the identifier set and the two URI-valued terms to stay
   * put.
   */
  @Test
  @DisplayName("Get JSON-LD context")
  @Description(useJavaDoc = true)
  void shouldReturnJsonLdContext(SoftAssertions softly) {
    var context =
        givenUnauthenticatedJsonRequest()
            .get(CONTEXT_PATH)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(context.getString("@context.@vocab")).isEqualTo(APPROVAL_VOCABULARY);
    softly
        .assertThat(context.getString("@context.identifiers.@container"))
        .isEqualTo(SET_CONTAINER);
    softly.assertThat(context.getString("@context.source.@type")).isEqualTo(NODE_REFERENCE_TYPE);
    softly.assertThat(context.getString("@context.handle.@type")).isEqualTo(NODE_REFERENCE_TYPE);
  }

  /** JSON-LD clients ask for the context as {@code application/ld+json} rather than plain JSON. */
  @Test
  @DisplayName("Get JSON-LD context as application/ld+json")
  @Description(useJavaDoc = true)
  void shouldReturnJsonLdContextAsJsonLdMediaType() {
    given()
        .accept(JSON_LD_MEDIA_TYPE)
        .get(CONTEXT_PATH)
        .then()
        .statusCode(HTTP_OK)
        .contentType(JSON_LD_MEDIA_TYPE);
  }
}
