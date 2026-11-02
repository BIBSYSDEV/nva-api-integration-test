package no.sikt.nva.apitest.approvals;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.CONTEXT_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.CURRENT_CONTEXT_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.ONTOLOGY_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.absolute;
import static no.sikt.nva.apitest.approvals.Approvals.CONTENT_LOCATION_HEADER;
import static no.sikt.nva.apitest.approvals.Approvals.DETAIL_FIELD;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + CONTEXT_PATH)
class FetchContextTest extends IntegrationTestBase {

  private static final String VOCABULARY_FORMAT = "%s%s#";
  private static final String JSON_LD_MEDIA_TYPE = "application/ld+json";
  private static final String NODE_REFERENCE_TYPE = "@id";
  private static final String SET_CONTAINER = "@set";
  private static final String UNKNOWN_VERSION = "/v0";
  private static final String VERSION_NOT_FOUND_MESSAGE = "Version not found";

  /**
   * The context defines the terms an approval document is interpreted with, so a consumer that
   * resolves it expects the vocabulary, the identifier set and the two URI-valued terms to stay
   * put. The vocabulary is the ontology served by the same environment, so it follows the host.
   */
  @Test
  @DisplayName("Get JSON-LD context")
  @Description
  void shouldReturnJsonLdContext(SoftAssertions softly) {
    assertApprovalTerms(softly, fetchContext(CONTEXT_PATH));
  }

  /** JSON-LD clients ask for the context as {@code application/ld+json} rather than plain JSON. */
  @Test
  @DisplayName("Get JSON-LD context as application/ld+json")
  @Description
  void shouldReturnJsonLdContextAsJsonLdMediaType() {
    given()
        .accept(JSON_LD_MEDIA_TYPE)
        .get(CONTEXT_PATH)
        .then()
        .statusCode(HTTP_OK)
        .contentType(JSON_LD_MEDIA_TYPE);
  }

  /**
   * The unversioned context is the current version, and says which one it is, so a consumer can pin
   * the version it resolved.
   */
  @Test
  @DisplayName("Get JSON-LD context points to its version")
  @Description
  void shouldPointToCurrentContextVersion() {
    givenUnauthenticatedJsonRequest()
        .get(CONTEXT_PATH)
        .then()
        .statusCode(HTTP_OK)
        .header(CONTENT_LOCATION_HEADER, absolute(CURRENT_CONTEXT_PATH));
  }

  /**
   * Approvals reference the versioned context, so that is the one which must resolve, with the same
   * terms as the current one.
   */
  @Test
  @DisplayName("Get versioned JSON-LD context")
  @Description
  void shouldReturnVersionedJsonLdContext(SoftAssertions softly) {
    assertApprovalTerms(softly, fetchContext(CURRENT_CONTEXT_PATH));
  }

  /**
   * A version that was never published is not found rather than silently the current one, and it is
   * the context endpoint that says so rather than a missing route.
   */
  @Test
  @DisplayName("Get JSON-LD context of unknown version")
  @Description
  void shouldReturnNotFoundForUnknownContextVersion() {
    var problem =
        givenUnauthenticatedJsonRequest()
            .get(CONTEXT_PATH + UNKNOWN_VERSION)
            .then()
            .statusCode(HTTP_NOT_FOUND)
            .extract()
            .jsonPath();

    assertThat(problem.getString(DETAIL_FIELD)).contains(VERSION_NOT_FOUND_MESSAGE);
  }

  private static JsonPath fetchContext(String path) {
    return givenUnauthenticatedJsonRequest()
        .get(path)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath();
  }

  private static void assertApprovalTerms(SoftAssertions softly, JsonPath context) {
    softly.assertThat(context.getString("@context.@vocab")).isEqualTo(approvalVocabulary());
    softly
        .assertThat(context.getString("@context.identifiers.@container"))
        .isEqualTo(SET_CONTAINER);
    softly.assertThat(context.getString("@context.source.@type")).isEqualTo(NODE_REFERENCE_TYPE);
    softly.assertThat(context.getString("@context.handle.@type")).isEqualTo(NODE_REFERENCE_TYPE);
  }

  private static String approvalVocabulary() {
    return VOCABULARY_FORMAT.formatted(RestAssured.baseURI, ONTOLOGY_PATH);
  }
}
