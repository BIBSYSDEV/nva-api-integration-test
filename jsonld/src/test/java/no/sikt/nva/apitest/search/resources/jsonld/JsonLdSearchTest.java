package no.sikt.nva.apitest.search.resources.jsonld;

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
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_THESIS_CURATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.ENTITY_DESCRIPTION_FIELD;
import static no.sikt.nva.apitest.search.SchemaOrgExpectationFixtures.EXPECTED_SCHEMA_ORG_ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.search.SchemaOrgExpectationFixtures.EXPECTED_SCHEMA_ORG_ACADEMIC_CHAPTER;
import static no.sikt.nva.apitest.search.SchemaOrgExpectationFixtures.EXPECTED_SCHEMA_ORG_ACADEMIC_MONOGRAPH;
import static no.sikt.nva.apitest.search.SchemaOrgExpectationFixtures.EXPECTED_SCHEMA_ORG_CONFERENCE_LECTURE;
import static no.sikt.nva.apitest.search.SchemaOrgExpectationFixtures.EXPECTED_SCHEMA_ORG_DEGREE_MASTER;
import static no.sikt.nva.apitest.search.SchemaOrgExpectationFixtures.EXPECTED_SCHEMA_ORG_DEGREE_PHD;
import static no.sikt.nva.apitest.search.SchemaOrgExpectationFixtures.EXPECTED_SCHEMA_ORG_REPORT_RESEARCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.sikt.Category;
import no.sikt.Contributor;
import no.sikt.nva.apitest.publication.PublicationFields;
import no.sikt.nva.apitest.search.SchemaOrgExpectation;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
class JsonLdSearchTest extends JsonLdTestBase {

  @InjectSoftAssertions private SoftAssertions softly;

  private static final String APPLICATION_VND_SCHEMAORG_LD_JSON =
      "application/vnd.schemaorg.ld+json";
  private static final String APPLICATION_LD_JSON_WITH_PROFILE =
      "application/ld+json; profile=\"https://schema.org\"";
  private static final String LD_JSON_CONTENT_TYPE_FRAGMENT = "ld+json";

  private static final String SCHEMA_ORG_CONTEXT = "https://schema.org";
  private static final String ITEM_LIST_TYPE = "ItemList";
  private static final String PERIODICAL_TYPE = "Periodical";
  private static final String BOOK_TYPE = "Book";
  private static final String ORGANIZATION_TYPE = "Organization";

  private static final String CUSTOMER_RESOURCES_PATH = "/search/customer/resources";

  private static final String CONTEXT_POINTER = "'@context'";
  private static final String TYPE_POINTER = "'@type'";
  private static final String ITEM_TYPES_POINTER = "itemListElement.'@type'";
  private static final String FIRST_ITEM_TYPE_POINTER = "itemListElement[0].'@type'";
  private static final String FIRST_ITEM_ID_POINTER = "itemListElement[0].'@id'";
  private static final String FIRST_ITEM_URL_POINTER = "itemListElement[0].url";
  private static final String FIRST_ITEM_NAME_POINTER = "itemListElement[0].name";
  private static final String FIRST_ITEM_DATE_PUBLISHED_POINTER =
      "itemListElement[0].datePublished";
  private static final String FIRST_ITEM_KEYWORDS_POINTER = "itemListElement[0].keywords";
  private static final String FIRST_ITEM_AUTHOR_NAMES_POINTER = "itemListElement[0].author.name";
  private static final String FIRST_ITEM_FIRST_AUTHOR_NAME_POINTER =
      "itemListElement[0].author[0].name";
  private static final String FIRST_ITEM_ISBN_POINTER = "itemListElement[0].isbn";
  private static final String FIRST_ITEM_PUBLISHER_TYPE_POINTER =
      "itemListElement[0].publisher.'@type'";
  private static final String FIRST_ITEM_PUBLISHER_NAME_POINTER =
      "itemListElement[0].publisher.name";
  private static final String FIRST_ITEM_IS_PART_OF_TYPE_POINTER =
      "itemListElement[0].isPartOf.'@type'";
  private static final String FIRST_ITEM_IS_PART_OF_NAME_POINTER =
      "itemListElement[0].isPartOf.name";

  private static final String EXPECTED_PUBLISHER = "Springer Nature";
  private static final String ONLINE_ISSN = "1520-4898";

  private static final String PUBLICATION_CHANNELS_PATH = "publication-channels-v2";
  private static final String SERIAL_PUBLICATION_PATH = "serial-publication";
  private static final String ISSN_JOURNAL_IDENTIFIER = "271CEF41-0052-48CA-BB31-6780C7BA1F44";

