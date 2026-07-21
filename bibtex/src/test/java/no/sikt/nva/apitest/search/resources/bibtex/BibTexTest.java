package no.sikt.nva.apitest.search.resources.bibtex;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.Category.ACADEMIC_CHAPTER;
import static no.sikt.Category.ACADEMIC_MONOGRAPH;
import static no.sikt.Category.CONFERENCE_LECTURE;
import static no.sikt.Category.DEGREE_MASTER;
import static no.sikt.Category.DEGREE_PHD;
import static no.sikt.Category.RESEARCH_REPORT;
import static no.sikt.Role.CREATOR;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_MONTH_SHORT_NAME;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.ENTITY_DESCRIPTION_FIELD;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_CHAPTER;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_CONFERENCE_LECTURE;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_DEGREE_MASTER;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_DEGREE_PHD;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_REPORT_RESEARCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.sikt.Category;
import no.sikt.Contributor;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.publication.PublicationFields;
import no.sikt.nva.apitest.search.BibTexExpectation;
import no.sikt.nva.apitest.search.SearchTestBase;
import nva.commons.core.StringUtils;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
class BibTexTest extends SearchTestBase {

  private static final String TEXT_X_BIBTEX = "text/x-bibtex";

  private static Stream<Arguments> publicationsInBibTexFormatProvider() {
    return Stream.of(
        argumentSet("AcademicArticle", ACADEMIC_ARTICLE, EXPECTED_BIBTEX_ACADEMIC_ARTICLE),
        argumentSet("AcademicMonograph", ACADEMIC_MONOGRAPH, EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH),
        argumentSet("AcademicChapter", ACADEMIC_CHAPTER, EXPECTED_BIBTEX_ACADEMIC_CHAPTER),
        argumentSet("DegreeMaster", DEGREE_MASTER, EXPECTED_BIBTEX_DEGREE_MASTER),
        argumentSet("DegreePhD", DEGREE_PHD, EXPECTED_BIBTEX_DEGREE_PHD),
        argumentSet("ReportResearch", RESEARCH_REPORT, EXPECTED_BIBTEX_REPORT_RESEARCH),
        argumentSet("ConferenceLecture", CONFERENCE_LECTURE, EXPECTED_BIBTEX_CONFERENCE_LECTURE));
  }

  /** Test content returned with content type 'text/x-bibtex' is correct BibTex-format. */
  @ParameterizedTest
  @MethodSource("publicationsInBibTexFormatProvider")
  @DisplayName("Search with content type 'text/x-bibtex' produces BibTeX export")
  @Description(useJavaDoc = true)
  void shouldReturnPublicationsInBibTexFormat(
      Category category, BibTexExpectation expectation, SoftAssertions softly) {

    RestAssured.registerParser(TEXT_X_BIBTEX, Parser.TEXT);

    var titleUuid = UUID.randomUUID().toString();
    var title = "BibTex Integration test publication " + titleUuid;

    var identifier = PUBLICATION_FACTORY.createPublishedPublication(category, title);

    var responseBody = waitForIndexing(titleUuid);
    var allExpectations = buildAllExpectations(expectation, title, identifier);

    softly.assertThat(responseBody.lines()).hasSize(allExpectations.size());
    allExpectations.forEach(expected -> softly.assertThat(responseBody).contains(expected));

    softly
        .assertThat(responseBody.lines().filter(line -> !line.startsWith("@")).toList())
        .isSorted();
  }

  private String getResponseBody(String query) {
    return given()
        .param("query", query)
        .basePath("/search/resources")
        .accept(TEXT_X_BIBTEX)
        .when()
        .get()
        .then()
        .statusCode(200)
        .contentType(TEXT_X_BIBTEX)
        .extract()
        .asString();
  }

  /**
   * Polls the BibTeX search until the query has a hit. Search is eventually consistent and a hit
   * can flicker, so callers should assert on the returned snapshot instead of issuing a new
   * request.
   */
  private String waitForIndexing(String query) {
    return pollUntil(() -> getResponseBody(query), StringUtils::isNotBlank);
  }

  /**
   * Polls the shared query until every expected publication is indexed, since indexing is
   * asynchronous with no ordering guarantee.
   */
  private String awaitIndexedPublications(String query, int expectedCount) {
    return pollUntil(bibtexSearchRequest(query), hasNumberOfBibTexEntries(expectedCount));
  }

