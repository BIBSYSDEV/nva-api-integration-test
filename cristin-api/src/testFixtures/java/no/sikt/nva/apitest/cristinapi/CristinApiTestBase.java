package no.sikt.nva.apitest.cristinapi;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_AFFILIATIONS_PATH;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import io.restassured.RestAssured;
import java.nio.file.Path;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;

public class CristinApiTestBase extends IntegrationTestBase {

  @BeforeAll
  static void initCristinApiTests() {
    RestAssured.baseURI = "https://api.cristin-test.uio.no/v2/";
  }

  public String readResourceFileAsString(String filename) {
    return stringFromResources(Path.of(filename));
  }

  public static String findExistingAffiliationId(String personId, String institutionId) {
    return givenAuthenticatedCristinJsonRequest()
        .when()
        .get(PERSON_AFFILIATIONS_PATH, personId)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath()
        .param("institutionId", institutionId)
        .getString(
            "find { it.affiliation.institution.cristin_institution_id == institutionId }.id");
  }
}