  /**
   * Tests that only read the search response share one publication per category, created once in
   * the nested class' BeforeAll instead of once per test.
   */
  @Nested
  class SharedPublicationSearches {

    private static final String SHARED_TITLE_PREFIX = "JsonLd Integration test publication ";
    private static final List<Category> SHARED_CATEGORIES =
        List.of(
            ACADEMIC_ARTICLE,
            ACADEMIC_MONOGRAPH,
            ACADEMIC_CHAPTER,
            DEGREE_MASTER,
            DEGREE_PHD,
            RESEARCH_REPORT,
            CONFERENCE_LECTURE);
    private static final Map<Category, String> TITLE_UUIDS_BY_CATEGORY = new ConcurrentHashMap<>();

    @BeforeAll
    static void createSharedPublications() {
      SHARED_CATEGORIES.parallelStream()
          .forEach(
              category -> {
                var titleUuid = UUID.randomUUID().toString();
                createTestPublication(category, SHARED_TITLE_PREFIX + titleUuid);
                TITLE_UUIDS_BY_CATEGORY.put(category, titleUuid);
              });
    }

    private static Stream<Arguments> publicationsInJsonLdFormatProvider() {
      return Stream.of(
          argumentSet("AcademicArticle", ACADEMIC_ARTICLE, EXPECTED_SCHEMA_ORG_ACADEMIC_ARTICLE),
          argumentSet(
              "AcademicMonograph", ACADEMIC_MONOGRAPH, EXPECTED_SCHEMA_ORG_ACADEMIC_MONOGRAPH),
          argumentSet("AcademicChapter", ACADEMIC_CHAPTER, EXPECTED_SCHEMA_ORG_ACADEMIC_CHAPTER),
          argumentSet("DegreeMaster", DEGREE_MASTER, EXPECTED_SCHEMA_ORG_DEGREE_MASTER),
          argumentSet("DegreePhD", DEGREE_PHD, EXPECTED_SCHEMA_ORG_DEGREE_PHD),
          argumentSet("ReportResearch", RESEARCH_REPORT, EXPECTED_SCHEMA_ORG_REPORT_RESEARCH),
          argumentSet(
              "ConferenceLecture", CONFERENCE_LECTURE, EXPECTED_SCHEMA_ORG_CONFERENCE_LECTURE));
    }

    private static Stream<Arguments> acceptHeaderVariantsProvider() {
      return Stream.of(
          argumentSet(APPLICATION_LD_JSON, APPLICATION_LD_JSON),
          argumentSet(APPLICATION_VND_SCHEMAORG_LD_JSON, APPLICATION_VND_SCHEMAORG_LD_JSON),
          argumentSet(APPLICATION_LD_JSON_WITH_PROFILE, APPLICATION_LD_JSON_WITH_PROFILE));
    }

    private static String sharedTitleUuid(Category category) {
      return TITLE_UUIDS_BY_CATEGORY.get(category);
    }

    private static String sharedTitle(Category category) {
      return SHARED_TITLE_PREFIX + sharedTitleUuid(category);
    }

    @ParameterizedTest
    @MethodSource("publicationsInJsonLdFormatProvider")
    @DisplayName("Search with content type 'application/ld+json' produces schema.org JSON-LD")
    @Description(
        "Search returned with content type 'application/ld+json' is valid schema.org JSON-LD")
    void shouldReturnPublicationsInJsonLdFormat(
        Category category, SchemaOrgExpectation expectation) {

      var response = awaitIndexed(sharedTitleUuid(category));
      var body = itemList(response);

      softly.assertThat(response.getContentType()).contains(LD_JSON_CONTENT_TYPE_FRAGMENT);
      assertItemListEnvelope(body);
      softly.assertThat(body.getInt(NUMBER_OF_ITEMS_POINTER)).isGreaterThanOrEqualTo(1);
      softly
          .assertThat(body.getString(FIRST_ITEM_TYPE_POINTER))
          .isEqualTo(expectation.schemaOrgType());
      softly.assertThat(body.getString(FIRST_ITEM_NAME_POINTER)).isEqualTo(sharedTitle(category));
      softly.assertThat(body.getString(FIRST_ITEM_ID_POINTER)).isNotBlank();
      softly.assertThat(body.getString(FIRST_ITEM_URL_POINTER)).isNotBlank();
      softly.assertThat(body.getString(FIRST_ITEM_DATE_PUBLISHED_POINTER)).isEqualTo(CURRENT_YEAR);
      softly
          .assertThat(body.getString(FIRST_ITEM_FIRST_AUTHOR_NAME_POINTER))
          .isEqualTo(UIB_CREATOR.name());
    }

