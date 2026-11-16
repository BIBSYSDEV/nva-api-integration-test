package no.sikt.nva.apitest.cristinapi.person;

import static java.net.HttpURLConnection.HTTP_OK;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import no.sikt.nva.apitest.base.Requests;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import no.sikt.nva.apitest.cristinapi.CristinPaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GET " + CristinPaths.PERSON_AFFILIATIONS_PATH)
class GetPersonAffiliationsApiTest extends CristinApiTestBase {

  @Test
  void shouldReturnAffiliationsForPerson() {
    var affilationsResponse =
        Requests.givenAuthenticatedCristinJsonRequest()
            .when()
            .get(CristinPaths.PERSON_AFFILIATIONS_PATH, "43419")
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .body()
            .asString();

    var expected = readResourceFileAsString("persons/person_affiliations_response.json");
    assertThatJson(affilationsResponse).isEqualTo(expected);
  }
}
