package no.sikt.nva.apitest.scientificindex.period;

import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.APP_ADMIN;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_DOI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_EDITOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_SUPPORT_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.PERIODS_PATH;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import java.time.Year;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.scientificindex.NviPeriods;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST " + PERIODS_PATH)
class CreatePeriodTest extends ScientificIndexTestBase {

  private static final int FIRST_TEST_YEAR_OFFSET = 10;

  private static final String NEW_PERIOD_YEAR = futureYear(FIRST_TEST_YEAR_OFFSET);
  private static final String UNAUTHENTICATED_PERIOD_YEAR = futureYear(FIRST_TEST_YEAR_OFFSET + 1);
  private static final String INVALID_DATE_PERIOD_YEAR = futureYear(FIRST_TEST_YEAR_OFFSET + 2);
  private static final String NON_ADMIN_PERIOD_YEAR = futureYear(FIRST_TEST_YEAR_OFFSET + 3);

  private static final List<String> TEST_PERIOD_YEARS =
      List.of(
          NEW_PERIOD_YEAR,
          UNAUTHENTICATED_PERIOD_YEAR,
          INVALID_DATE_PERIOD_YEAR,
          NON_ADMIN_PERIOD_YEAR);

  /**
   * Removes periods left behind by an earlier run, which would otherwise be rejected as conflicts.
   */
  @BeforeAll
  static void deleteLeftoverPeriods() {
    TEST_PERIOD_YEARS.forEach(NviPeriods::deletePeriod);
  }

  @AfterAll
  static void deleteCreatedPeriods() {
    TEST_PERIOD_YEARS.forEach(NviPeriods::deletePeriod);
  }

  private static String futureYear(int yearsFromNow) {
    return Integer.toString(Year.now(ZoneId.systemDefault()).plusYears(yearsFromNow).getValue());
  }

  private Map<String, String> createPeriodPayload(String year) {

    var futureYearStartDate = String.format("%s-01-01T01:00:00Z", year);
    var futureYearReportingDate = String.format("%s-12-31T23:59:00Z", year);

    return Map.of(
        "type", "NviPeriod",
        "publishingYear", year,
        "startDate", futureYearStartDate,
        "reportingDate", futureYearReportingDate);
  }

  @Test
  @DisplayName("Create new period")
  @Description(useJavaDoc = true)
  void shouldReturnNewPeriodWhenUserIsAuthenticated(SoftAssertions softly) {
    var payload = createPeriodPayload(NEW_PERIOD_YEAR);

    var response =
        givenAuthenticatedJsonRequestAsUser(APP_ADMIN)
            .body(payload)
            .when()
            .post(PERIODS_PATH)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath();

    var expectedPeriodId = RestAssured.baseURI + "/scientific-index/period/" + NEW_PERIOD_YEAR;
    softly.assertThat(response.getString("id")).isEqualTo(expectedPeriodId);
    softly.assertThat(response.getString("status")).isEqualTo("UnopenedPeriod");
  }

  @Test
  @DisplayName("Create new period unauthenticated")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenUserIsUnauthenticated(SoftAssertions softly) {
    var payload = createPeriodPayload(UNAUTHENTICATED_PERIOD_YEAR);

    givenAuthenticatedJsonRequestAsUser(UIB_EDITOR)
        .body(payload)
        .when()
        .post(PERIODS_PATH)
        .then()
        .statusCode(401);
  }

  @Test
  @DisplayName("Create new period already exists")
  @Description(useJavaDoc = true)
  void shouldReturnErrorWhenTryingToCreateExistingPeriod(SoftAssertions softly) {
    var payload = createPeriodPayload(CURRENT_YEAR);

    var response =
        givenAuthenticatedJsonRequestAsUser(APP_ADMIN)
            .body(payload)
            .when()
            .post(PERIODS_PATH)
            .then()
            .statusCode(400)
            .extract()
            .jsonPath();

    softly
        .assertThat(response.getString("detail"))
        .isEqualTo(String.format("Period with publishing year %s already exists!", CURRENT_YEAR));
  }

  @Test
  @DisplayName("Create new period wrong date format")
  @Description(useJavaDoc = true)
  void shouldReturnErrorWhenWrongDateFormat(SoftAssertions softly) {
    var payload = createPeriodPayload(INVALID_DATE_PERIOD_YEAR);
    var modifiedPayload = new HashMap<>(payload);
    modifiedPayload.put(
        "reportingDate", String.format("%s-31-121T23:59:00Z", INVALID_DATE_PERIOD_YEAR));

    var response =
        givenAuthenticatedJsonRequestAsUser(APP_ADMIN)
            .body(modifiedPayload)
            .when()
            .post(PERIODS_PATH)
            .then()
            .statusCode(400)
            .extract()
            .jsonPath();

    softly
        .assertThat(response.getString("detail"))
        .contains(String.format("%s-31-121T23:59:00Z", INVALID_DATE_PERIOD_YEAR));
  }

  @Test
  @DisplayName("Create new period overlapping already existing period")
  @Disabled("Not implemented")
  @Description(useJavaDoc = true)
  void shouldReturnErrorWhenTryingToCreateOverlappingPeriod(SoftAssertions softly) {

    var yearMinusTwo = Integer.toString(Year.now(ZoneId.systemDefault()).minusYears(2).getValue());
    var yearMinusOne = Integer.toString(Year.now(ZoneId.systemDefault()).minusYears(1).getValue());

    var payload = createPeriodPayload(yearMinusTwo);
    var modifiedPayload = new HashMap<>(payload);
    modifiedPayload.put("reportingDate", String.format("%s-06-01T23:59:00Z", yearMinusOne));

    givenAuthenticatedJsonRequestAsUser(APP_ADMIN)
        .body(modifiedPayload)
        .when()
        .post(PERIODS_PATH)
        .then()
        .statusCode(400)
        .extract()
        .jsonPath();
  }

  private static Stream<Arguments> userByRoleProvider() {
    return Stream.of(
        argumentSet("Registrar", UIB_CREATOR),
        argumentSet("Nvi-curator", UIB_NVI_CURATOR),
        argumentSet("DOI-curator", UIB_DOI_CURATOR),
        argumentSet("Publishing-curator", UIB_PUBLISHING_CURATOR),
        argumentSet("Support curator", UIB_SUPPORT_CURATOR),
        argumentSet("Editor", UIB_EDITOR));
  }

  @ParameterizedTest
  @MethodSource("userByRoleProvider")
  @DisplayName("Create new period user is not AppAdmin")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenCreatorNotAppAdmin(User user, SoftAssertions softly) {
    var payload = createPeriodPayload(NON_ADMIN_PERIOD_YEAR);

    givenAuthenticatedJsonRequestAsUser(user)
        .body(payload)
        .when()
        .post(PERIODS_PATH)
        .then()
        .log()
        .all()
        .statusCode(401);
  }
}
