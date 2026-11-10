package no.sikt.nva.apitest.cristinapi.person;

import static java.net.HttpURLConnection.HTTP_OK;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_POSITIONS_PATH;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetPersonPositionsApiTest extends CristinApiTestBase {

  private static final String EXPECTED_POSITIONS_FILE = "persons/person_positions_response.json";

  /** Get person positions returns list of positions and status {@code 200 Ok} */
  @Test
  @DisplayName("Get person positions returns list of positions")
  @Description
  void shouldReturnPersonPositions() {

    var positionsResponse =
        givenAuthenticatedCristinJsonRequest()
            .when()
            .get(PERSON_POSITIONS_PATH)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .body()
            .asString();

    var expected = readResourceFileAsString(EXPECTED_POSITIONS_FILE);
    assertThatJson(positionsResponse).when(IGNORING_EXTRA_ARRAY_ITEMS).isEqualTo(expected);
  }
}
