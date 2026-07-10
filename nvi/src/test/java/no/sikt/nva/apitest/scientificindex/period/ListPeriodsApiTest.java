package no.sikt.nva.apitest.scientificindex.period;

import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.listPeriodsPath;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListPeriodsApiTest extends ScientificIndexTestBase {

  // FIXME: See NP-51333
  @Test
  @Disabled("Bug: Requires MANAGE_NVI access right (See NP-51333)")
  @DisplayName("List periods")
  @Description("Listing periods returns all periods with 200 OK")
  void shouldReturnPeriodsWhenUserIsAuthenticated() {
    var response =
        givenAuthenticatedJsonRequestAsUser(UIB_CONTRIBUTOR)
            .get(listPeriodsPath())
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    assertThat(response.getList("periods.publishingYear", String.class)).contains(CURRENT_YEAR);
  }

  @Test
  @DisplayName("List periods unauthenticated")
  @Description("Listing periods without authentication returns 401 Unauthorized")
  void shouldReturnUnauthorizedWhenUnauthenticated() {
    givenUnauthenticatedJsonRequest().get(listPeriodsPath()).then().statusCode(401);
  }
}
