package no.sikt.nva.apitest.scientificindex.period;

import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.periodPath;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class FetchPeriodApiTest extends ScientificIndexTestBase {

  private static final String NONEXISTENT_PERIOD_YEAR = "1900";

  @InjectSoftAssertions private SoftAssertions softly;

  /** Fetching the current-year period returns it (open-period prerequisite). */
  @Test
  @DisplayName("Fetch period for current year")
  @Description(useJavaDoc = true)
  void shouldReturnPeriodWhenFetchingExistingPeriod() {
    var response =
        givenUnauthenticatedJsonRequest()
            .get(periodPath(CURRENT_YEAR))
            .then()
            .statusCode(200)
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
        .get(periodPath(NONEXISTENT_PERIOD_YEAR))
        .then()
        .statusCode(404);
  }
}
