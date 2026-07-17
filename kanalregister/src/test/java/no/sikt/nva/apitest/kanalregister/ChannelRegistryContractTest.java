package no.sikt.nva.apitest.kanalregister;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.qameta.allure.Description;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Contract tests for all GET endpoints of Kanalregisteret's external nva-api. Tests assert the
 * desired contract identically for both of HKDir's environments, so a red test means an upstream
 * bug: contact HKDir, never adjust assertions to match broken behavior.
 *
 * <p>Test methods are inherited by the per-environment subclasses. Because of that, soft assertions
 * use local {@link SoftAssertions#assertSoftly} blocks (an injected extension field leaked failures
 * between concurrent tests), and {@code @Description} texts are not shown in the Allure report (the
 * allure-junit5 listener does not resolve annotations on inherited methods).
 */
abstract class ChannelRegistryContractTest extends KanalregisterTestBase {

  private static final String LEVEL_PATH = "levelElementDto.level";
  private static final String LEVEL_YEAR_PATH = "levelElementDto.year";

  private static final String JOURNAL_RESOURCE = "findjournal";
  private static final String JOURNAL_SERIES_RESOURCE = "findjournalserie";
  private static final String SERIES_RESOURCE = "findseries";
  private static final String PUBLISHER_RESOURCE = "findpublisher";

  // Journal fixture (ACP): level 2 every year 2022-2027 (2022-2026 in kar-test).
  private static final String ACP_PID = "A135A19F-4111-4184-AB2F-7C00AA24BA05";
  private static final String ACP_NAME = "Atmospheric Chemistry and Physics";
  private static final String ACP_EISSN = "1680-7324";
  private static final String ACP_LEVEL = "2";
  private static final int ACP_YEAR = 2024;

  // Series fixture (LNCS): level 1 in 2024; the name matches several channels, so hits are picked
  // by PID.
  private static final String LNCS_PID = "C8CCD71B-FD8B-47B4-B72A-905F6219D7D5";
  private static final String LNCS_NAME = "Lecture Notes in Computer Science";
  private static final String LNCS_LEVEL = "1";
  private static final int LNCS_YEAR = 2024;

  // Publisher lookup fixture (Gyldendal Forlag): level 1 in 2025; named differently per
  // environment, so only used for lookups by PID.
  private static final String GYLDENDAL_PID = "9B2EE655-49AC-48D8-B33E-DADA32CDDB40";
  private static final String GYLDENDAL_LEVEL = "1";
  private static final int GYLDENDAL_YEAR = 2025;

  // Publisher search fixture (Gyldendal Undervisning): level 0 in 2025; the name matches exactly
  // one channel in both environments.
  private static final String GYLDENDAL_UNDERVISNING_PID = "69843B08-E4DC-4D51-AA13-CFB03107AA92";
  private static final String GYLDENDAL_UNDERVISNING_NAME = "Gyldendal Undervisning";
  private static final String GYLDENDAL_UNDERVISNING_LEVEL = "0";
  private static final int GYLDENDAL_UNDERVISNING_YEAR = 2025;

  // X-channel fixture (JCM): level X in 2026 with counting level 1; no level data in kar-test.
  private static final String JCM_PID = "30EAF3E3-A276-4E8E-86CC-9BCFFAFAA94E";
  private static final String JCM_NAME = "Journal of clinical medicine";
  private static final int JCM_X_YEAR = 2026;

  private static final String SEARCH_HITS_PATH = "entityResultSetDto.pageresult";
  private static final int SEARCH_PAGE_SIZE = 50;
  private static final String NAMED_BY_RESOURCE = "[{index}] {0}";

  private final String apiHost;
  private final boolean xChannelFixtureHasLevelData;

  protected ChannelRegistryContractTest(String apiHost, boolean xChannelFixtureHasLevelData) {
    super();
    this.apiHost = apiHost;
    this.xChannelFixtureHasLevelData = xChannelFixtureHasLevelData;
  }

  static Stream<Arguments> lookupFixtures() {
    return Stream.of(
        Arguments.of(JOURNAL_RESOURCE, ACP_PID, ACP_YEAR, ACP_LEVEL),
        Arguments.of(JOURNAL_SERIES_RESOURCE, ACP_PID, ACP_YEAR, ACP_LEVEL),
        Arguments.of(SERIES_RESOURCE, LNCS_PID, LNCS_YEAR, LNCS_LEVEL),
        Arguments.of(PUBLISHER_RESOURCE, GYLDENDAL_PID, GYLDENDAL_YEAR, GYLDENDAL_LEVEL));
  }

