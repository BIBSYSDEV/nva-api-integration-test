package no.sikt.nva.apitest.search.resources.bibtex;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.Category.ACADEMIC_CHAPTER;
import static no.sikt.Category.ACADEMIC_MONOGRAPH;
import static no.sikt.Category.CONFERENCE_LECTURE;
import static no.sikt.Category.DEGREE_MASTER;
import static no.sikt.Category.DEGREE_PHD;
import static no.sikt.Category.RESEARCH_REPORT;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_MONTH_SHORT_NAME;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_THESIS_CURATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.ENTITY_DESCRIPTION_FIELD;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_CHAPTER;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_CONFERENCE_LECTURE;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_DEGREE_MASTER;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_DEGREE_PHD;
import static no.sikt.nva.apitest.search.BibTexExpectationFixtures.EXPECTED_BIBTEX_REPORT_RESEARCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.sikt.Category;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.publication.PublicationFields;
import no.sikt.nva.apitest.search.BibTexExpectation;
import no.sikt.nva.apitest.search.SearchTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
class BibTexTest extends SearchTestBase {

  @InjectSoftAssertions private SoftAssertions softly;

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

  private String createTestPublication(Category category, String title) {
    return switch (category) {
      case ACADEMIC_CHAPTER -> {
        var anthologyIdentifier =
            PUBLICATION_FACTORY.createAnthologyForChapter(
                UIB_CREATOR,
                "BibTex integration test anthology " + UUID.randomUUID(),
                UIB_PUBLISHING_CURATOR,
                List.of(UIB_CREATOR));

        yield PUBLICATION_FACTORY.createChapterInAnthology(
            UIB_CREATOR,
            title,
            category,
            List.of(UIB_CREATOR),
            UIB_PUBLISHING_CURATOR,
            anthologyIdentifier);
      }
      case DEGREE_PHD, DEGREE_MASTER ->
          PUBLICATION_FACTORY.createPublishedPublication(
              UIB_THESIS_CURATOR, title, category, List.of(UIB_CREATOR), UIB_THESIS_CURATOR);
      default ->
          PUBLICATION_FACTORY.createPublishedPublication(
              UIB_CREATOR, title, category, List.of(UIB_CREATOR), UIB_PUBLISHING_CURATOR);
    };
  }

