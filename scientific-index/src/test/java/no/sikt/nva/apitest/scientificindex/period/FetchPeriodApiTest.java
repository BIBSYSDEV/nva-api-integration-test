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
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class FetchPeriodApiTest extends ScientificIndexTestBase {

  private static final String NONEXISTENT_PERIOD_YEAR = "1900";

  @InjectSoftAssertions private SoftAssertions softly;

  @Test
  @DisplayName("Fetch period for current year")
  @Description(
      "Fetching the NVI period for the current year without authentication should return the"
          + " period and statuscode 200 OK. This also verifies the open-period prerequisite for"
          + " the candidate tests.")
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

  @Test
  @DisplayName("Fetch nonexistent period")
  @Description("Fetching an NVI period that does not exist should return statuscode 404 Not Found")
  void shouldReturnNotFoundWhenPeriodDoesNotExist() {
    givenUnauthenticatedJsonRequest()
        .get(periodPath(NONEXISTENT_PERIOD_YEAR))
        .then()
        .statusCode(404);
  }
}