    @ParameterizedTest
    @MethodSource("publicationsInJsonLdFormatProvider")
    @DisplayName("Search with content type 'application/ld+json' produces JSON-LD for customer")
    @Description(
        "Authenticated customer search returned with content type 'application/ld+json' is valid"
            + " schema.org JSON-LD")
    void shouldReturnPublicationsInJsonLdFormatForCustomer(
        Category category, SchemaOrgExpectation expectation) {

      var response =
          pollUntil(
              () -> searchCustomerResources(sharedTitleUuid(category), APPLICATION_LD_JSON),
              result -> itemList(result).getInt(NUMBER_OF_ITEMS_POINTER) >= 1);
      var body = itemList(response);

      softly.assertThat(response.getContentType()).contains(LD_JSON_CONTENT_TYPE_FRAGMENT);
      assertItemListEnvelope(body);
      softly.assertThat(body.getInt(NUMBER_OF_ITEMS_POINTER)).isGreaterThanOrEqualTo(1);
      softly
          .assertThat(body.getString(FIRST_ITEM_TYPE_POINTER))
          .isEqualTo(expectation.schemaOrgType());
      softly.assertThat(body.getString(FIRST_ITEM_NAME_POINTER)).isEqualTo(sharedTitle(category));
    }

    @ParameterizedTest
    @MethodSource("acceptHeaderVariantsProvider")
    @DisplayName("All schema.org Accept-header variants return JSON-LD")
    @Description(
        "The three negotiable media types (application/ld+json, the vendor type and the profile"
            + " parameter variant) all resolve to schema.org JSON-LD")
    void shouldReturnSchemaOrgForAllAcceptHeaderVariants(String acceptHeader) {

      var response =
          pollUntil(
              () -> searchResources(sharedTitleUuid(ACADEMIC_ARTICLE), acceptHeader),
              result -> itemList(result).getInt(NUMBER_OF_ITEMS_POINTER) >= 1);
      var body = itemList(response);

      softly.assertThat(response.getContentType()).contains(LD_JSON_CONTENT_TYPE_FRAGMENT);
      assertItemListEnvelope(body);
      softly
          .assertThat(body.getString(FIRST_ITEM_TYPE_POINTER))
          .isEqualTo(EXPECTED_SCHEMA_ORG_ACADEMIC_ARTICLE.schemaOrgType());
      softly
          .assertThat(body.getString(FIRST_ITEM_NAME_POINTER))
          .isEqualTo(sharedTitle(ACADEMIC_ARTICLE));
    }

    @ParameterizedTest
    @MethodSource("acceptHeaderVariantsProvider")
    @DisplayName("All schema.org Accept-header variants return JSON-LD for customer")
    @Description(
        "The three negotiable media types (application/ld+json, the vendor type and the profile"
            + " parameter variant) all resolve to schema.org JSON-LD on the authenticated customer"
            + " endpoint")
    void shouldReturnSchemaOrgForAllAcceptHeaderVariantsForCustomer(String acceptHeader) {

      var response =
          pollUntil(
              () -> searchCustomerResources(sharedTitleUuid(ACADEMIC_ARTICLE), acceptHeader),
              result -> itemList(result).getInt(NUMBER_OF_ITEMS_POINTER) >= 1);
      var body = itemList(response);

      softly.assertThat(response.getContentType()).contains(LD_JSON_CONTENT_TYPE_FRAGMENT);
      assertItemListEnvelope(body);
      softly
          .assertThat(body.getString(FIRST_ITEM_TYPE_POINTER))
          .isEqualTo(EXPECTED_SCHEMA_ORG_ACADEMIC_ARTICLE.schemaOrgType());
      softly
          .assertThat(body.getString(FIRST_ITEM_NAME_POINTER))
          .isEqualTo(sharedTitle(ACADEMIC_ARTICLE));
    }
  }

  @Test
  @DisplayName("Search for multiple publications returns a schema.org ItemList")
  @Description(
      "A search matching several publications returns an ItemList whose numberOfItems and"
          + " itemListElement reflect all matches")
  void shouldReturnItemListWithMultiplePublications() {

    var commonUuid = UUID.randomUUID().toString();
    var titleRoot = "JsonLd-test-publication";
    var categories = List.of(ACADEMIC_ARTICLE, ACADEMIC_MONOGRAPH, DEGREE_MASTER, RESEARCH_REPORT);

    IntStream.range(0, categories.size())
        .forEach(
            index ->
                createTestPublication(
                    categories.get(index),
                    titleRoot + index + " " + commonUuid + " " + UUID.randomUUID()));

    var response = awaitIndexedCount(commonUuid, categories.size());
    var body = itemList(response);

    softly
        .assertThat(body.getInt(NUMBER_OF_ITEMS_POINTER))
        .isGreaterThanOrEqualTo(categories.size());
    softly
        .assertThat(body.getList(ITEM_LIST_ELEMENT_POINTER))
        .hasSizeGreaterThanOrEqualTo(categories.size());
    softly.assertThat(body.getList(ITEM_TYPES_POINTER)).doesNotContainNull();
  }