  private Callable<String> bibtexSearchRequest(String query) {
    return () -> getResponseBody(query);
  }

  private static Predicate<String> hasNumberOfBibTexEntries(int expectedCount) {
    return body -> body.lines().filter(line -> line.startsWith("@")).count() >= expectedCount;
  }

  /** Search returned with content type 'text/x-bibtex' is correct BibTex-format for customer. */
  @ParameterizedTest
  @MethodSource("publicationsInBibTexFormatProvider")
  @DisplayName("Search with content type 'text/x-bibtex' produces BibTeX export for customer")
  @Description(useJavaDoc = true)
  void shouldReturnPublicationsInBibTexFormatForCustomer(
      Category category, BibTexExpectation expectation, SoftAssertions softly) {

    RestAssured.registerParser(TEXT_X_BIBTEX, Parser.TEXT);

    var titleUuid = UUID.randomUUID().toString();
    var title = "BibTex Integration test publication " + titleUuid;

    var identifier = PUBLICATION_FACTORY.createPublishedPublication(category, title);

    waitForIndexing(titleUuid);

    var responseBody = getAuthorizedResponseBody(titleUuid, UIB_PUBLISHING_CURATOR);
    var allExpectations = buildAllExpectations(expectation, title, identifier);

    softly.assertThat(responseBody.lines()).hasSize(allExpectations.size());
    allExpectations.forEach(expected -> softly.assertThat(responseBody).contains(expected));

    softly
        .assertThat(responseBody.lines().filter(line -> !line.startsWith("@")).toList())
        .isSorted();
  }

  private String getAuthorizedResponseBody(String query, User user) {
    return givenAuthenticatedJsonRequestAsUser(user)
        .param("query", query)
        .basePath("/search/customer/resources")
        .accept(TEXT_X_BIBTEX)
        .when()
        .get()
        .then()
        .statusCode(200)
        .contentType(TEXT_X_BIBTEX)
        .extract()
        .asString();
  }

  private List<String> buildAllExpectations(
      BibTexExpectation expectation, String title, String identifier) {
    return Stream.concat(
            expectation.expectations().stream(),
            Stream.of(
                "@" + expectation.bibtexType() + "{" + identifier,
                "author = {" + UIB_CREATOR.name() + "}",
                "url = {" + baseURI + "/publication/" + identifier + "}",
                "title = {" + title + "}",
                "month = {" + CURRENT_MONTH_SHORT_NAME + "}",
                "year = {" + CURRENT_YEAR + "}",
                "nva_api = {" + baseURI + "/publication/" + identifier,
                "}"))
        .toList();
  }

  /**
   * A list of publications should contain a type that starts with '@' and two newlines between
   * publications.
   */
  @Test
  @DisplayName("Search for multiple publications, return text/x-bibtex format")
  @Description(useJavaDoc = true)
  void shouldReturnListOfPublicationsInBibTexFormat(SoftAssertions softly) {

    var commonUuid = UUID.randomUUID().toString();
    var titleRoot = "BibTex-test-publication";
    var categories =
        List.of(
            ACADEMIC_ARTICLE,
            ACADEMIC_MONOGRAPH,
            ACADEMIC_CHAPTER,
            CONFERENCE_LECTURE,
            DEGREE_MASTER,
            DEGREE_PHD);

    IntStream.range(0, categories.size())
        .forEach(
            i ->
                PUBLICATION_FACTORY.createPublishedPublication(
                    categories.get(i), titleRoot + i + " " + commonUuid + " " + UUID.randomUUID()));

    var responseBody = awaitIndexedPublications(commonUuid, categories.size());

    softly
        .assertThat(responseBody.lines().filter(line -> line.startsWith("@")))
        .hasSize(categories.size());
    // Publications are separated by two newlines, which is a single blank line per gap once split
    // into lines, so N publications yield N-1 blank lines.
    softly.assertThat(responseBody.lines().filter(String::isBlank)).hasSize(categories.size() - 1);
  }

  /**
   * A publication with both onlineIssn and printIssn should only return onlineIssn in BibTex
   * format.
   */
  @Test
  @DisplayName("Use onlineIssn when both onlineIssn and printIssn exists")
  @Description(useJavaDoc = true)
  void shouldReturnOnlineIssnWhenBothOnlineIssnAndPrintIssnIsPresent() {

    final var onlineIssn = "1520-4898";
    var titleUuid = UUID.randomUUID().toString();
    var title = "BibTex Integration test publication ISSN " + titleUuid;
    createIssnPublication(title);

    var response = waitForIndexing(titleUuid);
    assertThat(response.lines()).contains("  issn = {" + onlineIssn + "},");
  }

