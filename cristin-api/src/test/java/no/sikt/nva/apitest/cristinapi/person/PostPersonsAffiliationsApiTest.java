package no.sikt.nva.apitest.cristinapi.person;

import static io.restassured.http.Method.POST;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSON_AFFILIATIONS_PATH;

import io.qameta.allure.Description;
import java.util.Map;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("POST " + PERSON_AFFILIATIONS_PATH)
class PostPersonsAffiliationsApiTest extends CristinApiTestBase {

  private static final String PERSON_ID = "43419";
  private static final String INSTIUTION_ID = "2012";

  @BeforeAll
  static void cleanup() {

    var affiliationsId = findExistingAffiliationId(PERSON_ID, INSTIUTION_ID);

    givenAuthenticatedCristinJsonRequest()
        .header("Cristin-Representing-Institution", INSTIUTION_ID)
        .when()
        .delete(PERSON_AFFILIATIONS_PATH + "/{uid}", PERSON_ID, affiliationsId);
  }

  private static String findExistingAffiliationId(String personId, String institutionId) {
    var affiliationsResponse =
        givenAuthenticatedCristinJsonRequest()
            .when()
            .get(PERSON_AFFILIATIONS_PATH, personId)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    var personAffiliation =
        affiliationsResponse.getList("").stream()
            .filter(
                affiliationItem -> {
                  var affiliation = ((Map<String, Object>) affiliationItem).get("affiliation");
                  var institution = ((Map<String, Object>) affiliation).get("institution");
                  return institutionId.equals(
                      ((Map<String, String>) institution).get("cristin_institution_id"));
                })
            .toList()
            .getFirst();
    return ((Map<String, Object>) personAffiliation).get("id").toString();
  }

  /**
   * Adding an existing affiliation to a person returns the affiliation and status {@code 201
   * Created}
   */
  @Test
  @DisplayName("Adding an existing affiliation to a person returns the affiliation")
  @Description
  void shouldAddAffiliation() {

    var createPayload = readResourceFileAsString("persons/create_affiliation_payload.json");
    var createResponse =
        givenAuthenticatedCristinJsonRequest()
            .header("Cristin-Representing-Institution", INSTIUTION_ID)
            .when()
            .body(createPayload)
            .post(PERSON_AFFILIATIONS_PATH, PERSON_ID)
            .then()
            .statusCode(HTTP_CREATED)
            .extract()
            .body()
            .asString();

    var expectedResponse = readResourceFileAsString("persons/create_affiliation_response.json");
    assertThatJson(createResponse).when(IGNORING_EXTRA_FIELDS).isEqualTo(expectedResponse);
  }

  /** Trying to add an existing affiliation to a person return {@code 400 Bad Request} */
  @Test
  @DisplayName("Trying to add an existing affiliation to a person return Bad Request")
  @Description
  void shouldReturnBadRequestWhenTryingToAddExistingAffiliationToPerson() {

    var existingInstitutionId = "194";
    var existingAffiliationPayload =
        readResourceFileAsString("persons/existing_affiliation_payload.json");
    givenAuthenticatedCristinJsonRequest()
        .header("Cristin-Representing-Institution", existingInstitutionId)
        .when()
        .body(existingAffiliationPayload)
        .post(PERSON_AFFILIATIONS_PATH, PERSON_ID)
        .then()
        .statusCode(HTTP_BAD_REQUEST);
  }

  /**
   * Trying to add affiliation to a person when not authenticated returns {@code 401 Unauthorized}
   */
  @Test
  @DisplayName("Trying to add affiliation to a person when not authenticated returns Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhenNotAuthenticated() {
    requestShouldReturnUnauthorized(POST, PERSON_AFFILIATIONS_PATH, PERSON_ID);
  }
}
