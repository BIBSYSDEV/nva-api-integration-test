package no.sikt.nva.apitest.approvals;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.ONTOLOGY_PATH;

import io.qameta.allure.Description;
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
  private static final String VOCABULARY_PREFIX = "@prefix : <https://nva.unit.no/approval#> .";
  private static final String APPROVAL_CLASS = ":Approval a rdfs:Class";
  private static final String IDENTIFIER_CLASS = ":Identifier a rdfs:Class";

  /**
   * The ontology describes the two classes an approval document is built from, and is served as
   * Turtle rather than JSON.
   */
  @Test
  @DisplayName("Get RDF ontology")
  @Description(useJavaDoc = true)
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

    softly.assertThat(ontology).contains(VOCABULARY_PREFIX);
    softly.assertThat(ontology).contains(APPROVAL_CLASS);
    softly.assertThat(ontology).contains(IDENTIFIER_CLASS);
  }
}
