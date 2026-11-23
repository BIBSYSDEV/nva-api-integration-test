package no.sikt.nva.apitest.cristinapi.person;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedCristinJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.cristinapi.CristinPaths.PERSONS_BASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.cristinapi.CristinApiTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("POST " + PERSONS_BASE_PATH)
class PostPersonApiTest extends CristinApiTestBase {

  private static final String INSTITUTION_ID = "2012";

  /** Create person returns {@code 201 Created} */
  @Test
  @Disabled(
      "Disabled to avoid creating too many persons in cristin test. When running test, replace nin"
          + " with syntetic nin from 'persons/syntetic_nin.txt'")
  @DisplayName("Create person returns person")
  @Description
  void shouldCreatePerson() {
    // synteticNin needs to be unique in Cristin, get value from 'persons/syntetic_nin.txt'
    var synteticNin = "08306211057";
    var createPayload =
        readResourceFileAsString("persons/post_person_payload.json")
            .replace("dummy_nin", synteticNin);

    var newPersonResponse =
        givenAuthenticatedCristinJsonRequest()
            .header("Cristin-Representing-Institution", INSTITUTION_ID)
            .body(createPayload)
            .when()
            .post(PERSONS_BASE_PATH)
            .then()
            .statusCode(HTTP_CREATED)
            .extract()
            .response();

    var expectedResponse =
        readResourceFileAsString("persons/post_person_response.json")
            .replace("dummy_nin", synteticNin);
    assertThatJson(newPersonResponse.body().asString())
        .when(IGNORING_EXTRA_FIELDS)
        .isEqualTo(expectedResponse);
    var cristinId = newPersonResponse.jsonPath().getString("cristin_person_id");
    assertThat(newPersonResponse.header("location")).endsWith(cristinId);
  }

  /** Create person when unauthenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Create person when unauthenticated returns Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhenUnauthenticated() {

    givenUnauthenticatedJsonRequest()
        .when()
        .post(PERSONS_BASE_PATH)
        .then()
        .statusCode(HTTP_UNAUTHORIZED);
  }
}