  static Stream<Arguments> searchFixtures() {
    return Stream.of(
        Arguments.of(JOURNAL_RESOURCE, ACP_NAME, ACP_PID, ACP_YEAR, ACP_LEVEL),
        Arguments.of(JOURNAL_SERIES_RESOURCE, ACP_NAME, ACP_PID, ACP_YEAR, ACP_LEVEL),
        Arguments.of(SERIES_RESOURCE, LNCS_NAME, LNCS_PID, LNCS_YEAR, LNCS_LEVEL),
        Arguments.of(
            PUBLISHER_RESOURCE,
            GYLDENDAL_UNDERVISNING_NAME,
            GYLDENDAL_UNDERVISNING_PID,
            GYLDENDAL_UNDERVISNING_YEAR,
            GYLDENDAL_UNDERVISNING_LEVEL));
  }

  static Stream<Arguments> noYearLookupFixtures() {
    return Stream.of(
        Arguments.of(JOURNAL_RESOURCE, ACP_PID),
        Arguments.of(JOURNAL_SERIES_RESOURCE, ACP_PID),
        Arguments.of(SERIES_RESOURCE, LNCS_PID),
        Arguments.of(PUBLISHER_RESOURCE, GYLDENDAL_PID));
  }

  @ParameterizedTest(name = NAMED_BY_RESOURCE)
  @MethodSource("lookupFixtures")
  @DisplayName("Lookup returns level for the requested year")
  @Description("A lookup returns the level for the requested year, like search does")
  void shouldReturnLevelForRequestedYearOnLookup(
      String resource, String pid, int year, String expectedLevel) {
    var response = lookUpChannel(resource, pid, year);

    SoftAssertions.assertSoftly(
        softly -> {
          softly.assertThat(response.getString(LEVEL_PATH)).isEqualTo(expectedLevel);
          softly.assertThat(response.getString(LEVEL_YEAR_PATH)).isEqualTo(String.valueOf(year));
        });
  }

  @ParameterizedTest(name = NAMED_BY_RESOURCE)
  @MethodSource("searchFixtures")
  @DisplayName("Search by name returns level for the requested year")
  @Description("A name search returns hits with the level for the requested year")
  void shouldReturnLevelForRequestedYearOnSearch(
      String resource, String searchName, String expectedPid, int year, String expectedLevel) {
    var hit = searchChannels(resource, "name", searchName, year).setRootPath(hitByPid(expectedPid));

    SoftAssertions.assertSoftly(
        softly -> {
          softly.assertThat(hit.getString(LEVEL_PATH)).isEqualTo(expectedLevel);
          softly.assertThat(hit.getString(LEVEL_YEAR_PATH)).isEqualTo(String.valueOf(year));
        });
  }

  @ParameterizedTest(name = NAMED_BY_RESOURCE)
  @MethodSource("noYearLookupFixtures")
  @DisplayName("Lookup without year does not return the highest-year level")
  @Description(
      "A lookup without year returns the current year's level or null, never the highest year"
          + " (contract not yet confirmed by HKDir)")
  void shouldNotReturnHighestYearLevelOnLookupWithoutYear(String resource, String pid) {
    var response = lookUpChannelWithoutYear(resource, pid);

    var currentYear = String.valueOf(LocalDate.now(ZoneId.systemDefault()).getYear());
    var levelYear = response.getString(LEVEL_YEAR_PATH);
    assertThat(levelYear)
        .as("levelElementDto.year without a year segment")
        .satisfiesAnyOf(
            year -> assertThat(year).isNull(), year -> assertThat(year).isEqualTo(currentYear));
  }

  @ParameterizedTest(name = NAMED_BY_RESOURCE)
  @MethodSource("lookupFixtures")
  @DisplayName("Level history includes the requested year")
  @Description("A lookup's levelHistories includes the requested year (agreed with HKDir)")
  void shouldIncludeRequestedYearInLevelHistory(
      String resource, String pid, int year, String expectedLevel) {
    var response = lookUpChannel(resource, pid, year);

    var levelForRequestedYear =
        response.getString("levelHistories.find { it.year == %d }.level".formatted(year));
    assertThat(levelForRequestedYear)
        .as("levelHistories entry for %d", year)
        .isEqualTo(expectedLevel);
  }

  @ParameterizedTest(name = NAMED_BY_RESOURCE)
  @MethodSource("lookupFixtures")
  @DisplayName("Lookup exposes levelDisplay alongside level")
  @Description("A lookup exposes levelDisplay, without which X-channels cannot be distinguished")
  void shouldExposeLevelDisplayOnLookup(
      String resource, String pid, int year, String expectedLevel) {
    var response = lookUpChannel(resource, pid, year);

    assertThat(response.getString("levelElementDto.levelDisplay"))
        .as("levelDisplay for a channel with level %s", expectedLevel)
        .isEqualTo(expectedLevel);
  }

