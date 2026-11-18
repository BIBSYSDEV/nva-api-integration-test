package no.sikt.nva.apitest.cristinapi.person;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.AFFILIATION_PATH;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_AFFILIATIONS_PATH;

@DisplayName ("PATCH " + PERSON_AFFILIATIONS_PATH)
class PatchPersonsAffiliationsApiTest extends CristinApiTestBase {

  private static final String PERSON_ID = "43419";
  private static final String INSTITUTION_ID = "192";

  private static final String AFFILIATION_ID = findExistingAffiliationId(PERSON_ID, INSTITUTION_ID);

  private float readFtePercentage(String personId, String institutionId) {
    return givenAuthenticatedCristinJsonRequest()
    .when()
    .get(PERSON_AFFILIATIONS_PATH, personId)
    .then()
    .statusCode(HTTP_OK)
    .extract()
    .jsonPath()
    .param("institutionId", institutionId)
    .getFloat("find { it.affiliation.institution.cristin_institution_id == institutionId }.fte_percentage");
  }

  /** Patch person affiliation updates person affiliation and returns {@code 204 No Content} */
  @Test
  @DisplayName ("Patch person affiliation updates person affiliation and returns No Content")
  @Description 
  void shouldUpdatePersonAffiliation() {

    var currentPercentage = readFtePercentage(PERSON_ID, INSTITUTION_ID);
    var percentageChange = currentPercentage == 100f ? 50f : 100f;

    var changePayload = Map.of("fte_percentage", percentageChange);

    givenAuthenticatedCristinJsonRequest()
    .contentType("application/merge-patch+json")
    .header("Cristin-Representing-Institution", INSTITUTION_ID)
    .body(changePayload)
    .when()
    .patch(AFFILIATION_PATH, PERSON_ID, AFFILIATION_ID)
    .then()
    .statusCode(HTTP_NO_CONTENT);

    var changedPercentage = readFtePercentage(PERSON_ID, INSTITUTION_ID);
    assertThat(changedPercentage).isEqualTo(percentageChange);
  }

}
