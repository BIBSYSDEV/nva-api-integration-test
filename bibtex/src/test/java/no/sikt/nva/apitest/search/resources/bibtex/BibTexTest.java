package no.sikt.nva.apitest.search.resources.bibtex;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.Category.ACADEMIC_CHAPTER;
import static no.sikt.Category.ACADEMIC_MONOGRAPH;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_MONTH_SHORT_NAME;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_CHAPTER;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class BibTexTest extends SearchTestBase {

  private static Stream<Arguments> publicationsInBibTexFormatProvider() {
    return Stream.of(
        argumentSet("AcademicArticle", ACADEMIC_ARTICLE, EXPECTED_BIBTEX_ACADEMIC_ARTICLE),
        argumentSet("AcademicMonograph", ACADEMIC_MONOGRAPH, EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH),
        argumentSet("AcademicChapter", ACADEMIC_CHAPTER, EXPECTED_BIBTEX_ACADEMIC_CHAPTER));
  }

  @ParameterizedTest
  @MethodSource("publicationsInBibTexFormatProvider")
  @DisplayName("Search with content type 'text/x-bibtex' produces BibTeX export")
  void shouldReturnPublicationsInBibTexFormat(Category category, BibTexExpectation expectation) {

    RestAssured.registerParser("text/x-bibtex", Parser.TEXT);

    var titleUuid = UUID.randomUUID().toString();

    var title = "BibTex Integration test publication " + titleUuid;
    var anthologyIdentifier =
        category == ACADEMIC_CHAPTER
            ? PUBLICATION_FACTORY.createAnthologyForChapter(
                UIB_CREATOR, title, UIB_PUBLISHING_CURATOR, List.of(UIB_CREATOR))
            : "";
    var identifier =
        category == ACADEMIC_CHAPTER
            ? PUBLICATION_FACTORY.createChapterInAnthology(
                UIB_CREATOR,
                title,
                category,
                List.of(UIB_CREATOR),
                UIB_PUBLISHING_CURATOR,
                anthologyIdentifier,
                List.of(UIB_CREATOR))
            : PUBLICATION_FACTORY.createPublishedPublication(
                UIB_CREATOR, title, category, List.of(UIB_CREATOR), UIB_PUBLISHING_CURATOR);

    with()
        .pollInterval(fibonacci().with().unit(SECONDS))
        .await()
        .atMost(12, SECONDS)
        .until(() -> !getResponseBody(titleUuid).isEmpty());

    var responseBody = getResponseBody(titleUuid);
    var allExpectations = buildAllExpectations(expectation, title, identifier);

    // if (category == ACADEMIC_CHAPTER) {
    //   allExpectations =
    //       Stream.concat(
    //               allExpectations.stream(),
    //               buildAllExpectations(
    //                   EXPECTED_BIBTEX_BOOK_ANTHOLOGY, "Anthology for " + title,
    // anthologyIdentifier)
    //                   .stream())
    //           .toList();
    // }

    // given()
    // .param("query", titleUuid)
    // .accept("text/x-bibtex")
    // .when()
    // .get("/search/resources")
    // .then()
    // .statusCode(200)
    // .contentType("text/x-bibtex")
    // .body("testing", Matchers.notNullValue());

    allExpectations.forEach(expected -> assertTrue(responseBody.contains(expected)));
  }

  private String getResponseBody(String titleUuid) {
    return given()
        .param("query", titleUuid)
        .accept("text/x-bibtex")
        .when()
        .get("/search/resources")
        .then()
        .statusCode(200)
        .contentType("text/x-bibtex")
        .extract()
        .asString();
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
                "year = {" + CURRENT_YEAR + "}",
                "}"))
        .toList();
  }
}
