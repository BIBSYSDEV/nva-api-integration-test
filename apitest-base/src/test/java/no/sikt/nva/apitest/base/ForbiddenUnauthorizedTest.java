package no.sikt.nva.apitest.base;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import org.junit.jupiter.params.provider.MethodSource;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.http.Method;
import static io.restassured.http.Method.DELETE;
import static io.restassured.http.Method.GET;
import static io.restassured.http.Method.PATCH;
import static io.restassured.http.Method.POST;
import static io.restassured.http.Method.PUT;
import no.sikt.nva.apitest.base.RemainingArguments.PathParams;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;

@ExtendWith(SoftAssertionsExtension.class)
class ForbiddenUnauthorizedTest extends IntegrationTestBase {

  private static final String PRIMARY_ID = "123456";
  private static final String SECONDARY_ID = "654321";

  static Stream<Arguments> endpoints() {
    return Stream.of(
      // argumentSet("POST /customer", POST, "/customer", UIB_CREATOR),
      argumentSet("GET /customer/{identifier}", GET, "/customer/{identifier}", UIB_CREATOR, SECONDARY_ID),
      argumentSet("PUT /customer/{identifier}", PUT, "/customer/{identifier}", UIB_CREATOR, SECONDARY_ID),
      // argumentSet("GET /customer/{identifier}/doiagent", GET, "/customer/{identifier}/doiagent", UIB_CREATOR, SECONDARY_ID),
      // argumentSet("PUT /customer/{identifier}/doiagent", PUT, "/customer/{identifier}/doiagent", UIB_CREATOR, SECONDARY_ID),
      // argumentSet("GET /customer/{identifier}/vocabularies", GET, "/customer/{identifier}/vocabularies", UIB_CREATOR, SECONDARY_ID),
      // argumentSet("POST /customer/{identifier}/vocabularies", POST, "/customer/{identifier}/vocabularies", UIB_CREATOR, SECONDARY_ID),
      // argumentSet("PUT /customer/{identifier}/vocabularies", PUT, "/customer/{identifier}/vocabularies", UIB_CREATOR, SECONDARY_ID),
      // argumentSet("POST /customer/{identifier}/channel-claim", POST, "/customer/{identifier}/channel-claim", UIB_CREATOR, SECONDARY_ID),
      // argumentSet("DELETE /customer/{identifier}/channel-claim/{claimIdentifier}", DELETE, "/customer/{identifier}/channel-claim/{claimIdentifier}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      argumentSet("GET /customer/channel-claims", GET, "/customer/channel-claims", UIB_CREATOR),
      argumentSet("GET /customer/orgDomain/{orgDomain}", GET, "/customer/orgDomain/{orgDomain}", UIB_CREATOR, SECONDARY_ID),
      argumentSet("GET /customer/cristinId/{cristinId}", GET, "/customer/cristinId/{cristinId}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /cristin/project", POST, "/cristin/project", UIB_CREATOR),
      // argumentSet("POST /cristin/keywords", POST, "/cristin/keywords", UIB_CREATOR),
      argumentSet("POST /cristin/person", POST, "/cristin/person", UIB_CREATOR),
      argumentSet("PATCH /cristin/person/{id}", PATCH, "/cristin/person/{id}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /cristin/person/identityNumber", POST, "/cristin/person/identityNumber", UIB_CREATOR),
      // argumentSet("GET /cristin/person/{id}/organization/{orgId}", GET, "/cristin/person/{id}/organization/{orgId}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("PATCH /cristin/person/{id}/organization/{orgId}", PATCH, "/cristin/person/{id}/organization/{orgId}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("GET /cristin/person/{id}/employment", GET, "/cristin/person/{id}/employment", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /cristin/person/{id}/employment", POST, "/cristin/person/{id}/employment", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("DELETE /cristin/person/{id}/employment/{employmentId}", DELETE, "/cristin/person/{id}/employment/{employmentId}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("PATCH /cristin/person/{id}/employment/{employmentId}", PATCH, "/cristin/person/{id}/employment/{employmentId}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("PUT /cristin/person/{id}/picture", PUT, "/cristin/person/{id}/picture", UIB_CREATOR, PRIMARY_ID),
      argumentSet("POST /approval", POST, "/approval", UIB_CREATOR)
      // argumentSet("POST /approval/{approvalId}", POST, "/approval/{approvalId}", UIB_CREATOR, SECONDARY_ID)
    );
  }

  @ParameterizedTest
  @MethodSource("endpoints")
  void testForbidden(Method method, String path, User forbiddenUser,
                     @PathParams List<Object> pathParams) {

    var get = GET;
    var delete = DELETE;
    var post = POST;
    var put = PUT;
    var patch = PATCH;

    var logConfig =
        LogConfig.logConfig()
            .blacklistHeaders(List.of("Authorization"));
    RestAssured.config = RestAssured.config().logConfig(logConfig);


    givenAuthenticatedRequestAsUser(forbiddenUser)
    .when()
    .request(method, path, pathParams.toArray())
    .then()
    .log()
    .all()
    .statusCode(HTTP_FORBIDDEN);
  }
}