  @Test
  @DisplayName("Article journal is exposed as a Periodical with its ISSN")
  @Description(
      "An article with both onlineIssn and printIssn should expose the onlineIssn on a schema.org"
          + " Periodical")
  void shouldExposeOnlineIssnOnPeriodicalForArticle() {

    var titleUuid = UUID.randomUUID().toString();
    var title = "JsonLd Integration test publication ISSN " + titleUuid;

    createIssnPublication(title);

    var response = awaitIndexed(titleUuid);

    softly.assertThat(response.body().asString()).contains(PERIODICAL_TYPE);
    softly.assertThat(response.body().asString()).contains(ONLINE_ISSN);
  }

  @Test
  @DisplayName("Monograph exposes ISBN and publisher")
  @Description("A monograph is a schema.org Book carrying its ISBN and a publisher Organization")
  void shouldExposeIsbnAndPublisherForMonograph() {

    var titleUuid = UUID.randomUUID().toString();
    var title = "JsonLd Integration test publication monograph " + titleUuid;

    PUBLICATION_FACTORY.createPublishedPublication(
        UIB_CREATOR,
        title,
        ACADEMIC_MONOGRAPH,
        List.of(new Contributor(UIB_CREATOR, CREATOR)),
        UIB_PUBLISHING_CURATOR);

    var response = awaitIndexed(titleUuid);
    var body = itemList(response);

    softly
        .assertThat(body.getString(FIRST_ITEM_TYPE_POINTER))
        .isEqualTo(EXPECTED_SCHEMA_ORG_ACADEMIC_MONOGRAPH.schemaOrgType());
    softly.assertThat(body.getString(FIRST_ITEM_ISBN_POINTER)).isNotBlank();
    softly
        .assertThat(body.getString(FIRST_ITEM_PUBLISHER_TYPE_POINTER))
        .isEqualTo(ORGANIZATION_TYPE);
    softly
        .assertThat(body.getString(FIRST_ITEM_PUBLISHER_NAME_POINTER))
        .isEqualTo(EXPECTED_PUBLISHER);
  }

  @Test
  @DisplayName("Chapter exposes its book through isPartOf")
  @Description(
      "A chapter in an anthology is a schema.org Chapter whose isPartOf is the containing Book")
  void shouldExposeBookThroughIsPartOfForChapter() {

    var titleUuid = UUID.randomUUID().toString();
    var title = "JsonLd Integration test publication chapter " + titleUuid;
    var anthologyTitle = "JsonLd integration test anthology " + UUID.randomUUID();

    var anthologyIdentifier =
        PUBLICATION_FACTORY.createAnthologyForChapter(
            UIB_CREATOR,
            anthologyTitle,
            UIB_PUBLISHING_CURATOR,
            List.of(new Contributor(UIB_CREATOR, CREATOR)));

    PUBLICATION_FACTORY.createChapterInAnthology(
        UIB_CREATOR,
        title,
        ACADEMIC_CHAPTER,
        List.of(new Contributor(UIB_CREATOR, CREATOR)),
        UIB_PUBLISHING_CURATOR,
        anthologyIdentifier);

    var response = awaitIndexed(titleUuid);
    var body = itemList(response);

    softly
        .assertThat(body.getString(FIRST_ITEM_TYPE_POINTER))
        .isEqualTo(EXPECTED_SCHEMA_ORG_ACADEMIC_CHAPTER.schemaOrgType());
    softly.assertThat(body.getString(FIRST_ITEM_IS_PART_OF_TYPE_POINTER)).isEqualTo(BOOK_TYPE);
    softly.assertThat(body.getString(FIRST_ITEM_IS_PART_OF_NAME_POINTER)).contains(anthologyTitle);
  }

  @Test
  @DisplayName("Keywords are joined with ', '")
  @Description(
      "Publication tags are exposed as a single comma-separated schema.org keywords string")
  void shouldJoinMultipleKeywordsWithComma() {

    var titleUuid = UUID.randomUUID().toString();
    var title = "JsonLd Integration test publication multiple keywords " + titleUuid;

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

    var searchResponse = awaitIndexed(titleUuid);
    var keywords = itemList(searchResponse).getString(FIRST_ITEM_KEYWORDS_POINTER);

    assertThat(keywords)
        .isNotBlank()
        .satisfies(
            joined ->
                assertThat(joined.split(", ")).containsExactlyInAnyOrder("key1", "key2", "key3"));
  }

