package no.sikt.nva.apitest.approvals;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.CURRENT_ONTOLOGY_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.ONTOLOGY_PATH;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.absolute;
import static no.sikt.nva.apitest.approvals.Approvals.CONTENT_LOCATION_HEADER;
import static no.sikt.nva.apitest.approvals.Approvals.DETAIL_FIELD;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + ONTOLOGY_PATH)
class FetchOntologyTest extends IntegrationTestBase {

  private static final String TURTLE_MEDIA_TYPE = "text/turtle";
  private static final String VOCABULARY_PREFIX_FORMAT = "@prefix : <%s%s#> .";
  private static final String APPROVAL_CLASS = ":Approval a rdfs:Class";
  private static final String IDENTIFIER_CLASS = ":Identifier a rdfs:Class";
  private static final String VERSION_IRI_FORMAT = "owl:versionIRI <%s%s>";
  private static final String UNKNOWN_VERSION = "/0.0.0";
  private static final String VERSION_NOT_FOUND_MESSAGE = "Version not found";

  /**
   * The ontology describes the two classes an approval document is built from, and is served as
   * Turtle rather than JSON.
   */
  @Test
  @DisplayName("Get RDF ontology")
  @Description
  void shouldReturnTurtleOntology(SoftAssertions softly) {
    var ontology =
        given()
            .accept(TURTLE_MEDIA_TYPE)
            .get(ONTOLOGY_PATH)
            .then()
            .statusCode(HTTP_OK)
            .contentType(TURTLE_MEDIA_TYPE)
            .extract()
            .asString();

    softly.assertThat(ontology).contains(vocabularyPrefix());
    softly.assertThat(ontology).contains(APPROVAL_CLASS);
    softly.assertThat(ontology).contains(IDENTIFIER_CLASS);
  }

  /** The latest ontology says which version it is, so a consumer can pin the one it resolved. */
  @Test
  @DisplayName("Get RDF ontology points to its version")
  @Description
  void shouldPointToCurrentOntologyVersion() {
    given()
        .accept(TURTLE_MEDIA_TYPE)
        .get(ONTOLOGY_PATH)
        .then()
        .statusCode(HTTP_OK)
        .header(CONTENT_LOCATION_HEADER, absolute(CURRENT_ONTOLOGY_PATH));
  }

  /**
   * A versioned ontology resolves on its own, and identifies itself by the version IRI that the
   * unversioned namespace points to.
   */
  @Test
  @DisplayName("Get versioned RDF ontology")
  @Description
  void shouldReturnVersionedTurtleOntology() {
    var ontology =
        given()
            .accept(TURTLE_MEDIA_TYPE)
            .get(CURRENT_ONTOLOGY_PATH)
            .then()
            .statusCode(HTTP_OK)
            .contentType(TURTLE_MEDIA_TYPE)
            .extract()
            .asString();

    assertThat(ontology).contains(versionIri());
  }

  /**
   * A version that was never published is not found rather than silently the latest one, and it is
   * the ontology endpoint that says so rather than a missing route.
   */
  @Test
  @DisplayName("Get RDF ontology of unknown version")
  @Description
  void shouldReturnNotFoundForUnknownOntologyVersion() {
    var problem =
        given()
            .accept(TURTLE_MEDIA_TYPE)
            .get(ONTOLOGY_PATH + UNKNOWN_VERSION)
            .then()
            .statusCode(HTTP_NOT_FOUND)
            .extract()
            .jsonPath();

    assertThat(problem.getString(DETAIL_FIELD)).contains(VERSION_NOT_FOUND_MESSAGE);
  }

  private static String vocabularyPrefix() {
    return VOCABULARY_PREFIX_FORMAT.formatted(RestAssured.baseURI, ONTOLOGY_PATH);
  }

  private static String versionIri() {
    return VERSION_IRI_FORMAT.formatted(RestAssured.baseURI, CURRENT_ONTOLOGY_PATH);
  }
}
