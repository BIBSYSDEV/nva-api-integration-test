package no.sikt.nva.apitest.scientificindex.period;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.PERIOD_PATH;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET " + PERIOD_PATH)
class FetchPeriodTest extends ScientificIndexTestBase {

  private static final String NONEXISTENT_PERIOD_YEAR = "1900";

  /** Fetching the current-year period returns it (open-period prerequisite). */
  @Test
  @DisplayName("Fetch period for current year")
  @Description(useJavaDoc = true)
  void shouldReturnPeriodWhenFetchingExistingPeriod(SoftAssertions softly) {
    var response =
        givenUnauthenticatedJsonRequest()
            .get(PERIOD_PATH, CURRENT_YEAR)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("type")).isEqualTo("NviPeriod");
    softly.assertThat(response.getString("publishingYear")).isEqualTo(CURRENT_YEAR);
    softly.assertThat(response.getString("startDate")).isNotEmpty();
    softly.assertThat(response.getString("reportingDate")).isNotEmpty();
  }

  /** Fetching a period that doesn't exist returns status {@code 404 Not Found}. */
  @Test
  @DisplayName("Fetch nonexistent period")
  @Description(useJavaDoc = true)
  void shouldReturnNotFoundWhenPeriodDoesNotExist() {
    givenUnauthenticatedJsonRequest()
        .get(PERIOD_PATH, NONEXISTENT_PERIOD_YEAR)
        .then()
        .statusCode(HTTP_NOT_FOUND);
  }
}