  @Test
  @DisplayName("Multiple authors are exposed as a list")
  @Description("A publication with several creators exposes each as a schema.org author")
  void shouldExposeMultipleAuthors() {

    var titleUuid = UUID.randomUUID().toString();
    var title = "JsonLd Integration test publication multiple authors " + titleUuid;

    PUBLICATION_FACTORY.createPublishedPublication(
        UIB_CREATOR,
        title,
        ACADEMIC_ARTICLE,
        List.of(
            new Contributor(UIB_CREATOR, CREATOR),
            new Contributor(UIB_CONTRIBUTOR, CREATOR),
            new Contributor(UIB_PUBLISHING_CURATOR, CREATOR)),
        UIB_PUBLISHING_CURATOR);

    var response = awaitIndexed(titleUuid);
    var authorNames = itemList(response).getList(FIRST_ITEM_AUTHOR_NAMES_POINTER, String.class);

    softly
        .assertThat(authorNames)
        .containsExactlyInAnyOrder(
            UIB_CREATOR.name(), UIB_CONTRIBUTOR.name(), UIB_PUBLISHING_CURATOR.name());
  }

  private void assertItemListEnvelope(JsonPath body) {
    softly.assertThat(body.getString(CONTEXT_POINTER)).isEqualTo(SCHEMA_ORG_CONTEXT);
    softly.assertThat(body.getString(TYPE_POINTER)).isEqualTo(ITEM_LIST_TYPE);
  }

  private Response searchResources(String query, String acceptHeader) {
    return given()
        .param("query", query)
        .basePath(RESOURCES_PATH)
        .accept(acceptHeader)
        .when()
        .get()
        .then()
        .statusCode(200)
        .extract()
        .response();
  }

  private Response searchCustomerResources(String query, String acceptHeader) {
    return givenAuthenticatedJsonRequestAsUser(UIB_PUBLISHING_CURATOR)
        .param("query", query)
        .basePath(CUSTOMER_RESOURCES_PATH)
        .accept(acceptHeader)
        .when()
        .get()
        .then()
        .statusCode(200)
        .extract()
        .response();
  }

  private Response awaitIndexed(String query) {
    return pollUntil(
        () -> searchResources(query, APPLICATION_LD_JSON),
        response -> itemList(response).getInt(NUMBER_OF_ITEMS_POINTER) >= 1);
  }

  private Response awaitIndexedCount(String query, int expectedCount) {
    return pollUntil(
        () -> searchResources(query, APPLICATION_LD_JSON),
        response -> itemList(response).getList(ITEM_LIST_ELEMENT_POINTER).size() >= expectedCount);
  }

  private static void createTestPublication(Category category, String title) {
    switch (category) {
      case ACADEMIC_CHAPTER -> {
        var anthologyIdentifier =
            PUBLICATION_FACTORY.createAnthologyForChapter(
                UIB_CREATOR,
                "JsonLd integration test anthology " + UUID.randomUUID(),
                UIB_PUBLISHING_CURATOR,
                List.of(new Contributor(UIB_CREATOR, CREATOR)));

        PUBLICATION_FACTORY.createChapterInAnthology(
            UIB_CREATOR,
            title,
            category,
            List.of(new Contributor(UIB_CREATOR, CREATOR)),
            UIB_PUBLISHING_CURATOR,
            anthologyIdentifier);
      }
      case DEGREE_PHD, DEGREE_MASTER ->
          PUBLICATION_FACTORY.createPublishedPublication(
              UIB_THESIS_CURATOR,
              title,
              category,
              List.of(new Contributor(UIB_CREATOR, CREATOR)),
              UIB_THESIS_CURATOR);
      default ->
          PUBLICATION_FACTORY.createPublishedPublication(
              UIB_CREATOR,
              title,
              category,
              List.of(new Contributor(UIB_CREATOR, CREATOR)),
              UIB_PUBLISHING_CURATOR);
    }
  }

  private void createIssnPublication(String title) {
    var issnJournalUri =
        "%s/%s/%s/%s/%s"
            .formatted(
                baseURI,
                PUBLICATION_CHANNELS_PATH,
                SERIAL_PUBLICATION_PATH,
                ISSN_JOURNAL_IDENTIFIER,
                CURRENT_YEAR);

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
}
