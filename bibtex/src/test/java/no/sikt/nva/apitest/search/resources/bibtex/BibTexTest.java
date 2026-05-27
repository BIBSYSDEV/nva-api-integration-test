package no.sikt.nva.apitest.search.resources.bibtex;

import static io.restassured.RestAssured.given;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.Category.ACADEMIC_MONOGRAPH;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_MONTH_SHORT_NAME;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import no.sikt.Category;
import no.sikt.nva.apitest.search.BibTexExpectation;
import no.sikt.nva.apitest.search.SearchTestBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BibTexTest extends SearchTestBase {

  private static Stream<Arguments> publicationsInBibTexFormatProvider() {
    return Stream.of(
        argumentSet("AcademicArticle", ACADEMIC_ARTICLE, EXPECTED_BIBTEX_ACADEMIC_ARTICLE),
        argumentSet("AcademicMonograph", ACADEMIC_MONOGRAPH, EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH));
  }

  @ParameterizedTest
  @MethodSource("publicationsInBibTexFormatProvider")
  void shouldReturnPublicationsInBibTexFormat(Category category, BibTexExpectation expectation) {

    RestAssured.registerParser("text/x-bibtex", Parser.TEXT);

    var titleUuid = UUID.randomUUID().toString();

    var title = "BibTex Integration test publication " + titleUuid;
    var identifier =
        PUBLICATION_FACTORY.createPublishedPublication(
            UIB_CREATOR, title, category, List.of(UIB_CREATOR), UIB_PUBLISHING_CURATOR);

    var indexed = false;
    var count = 0;

    while (!indexed && count < 3) {
      count++;

      var responseBody =
          given()
              .param("query", titleUuid)
              .accept("text/x-bibtex")
              .when()
              .get("/search/resources")
              .then()
              .statusCode(200)
              .contentType("text/x-bibtex")
              .extract()
              .asString();

      if (responseBody.isEmpty()) {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) { // NOPMD - intentionally ignored, wait for search index
        }
      } else {
        indexed = true;

        var allExpectations = buildAllExpectations(expectation, title, identifier);

        allExpectations.forEach(expected -> assertTrue(responseBody.contains(expected)));
      }
    }

    assertTrue(indexed, "Publication was not indexed after 3 attempts");
  }

  private List<String> buildAllExpectations(
      BibTexExpectation expectation, String title, String identifier) {
    return Stream.concat(
            expectation.expectations().stream(),
            Stream.of(
                "@" + expectation.bibtexType() + "{" + identifier,
                "url = {" + RestAssured.baseURI + "/publication/" + identifier + "}",
                "title = {" + title + "}",
                "month = {" + CURRENT_MONTH_SHORT_NAME + "}",
                "year = {" + CURRENT_YEAR + "}"))
        .toList();
  }
}
