package no.sikt.nva.apitest.scientificindex.period;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.PERIODS_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET " + PERIODS_PATH)
class ListPeriodsTest extends ScientificIndexTestBase {

  /** Listing periods returns all periods with status {@code 200 OK}. */
  // FIXME: See NP-51333
  @Test
  @Disabled("Bug: Requires MANAGE_NVI access right (See NP-51333)")
  @DisplayName("List periods")
  @Description(useJavaDoc = true)
  void shouldReturnPeriodsWhenUserIsAuthenticated() {
    var response =
        givenAuthenticatedJsonRequestAsUser(UIB_CONTRIBUTOR)
            .get(PERIODS_PATH)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    assertThat(response.getList("periods.publishingYear", String.class)).contains(CURRENT_YEAR);
  }

  /** Listing periods without authentication returns status {@code 401 Unauthorized}. */
  @Test
  @DisplayName("List periods unauthenticated")
  @Description(useJavaDoc = true)
  void shouldReturnUnauthorizedWhenUnauthenticated() {
    givenUnauthenticatedJsonRequest().get(PERIODS_PATH).then().statusCode(HTTP_UNAUTHORIZED);
  }
}
