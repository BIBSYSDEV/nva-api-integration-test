package no.sikt.nva.apitest.search.resources.bibtex;

import java.util.List;
import java.util.UUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import org.junit.jupiter.params.provider.MethodSource;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.parsing.Parser;
import no.sikt.Category;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.Category.ACADEMIC_MONOGRAPH;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_MONTH_SHORT_NAME;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import no.sikt.nva.apitest.search.BibTexExpectation;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH;
import no.sikt.nva.apitest.search.SearchTestBase;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
@ExtendWith(SoftAssertionsExtension.class)
class BibTexTest extends SearchTestBase {

  @InjectSoftAssertions private SoftAssertions softly;

  private static Stream<Arguments> publicationsInBibTexFormatProvider() {
    return Stream.of(
        argumentSet("AcademicArticle", ACADEMIC_ARTICLE, EXPECTED_BIBTEX_ACADEMIC_ARTICLE),
        argumentSet("AcademicMonograph", ACADEMIC_MONOGRAPH, EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH));
  }

  @ParameterizedTest
  @MethodSource("publicationsInBibTexFormatProvider")
  @DisplayName("Search with content type 'text/x-bibtex' produces BibTeX export")
  void shouldReturnPublicationsInBibTexFormat(Category category, BibTexExpectation expectation) {

    RestAssured.registerParser("text/x-bibtex", Parser.TEXT);

    var titleUuid = UUID.randomUUID().toString();

    var title = "BibTex Integration test publication " + titleUuid;
    var identifier =
        PUBLICATION_FACTORY.createPublishedPublication(
            UIB_CREATOR, title, category, List.of(UIB_CREATOR), UIB_PUBLISHING_CURATOR);

    with()
        .pollInterval(fibonacci().with().unit(SECONDS))
        .await()
        .atMost(12, SECONDS)
        .until(() -> !getResponseBody(titleUuid).isEmpty());

    var responseBody = getResponseBody(titleUuid);
    var allExpectations = buildAllExpectations(expectation, title, identifier);

    allExpectations.forEach(expected -> softly.assertThat(responseBody).contains(expected));
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
                "year = {" + CURRENT_YEAR + "}"))
        .toList();
  }
}