  @ParameterizedTest
  @MethodSource("publicationsInBibTexFormatProvider")
  @DisplayName("Search with content type 'text/x-bibtex' produces BibTeX export")
  @Description("Test content returned with content type 'text/x-bibtex' is correct BibTex-format")
  void shouldReturnPublicationsInBibTexFormat(Category category, BibTexExpectation expectation) {

    RestAssured.registerParser(TEXT_X_BIBTEX, Parser.TEXT);

    var titleUuid = UUID.randomUUID().toString();
    var title = "BibTex Integration test publication " + titleUuid;

    var identifier = createTestPublication(category, title);

    waitForIndexing(titleUuid);

    var responseBody = getResponseBody(titleUuid);
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

  private void waitForIndexing(String query) {
    with()
        .pollInterval(fibonacci().with().unit(SECONDS))
        .await()
        .atMost(120, SECONDS)
        .until(
            () -> {
              var body = getResponseBody(query);
              return body != null && !body.isEmpty();
            });

    assertThat(getResponseBody(query)).isNotEmpty();
  }

  @ParameterizedTest
  @MethodSource("publicationsInBibTexFormatProvider")
  @DisplayName("Search with content type 'text/x-bibtex' produces BibTeX export for customer")
  @Description(
      "Search returned with content type 'text/x-bibtex' is correct BibTex-format for customer")
  void shouldReturnPublicationsInBibTexFormatForCustomer(
      Category category, BibTexExpectation expectation) {

    RestAssured.registerParser(TEXT_X_BIBTEX, Parser.TEXT);

    var titleUuid = UUID.randomUUID().toString();
    var title = "BibTex Integration test publication " + titleUuid;

    var identifier = createTestPublication(category, title);

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

  @Test
  @DisplayName("Search for multiple publications, return text/x-bibtex format")
  @Description(
      "A list of publications should contain a type that starts with '@' and two newlines between"
          + " publications")
  void shouldReturnListOfPublicationsInBibTexFormat() {

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
                createTestPublication(
                    categories.get(i), titleRoot + i + " " + commonUuid + " " + UUID.randomUUID()));

    // retry until last test publication is indexed
    var retryQuery = titleRoot + (categories.size() - 1);
    waitForIndexing(retryQuery);

    var responseBody = getResponseBody(commonUuid);

    softly
        .assertThat(responseBody.lines().filter(line -> line.startsWith("@")))
        .hasSize(categories.size());
    softly
        .assertThat(responseBody.lines().filter(String::isBlank))
        .hasSize((categories.size() - 1) * 2);
  }

  @Test
  @DisplayName("Use onlineIssn when both onlineIssn and printIssn exists")
  @Description(
      "A publication with both onlineIssn and printIssn should only return onlineIssn in BibTex"
          + " format")
  void shouldReturnOnlineIssnWhenBothOnlineIssnAndPrintIssnIsPresent() {

    final var onlineIssn = "1520-4898";
    var titleUuid = UUID.randomUUID().toString();
    var title = "BibTex Integration test publication ISSN " + titleUuid;
    createIssnPublication(title);

    waitForIndexing(titleUuid);

    var response = getResponseBody(titleUuid);
    assertThat(response.lines()).contains("  issn = {" + onlineIssn + "},");
  }

  private void createIssnPublication(String title) {
    final var issnJournalUri =
        baseURI
            + "/publication-channels-v2/serial-publication/271CEF41-0052-48CA-BB31-6780C7BA1F44/"
            + CURRENT_YEAR;

    var publicationContextMap = new HashMap<String, Object>();
    publicationContextMap.put("id", issnJournalUri);
    var referenceMap =
        PUBLICATION_FACTORY.buildReferenceMap(publicationContextMap, new HashMap<>());

    PUBLICATION_FACTORY.createPublishedPublicationWithReference(
        UIB_CREATOR,
        title,
        ACADEMIC_ARTICLE,
        List.of(UIB_CREATOR),
        UIB_PUBLISHING_CURATOR,
        referenceMap);
  }

  @Test
  @DisplayName("Authors are joined with 'and'")
  @Description(
      "A publication with multiple authors should present a list of authors separated with 'and'")
  void shouldPresentMultipleAuthorsSeparatedWithAnd() {
    var titleUuid = UUID.randomUUID().toString();
    var title = "BibTex Integration test publication multiple authors " + titleUuid;

    PUBLICATION_FACTORY.createPublishedPublication(
        UIB_CREATOR,
        title,
        ACADEMIC_ARTICLE,
        List.of(UIB_CREATOR, UIB_CONTRIBUTOR, UIB_PUBLISHING_CURATOR),
        UIB_PUBLISHING_CURATOR);

    waitForIndexing(titleUuid);

    var response = getResponseBody(titleUuid);
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

  @Test
  @DisplayName("Keywords are joined with ','")
  @Description(
      "A publication with multiple keywords should present a list of keywords separated with ','")
  void shouldPresentMultipleKeywordsSeparatedWithComma() {
    var titleUuid = UUID.randomUUID().toString();
    var title = "BibTex Integration test publication multiple keywords " + titleUuid;

    var response = PUBLICATION_FACTORY.createDraftPublication(UIB_CREATOR);
    var identifier = response.body().jsonPath().getString("identifier");
    Map<String, Object> payload = response.body().jsonPath().getMap("");
    payload.remove(PublicationFields.CONTEXT_FIELD);
    var entityDescription =
        PUBLICATION_FACTORY.createEntityDescription(title, ACADEMIC_ARTICLE, List.of(UIB_CREATOR));
    entityDescription.put("tags", List.of("key1", "key2", "key3"));
    payload.put(ENTITY_DESCRIPTION_FIELD, entityDescription);

    PUBLICATION_FACTORY.updatePublication(UIB_CREATOR, payload);
    PUBLICATION_FACTORY.publish(UIB_PUBLISHING_CURATOR, identifier);

    waitForIndexing(titleUuid);

    var searchResponse = getResponseBody(titleUuid);
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
