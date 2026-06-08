package no.sikt.nva.apitest.search.resources.bibtex;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.Category.ACADEMIC_CHAPTER;
import static no.sikt.Category.ACADEMIC_MONOGRAPH;
import static no.sikt.Category.DEGREE_PHD;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_MONTH_SHORT_NAME;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_THESIS_CURATOR;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_CHAPTER;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_DEGREE_PHD;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_REPORT_RESEARCH;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import no.sikt.Category;
import no.sikt.nva.apitest.search.BibTexExpectation;
import no.sikt.nva.apitest.search.SearchTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
class BibTexTest extends SearchTestBase {

  @InjectSoftAssertions private SoftAssertions softly;

  private static Stream<Arguments> publicationsInBibTexFormatProvider() {
    return Stream.of(
        argumentSet("AcademicArticle", ACADEMIC_ARTICLE, EXPECTED_BIBTEX_ACADEMIC_ARTICLE),
        argumentSet("AcademicMonograph", ACADEMIC_MONOGRAPH, EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH),
        argumentSet("AcademicChapter", ACADEMIC_CHAPTER, EXPECTED_BIBTEX_ACADEMIC_CHAPTER),
        argumentSet("DegreePhD", DEGREE_PHD, EXPECTED_BIBTEX_DEGREE_PHD),
        argumentSet("ReportResearch", Category.RESEARCH_REPORT, EXPECTED_BIBTEX_REPORT_RESEARCH));
  }

  private String createTestPublication(Category category, String title) {
    var identifier = "";
    switch (category) {
      case ACADEMIC_CHAPTER -> {
        var anthologyUuid = UUID.randomUUID().toString();
        var anthologyTitle = "BibTex integration test anthology " + anthologyUuid;
        var anthologyIdentifier =
            PUBLICATION_FACTORY.createAnthologyForChapter(
                UIB_CREATOR, anthologyTitle, UIB_PUBLISHING_CURATOR, List.of(UIB_CREATOR));
        identifier =
            PUBLICATION_FACTORY.createChapterInAnthology(
                UIB_CREATOR,
                title,
                category,
                List.of(UIB_CREATOR),
                UIB_PUBLISHING_CURATOR,
                anthologyIdentifier,
                List.of(UIB_CREATOR));
      }
      case DEGREE_PHD ->
          identifier =
              PUBLICATION_FACTORY.createPublishedPublication(
                  UIB_THESIS_CURATOR, title, category, List.of(UIB_CREATOR), UIB_THESIS_CURATOR);
      default ->
          identifier =
              PUBLICATION_FACTORY.createPublishedPublication(
                  UIB_CREATOR, title, category, List.of(UIB_CREATOR), UIB_PUBLISHING_CURATOR);
    }

    return identifier;
  }

  @ParameterizedTest
  @MethodSource("publicationsInBibTexFormatProvider")
  @DisplayName("Search with content type 'text/x-bibtex' produces BibTeX export")
  void shouldReturnPublicationsInBibTexFormat(Category category, BibTexExpectation expectation) {

    RestAssured.registerParser("text/x-bibtex", Parser.TEXT);

    var titleUuid = UUID.randomUUID().toString();
    var title = "BibTex Integration test publication " + titleUuid;

    var identifier = createTestPublication(category, title);

    with()
        .pollInterval(fibonacci().with().unit(SECONDS))
        .await()
        .atMost(20, SECONDS)
        .until(() -> !getResponseBody(titleUuid).isEmpty());

    var responseBody = getResponseBody(titleUuid);
    var allExpectations = buildAllExpectations(expectation, title, identifier);

    allExpectations.forEach(expected -> softly.assertThat(responseBody).contains(expected));

    softly.assertAll();
  }

  private String getResponseBody(String titleUuid) {
    return given()
        .param("query", titleUuid)
        .accept("text/x-bibtex")
        .when()
        .get("/search/resources")
        .then()
        .log()
        .all()
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
