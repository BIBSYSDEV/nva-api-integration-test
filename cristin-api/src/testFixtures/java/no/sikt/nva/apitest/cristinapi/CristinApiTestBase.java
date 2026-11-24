package no.sikt.nva.apitest.cristinapi;

import static java.net.HttpURLConnection.HTTP_OK;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;

import io.restassured.RestAssured;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_AFFILIATIONS_PATH;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

public class CristinApiTestBase extends IntegrationTestBase {

  public static final String CRISTIN_REPRESENTING_INSTITUTION = "Cristin-Representing-Institution";
  public static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";

  @BeforeAll
  static void initCristinApiTests() {
    RestAssured.baseURI = "https://api.cristin-test.uio.no/v2/";
  }

  public static String readResourceFileAsString(String filename) {
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
