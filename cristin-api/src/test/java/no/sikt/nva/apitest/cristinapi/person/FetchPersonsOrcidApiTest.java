package no.sikt.nva.apitest.cristinapi.person;

import static java.net.HttpURLConnection.HTTP_OK;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_ORCID_PATH;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FetchPersonsOrcidApiTest extends CristinApiTestBase {

  private static final String EXPECTED_ORCID_FILE = "persons/persons_orcid_response.json";

  /** Get person with orcid returns list of persons with orcid and status {@code 200 Ok} */
  @Test
  @DisplayName("Get person with orcid returns list of persons with orcid")
  @Description
  void shouldReturnPersonsWithOrcid() {

    var positionsResponse =
        givenAuthenticatedCristinJsonRequest()
            .when()
            .get(PERSON_ORCID_PATH)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .body()
            .asString();

    var expected = readResourceFileAsString(EXPECTED_ORCID_FILE);
    assertThatJson(positionsResponse).when(IGNORING_EXTRA_ARRAY_ITEMS).isEqualTo(expected);
  }
}