  private void createIssnPublication(String title) {
    final var issnJournalUri =
        baseURI
            + "/publication-channels-v2/serial-publication/271CEF41-0052-48CA-BB31-6780C7BA1F44/"
            + CURRENT_YEAR;

    var referenceMap =
        PUBLICATION_FACTORY.buildReferenceMap(
            new HashMap<>(Map.of("id", issnJournalUri)), new HashMap<>());

    PUBLICATION_FACTORY.createPublishedPublicationWithReference(
        UIB_CREATOR,
        title,
        ACADEMIC_ARTICLE,
        List.of(new Contributor(UIB_CREATOR, CREATOR)),
        UIB_PUBLISHING_CURATOR,
        referenceMap);
  }

  /** A publication with multiple authors should present a list of authors separated with 'and'. */
  @Test
  @DisplayName("Authors are joined with 'and'")
  @Description(useJavaDoc = true)
  void shouldPresentMultipleAuthorsSeparatedWithAnd(SoftAssertions softly) {
    var titleUuid = UUID.randomUUID().toString();
    var title = "BibTex Integration test publication multiple authors " + titleUuid;

    PUBLICATION_FACTORY.createPublishedPublication(
        UIB_CREATOR,
        title,
        ACADEMIC_ARTICLE,
        List.of(
            new Contributor(UIB_CREATOR, CREATOR),
            new Contributor(UIB_CONTRIBUTOR, CREATOR),
            new Contributor(UIB_PUBLISHING_CURATOR, CREATOR)),
        UIB_PUBLISHING_CURATOR);

    var response = waitForIndexing(titleUuid);
    var authorLine =
        response.lines().filter(line -> line.contains("author")).findFirst().orElse("").trim();
    softly.assertThat(authorLine).contains(UIB_CREATOR.name());
    softly.assertThat(authorLine).contains(UIB_CONTRIBUTOR.name());
    softly.assertThat(authorLine).contains(UIB_PUBLISHING_CURATOR.name());

    softly
        .assertThat(authorLine)
        .isEqualTo(
            "author = {"
                + UIB_CREATOR.name()
                + " and "
                + UIB_CONTRIBUTOR.name()
                + " and "
                + UIB_PUBLISHING_CURATOR.name()
                + "},");
  }

  /** A publication with multiple keywords should present a list of keywords separated with ','. */
  @Test
  @DisplayName("Keywords are joined with ','")
  @Description(useJavaDoc = true)
  void shouldPresentMultipleKeywordsSeparatedWithComma(SoftAssertions softly) {
    var titleUuid = UUID.randomUUID().toString();
    var title = "BibTex Integration test publication multiple keywords " + titleUuid;

    var response = PUBLICATION_FACTORY.createDraftPublication(UIB_CREATOR);
    var identifier = response.body().jsonPath().getString("identifier");
    Map<String, Object> payload = response.body().jsonPath().getMap("");
    payload.remove(PublicationFields.CONTEXT_FIELD);
    var entityDescription =
        PUBLICATION_FACTORY.createEntityDescription(
            title, ACADEMIC_ARTICLE, List.of(new Contributor(UIB_CREATOR, CREATOR)));
    entityDescription.put("tags", List.of("key1", "key2", "key3"));
    payload.put(ENTITY_DESCRIPTION_FIELD, entityDescription);

    PUBLICATION_FACTORY.updatePublication(UIB_CREATOR, payload);
    PUBLICATION_FACTORY.publish(UIB_PUBLISHING_CURATOR, identifier);

    var searchResponse = waitForIndexing(titleUuid);
    var keywordLine =
        searchResponse
            .lines()
            .filter(line -> line.contains("keywords"))
            .findFirst()
            .orElse("")
            .trim();
    softly.assertThat(keywordLine).contains("key1");
    softly.assertThat(keywordLine).contains("key2");
    softly.assertThat(keywordLine).contains("key3");

    softly.assertThat(keywordLine.chars().filter(ch -> ch == ',').count()).isEqualTo(3);
  }
}
