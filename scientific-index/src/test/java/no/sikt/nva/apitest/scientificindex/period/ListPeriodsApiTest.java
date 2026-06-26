package no.sikt.nva.apitest.scientificindex.period;

import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.listPeriodsPath;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class ListPeriodsApiTest extends ScientificIndexTestBase {

  @Test
  @Disabled(
      "Requires an api-test user with the MANAGE_NVI access right, which is not yet provisioned."
          + " Replace UIB_NVI_CURATOR with the NVI admin user when available.")
  @DisplayName("List periods as NVI admin")
  @Description(
      "Listing NVI periods as a user with the MANAGE_NVI access right should return all periods"
          + " and statuscode 200 OK")
  void shouldReturnPeriodsWhenUserHasManageNviAccessRight() {
    var response =
        givenAuthenticatedJsonRequestAsUser(UIB_NVI_CURATOR)
            .get(listPeriodsPath())
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    assertThat(response.getList("periods.publishingYear", String.class)).contains(CURRENT_YEAR);
  }

  @Test
  @DisplayName("List periods without MANAGE_NVI access right")
  @Description(
      "Listing NVI periods as a user without the MANAGE_NVI access right should return statuscode"
          + " 401 Unauthorized")
  void shouldReturnUnauthorizedWhenUserLacksManageNviAccessRight() {
    givenAuthenticatedJsonRequestAsUser(UIB_NVI_CURATOR)
        .get(listPeriodsPath())
        .then()
        .statusCode(401);
  }

  @Test
  @DisplayName("List periods unauthenticated")
  @Description(
      "Listing NVI periods without authentication should return statuscode 401 Unauthorized")
  void shouldReturnUnauthorizedWhenUnauthenticated() {
    givenUnauthenticatedJsonRequest().get(listPeriodsPath()).then().statusCode(401);
  }
}