  @Test
  @DisplayName("X-channels carry counting level and X mark separately in search")
  @Description("A search hit for an X-channel has the counting level and the X mark separately")
  void shouldExposeCountingLevelAndXMarkSeparatelyInSearch() {
    assumeTrue(
        xChannelFixtureHasLevelData,
        "The X-channel fixture (Journal of Clinical Medicine) has no level data in kar-test");

    var jcmHit =
        searchChannels(JOURNAL_SERIES_RESOURCE, "name", JCM_NAME, JCM_X_YEAR)
            .setRootPath(hitByPid(JCM_PID));

    SoftAssertions.assertSoftly(
        softly -> {
          softly.assertThat(jcmHit.getString(LEVEL_PATH)).isEqualTo("1");
          softly.assertThat(jcmHit.getString("levelElementDto.levelDisplay")).isEqualTo("X");
        });
  }

  @Test
  @DisplayName("X-channels carry counting level and X mark separately in lookup")
  @Description("A lookup on an X-channel has the counting level and the X mark separately")
  void shouldExposeCountingLevelAndXMarkSeparatelyInLookup() {
    assumeTrue(
        xChannelFixtureHasLevelData,
        "The X-channel fixture (Journal of Clinical Medicine) has no level data in kar-test");

    var response = lookUpChannel(JOURNAL_SERIES_RESOURCE, JCM_PID, JCM_X_YEAR);

    SoftAssertions.assertSoftly(
        softly -> {
          softly.assertThat(response.getString(LEVEL_PATH)).isEqualTo("1");
          softly.assertThat(response.getString("levelElementDto.levelDisplay")).isEqualTo("X");
        });
  }

  @Test
  @DisplayName("ISSN search resolves to exactly one channel")
  @Description("An ISSN search resolves to exactly one channel")
  void shouldResolveIssnSearchToSingleChannel() {
    var response = searchChannels(JOURNAL_SERIES_RESOURCE, "issn", ACP_EISSN, ACP_YEAR);

    SoftAssertions.assertSoftly(
        softly -> {
          softly
              .assertThat(response.getInt("entityPageInformationDto.totalResults"))
              .as("totalResults")
              .isEqualTo(1);
          softly
              .assertThat(response.getList(SEARCH_HITS_PATH + ".pid", String.class))
              .containsExactly(ACP_PID);
        });
  }

  @Test
  @DisplayName("Missing values are JSON null, not the string \"null\"")
  @Description("Fields without a value are JSON null, never the literal string \"null\"")
  void shouldRepresentMissingDecisionTextsAsNull() {
    var hit =
        searchChannels(
                PUBLISHER_RESOURCE,
                "name",
                GYLDENDAL_UNDERVISNING_NAME,
                GYLDENDAL_UNDERVISNING_YEAR)
            .setRootPath(hitByPid(GYLDENDAL_UNDERVISNING_PID));

    SoftAssertions.assertSoftly(
        softly -> {
          softly
              .assertThat(hit.getString("levelElementDto.vedtak"))
              .as("vedtak")
              .isNotEqualTo("null");
          softly
              .assertThat(hit.getString("levelElementDto.decision"))
              .as("decision")
              .isNotEqualTo("null");
        });
  }

  @Test
  @DisplayName("Health endpoint responds successfully")
  @Description("GET /health responds with a success status")
  void shouldRespondSuccessfullyOnHealthEndpoint() {
    var statusCode = given().get(apiHost + "/health").statusCode();

    assertThat(statusCode).as("/health status code").isBetween(200, 299);
  }

  @Test
  @DisplayName("Database connection check responds")
  @Description("GET /checkdatabaseconnection returns 200 and confirms the connection")
  void shouldConfirmDatabaseConnection() {
    var body =
        given()
            .get(apiHost + "/checkdatabaseconnection")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();

    assertThat(body).startsWith("Connected to the database");
  }

  @Test
  @DisplayName("API root responds with service information")
  @Description("GET / returns 200 and identifies the service")
  void shouldRespondWithServiceInformationOnApiRoot() {
    var body = given().get(apiHost + "/").then().statusCode(200).extract().body().asString();

    assertThat(body).contains("Kanalregister NVA api");
  }

  private JsonPath lookUpChannel(String resource, String pid, int year) {
    return extractJson("%s/%s/%s/%d".formatted(apiHost, resource, pid, year));
  }

  private JsonPath lookUpChannelWithoutYear(String resource, String pid) {
    return extractJson("%s/%s/%s".formatted(apiHost, resource, pid));
  }

  private JsonPath searchChannels(String resource, String parameterName, String value, int year) {
    return given()
        .accept(ContentType.JSON)
        .queryParam(parameterName, value)
        .queryParam("year", year)
        .queryParam("pageno", 0)
        .queryParam("pagecount", SEARCH_PAGE_SIZE)
        .get("%s/%s/channels".formatted(apiHost, resource))
        .then()
        .statusCode(200)
        .extract()
        .jsonPath();
  }

  private static JsonPath extractJson(String absoluteUrl) {
    return given()
        .accept(ContentType.JSON)
        .get(absoluteUrl)
        .then()
        .statusCode(200)
        .extract()
        .jsonPath();
  }

  private static String hitByPid(String pid) {
    return "%s.find { it.pid == '%s' }".formatted(SEARCH_HITS_PATH, pid);
  }
}
