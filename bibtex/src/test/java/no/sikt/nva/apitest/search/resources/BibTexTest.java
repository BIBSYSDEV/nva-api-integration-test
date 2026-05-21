package no.sikt.nva.apitest.search.resources;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.parsing.Parser;
import no.sikt.Category;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.Category.ACADEMIC_MONOGRAPH;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import no.sikt.nva.apitest.search.SearchTestBase;

class BibTexTest extends SearchTestBase {

  private static final String YEAR = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
  private static final String MONTH_SHORT_NAME =
      Month.of(Calendar.getInstance().get(Calendar.MONTH) + 1)
          .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
          .toLowerCase(Locale.ENGLISH);

  private static final Map<Category, BibTexExpectation> BIBTEX_EXPECTATIONS =
      new EnumMap<>(Category.class);

  private record BibTexExpectation(String bibtexType, List<String> expectations) {}

  static {
    BIBTEX_EXPECTATIONS.put(
        ACADEMIC_ARTICLE,
        new BibTexExpectation(
            "article",
            List.of(
                "journal = {ACM Journal of Data and Information Quality}",
                "issn = {1936-1963}",
                "note = {nva type: AcademicArticle}",
                "number = {1}",
                "pages = {10--20}",
                "volume = {3}")));

    BIBTEX_EXPECTATIONS.put(
        ACADEMIC_MONOGRAPH,
        new BibTexExpectation(
            "book",
            List.of(
                "isbn = {9783161484100}",
                "note = {nva type: AcademicMonograph}",
                "pages = {150}",
                "publisher = {Springer Nature}")));
  }

  @ParameterizedTest
  @EnumSource(
      value = Category.class,
      names = {"ACADEMIC_ARTICLE", "ACADEMIC_MONOGRAPH"})
  void shouldReturnPublicationsInBibTexFormat(Category category) {

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

      if (!responseBody.isEmpty()) {
        indexed = true;

        var expectation = BIBTEX_EXPECTATIONS.get(category);
        var allExpectations = Stream.concat(
                expectation.expectations().stream(),
                Stream.of(
                    "@" + expectation.bibtexType() + "{" + identifier,
                    "url = {https://api.e2e.nva.aws.unit.no/publication/" + identifier + "}",
                    "title = {" + title + "}",
                    "month = {" + MONTH_SHORT_NAME + "}",
                    "year = {" + YEAR + "}"))
            .toList();

        allExpectations.forEach(expected -> assertTrue(responseBody.contains(expected)));
      } else {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) { // NOPMD - intentionally ignored, wait for search index
        }
      }
    }

    assertTrue(indexed, "Publication was not indexed after 3 attempts");
  }
}
