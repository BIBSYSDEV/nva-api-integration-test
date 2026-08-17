package no.sikt.nva.apitest.scientificindex.period;

import java.time.Year;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.APP_ADMIN;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_EDITOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.PERIODS_PATH;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST " + PERIODS_PATH)
public class CreatePeriodTest extends ScientificIndexTestBase {

  private int YEAR_OFFSET = 10;

  private Map<String, Object> createPeriodPayload(String year) {

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
  public void shouldReturnNewPeriodWhenUserIsAuthenticated(SoftAssertions softly) {

    var futureYear = Integer.toString(Year.now(ZoneId.systemDefault()).plusYears(YEAR_OFFSET).getValue());

    Map<String, Object> payload = createPeriodPayload(futureYear);

    var response =
        givenAuthenticatedJsonRequestAsUser(APP_ADMIN)
            .body(payload)
            .when()
            .post(PERIODS_PATH)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath();

    softly
        .assertThat(response.getString("id"))
        .isEqualTo(
            String.format(
                "https://api.e2e.nva.aws.unit.no/scientific-index/period/%s", futureYear));
    softly.assertThat(response.getString("status")).isEqualTo("UnopenedPeriod");
  }

  @Test
  @DisplayName("Create new period unauthenticated")
  @Description(useJavaDoc = true)
  public void shouldReturnUnauthorizedWhenUserIsUnauthenticated(SoftAssertions softly) {

    var futureYear = Integer.toString(Year.now(ZoneId.systemDefault()).plusYears(YEAR_OFFSET + 1).getValue());

    Map<String, Object> payload = createPeriodPayload(futureYear);

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
  public void shouldReturnErrorWhenTryingToCreateExistingPeriod(SoftAssertions softly) {

    var year = Integer.toString(Year.now(ZoneId.systemDefault()).getValue());

    Map<String, Object> payload = createPeriodPayload(year);

    var response = givenAuthenticatedJsonRequestAsUser(APP_ADMIN)
        .body(payload)
        .when()
        .post(PERIODS_PATH)
        .then()
        .statusCode(400)
        .extract()
        .jsonPath();

        softly.assertThat(response.getString("detail")).isEqualTo(String.format("Period with publishing year %s already exists!", year));
  }

  @Test
  @DisplayName("Create new period wrong date format")
  @Description(useJavaDoc = true)
  public void shouldReturnErrorWhenWrongDateFormat(SoftAssertions softly) {

    var year = Integer.toString(Year.now(ZoneId.systemDefault()).plusYears(YEAR_OFFSET + 2).getValue());

    Map<String, Object> payload = createPeriodPayload(year);
    var modifiedPayload = new HashMap<>(payload);
    modifiedPayload.put("reportingDate", String.format("%s-31-121T23:59:00Z", year));

    var response = givenAuthenticatedJsonRequestAsUser(APP_ADMIN)
        .body(modifiedPayload)
        .when()
        .post(PERIODS_PATH)
        .then()
        .log().all()
        .statusCode(400)
        .extract()
        .jsonPath();

        softly.assertThat(response.getString("detail")).contains(String.format("%s-31-121T23:59:00Z", year));
  }

  @Test
  @DisplayName("Create new period overlapping already existing period")
  @Disabled("Not implemented")
  @Description(useJavaDoc = true)
  public void shouldReturnErrorWhenTryingToCreateOverlappingPeriod(SoftAssertions softly) {

    var yearMinusTwo = Integer.toString(Year.now(ZoneId.systemDefault()).minusYears(2).getValue());
    var yearMinusOne = Integer.toString(Year.now(ZoneId.systemDefault()).minusYears(1).getValue());

    Map<String, Object> payload = createPeriodPayload(yearMinusTwo);
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
}
