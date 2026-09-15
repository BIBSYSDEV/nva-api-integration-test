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
  private static final String TERTIARY_ID = "321456";

  static Stream<Arguments> endpoints() {
    return Stream.of(
      
      
      
      
      
      
      
      
      argumentSet("POST /orcid/", POST, "/orcid/", UIB_CREATOR)
      
      
      
      // argumentSet("POST /scientific-index/candidate/{candidateIdentifier}/note", POST, "/scientific-index/candidate/{candidateIdentifier}/note", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("GET /scientific-index/reports", GET, "/scientific-index/reports", UIB_CREATOR),
      // argumentSet("GET /scientific-index/reports/{period}/institutions/{institution}", GET, "/scientific-index/reports/{period}/institutions/{institution}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("GET /scientific-index/candidate/{candidateIdentifier}", GET, "/scientific-index/candidate/{candidateIdentifier}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("GET /scientific-index/institution-report/{year}", GET, "/scientific-index/institution-report/{year}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("GET /scientific-index/candidate/{candidateIdentifier}", GET, "GET /scientific-index/candidate/{candidateIdentifier}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("PUT /scientific-index/candidate/{candidateIdentifier}/assignee", PUT, "/scientific-index/candidate/{candidateIdentifier}/assignee", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("GET /scientific-index/institution-approval-report/{year}", GET, "/scientific-index/institution-approval-report/{year}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("GET /scientific-index/reports/{period}", GET, "/scientific-index/reports/{period}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("GET /scientific-index/reports/{period}/institutions", GET, "/scientific-index/reports/{period}/institutions", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("GET /scientific-index/period", GET, "/scientific-index/period", UIB_CREATOR),
      // argumentSet("PUT /scientific-index/period", PUT, "/scientific-index/period", UIB_CREATOR),
      // argumentSet("POST /scientific-index/period", POST, "/scientific-index/period", UIB_CREATOR),
      // argumentSet("GET /scientific-index/candidate", GET, "/scientific-index/candidate", UIB_CREATOR),
      // argumentSet("PUT /scientific-index/candidate/{candidateIdentifier}/status", PUT, "/scientific-index/candidate/{candidateIdentifier}/status", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("DELETE /scientific-index/candidate/{candidateIdentifier}/note/{noteIdentifier}", DELETE, "/scientific-index/candidate/{candidateIdentifier}/note/{noteIdentifier}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID)

      // argumentSet("POST /users-roles/impersonation/stop", POST, "/users-roles/impersonation/stop", UIB_CREATOR),
      // argumentSet("PUT /users-roles/users/mine/accepted-terms", PUT, "/users-roles/users/mine/accepted-terms", UIB_CREATOR),
      // argumentSet("POST /users-roles/users", POST, "/users-roles/users", UIB_CREATOR),
      // argumentSet("POST /users-roles/login", POST, "/users-roles/login", UIB_CREATOR),
      // argumentSet("GET /users-roles/roles/{role}", GET, "/users-roles/roles/{role}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("GET /users-roles/external-clients/{clientId}", GET, "/users-roles/external-clients/{clientId}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /users-roles/impersonation", POST, "/users-roles/impersonation", UIB_CREATOR),
      // argumentSet("GET /users-roles/institutions/users", GET, "/users-roles/institutions/users", UIB_CREATOR),
      // argumentSet("GET /users-roles/users/{username}", GET, "/users-roles/users/{username}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("PUT /users-roles/users/{username}", PUT, "/users-roles/users/{username}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("GET /users-roles/external-clients", GET, "/users-roles/external-clients", UIB_CREATOR),
      // argumentSet("POST /users-roles/external-clients", POST, "/users-roles/external-clients", UIB_CREATOR)

      // argumentSet("POST /publication-channels-v2/series", POST, "/publication-channels-v2/series", UIB_CREATOR),
      // argumentSet("PUT /publication-channels-v2/publisher/{identifier}", PUT, "/publication-channels-v2/publisher/{identifier}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /publication-channels-v2/journal", POST, "/publication-channels-v2/journal", UIB_CREATOR),
      // argumentSet("PUT /publication-channels-v2/serial-publication/{identifier}", PUT, "/publication-channels-v2/serial-publication/{identifier}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /publication-channels-v2/serial-publication", POST, "/publication-channels-v2/serial-publication", UIB_CREATOR),
      // argumentSet("POST /publication-channels-v2/publisher", POST, "/publication-channels-v2/publisher", UIB_CREATOR),
      // argumentSet("DELETE /publication-channels-v2/channel/{identifier}", DELETE, "/publication-channels-v2/channel/{identifier}", UIB_CREATOR, PRIMARY_ID)

      // argumentSet("POST /publication/{publicationIdentifier}/publish", POST, "/publication/{publicationIdentifier}/publish", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /publication/{publicationIdentifier}/file-upload/listparts", POST, "/publication/{publicationIdentifier}/file-upload/listparts", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /publication/{publicationIdentifier}/doi", POST, "/publication/{publicationIdentifier}/doi", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /publication/{publicationIdentifier}/file-upload/complete", POST, "/publication/{publicationIdentifier}/file-upload/complete", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /publication/{publicationIdentifier}/file/{fileIdentifier}", POST, "POST /publication/{publicationIdentifier}/file/{fileIdentifier}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("DELETE /publication/{publicationIdentifier}/file/{fileIdentifier}", DELETE, "/publication/{publicationIdentifier}/file/{fileIdentifier}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("POST /publication/{publicationIdentifier}/ticket", POST, "/publication/{publicationIdentifier}/ticket", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("GET /publication/{publicationIdentifier}/log", GET, "/publication/{publicationIdentifier}/log", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("GET /publication/{publicationIdentifier}/tickets", GET, "/publication/{publicationIdentifier}/tickets", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /publication/{publicationIdentifier}/ticket/{ticketIdentifier}/message", POST, "/publication/{publicationIdentifier}/ticket/{ticketIdentifier}/message", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("POST /publication/", POST, "/publication/", UIB_CREATOR),
      // argumentSet("POST /publication/{publicationIdentifier}/file-upload/create", POST, "/publication/{publicationIdentifier}/file-upload/create", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("PUT /publication/{publicationIdentifier}", PUT, "/publication/{publicationIdentifier}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("DELETE /publication/{publicationIdentifier}", DELETE, "/publication/{publicationIdentifier}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /publication/{publicationIdentifier}/file-upload/prepare", POST, "/publication/{publicationIdentifier}/file-upload/prepare", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("DELETE /publication/{publicationIdentifier}/ticket/{ticketIdentifier}/message/{messageIdentifier}", DELETE, "/publication/{publicationIdentifier}/ticket/{ticketIdentifier}/message/{messageIdentifier}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID, TERTIARY_ID),
      // argumentSet("GET /publication/by-owner", GET, "/publication/by-owner", UIB_CREATOR),
      // argumentSet("POST /publication/{publicationIdentifier}/file-upload/abort", POST, "/publication/{publicationIdentifier}/file-upload/abort", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("GET /publication/{publicationIdentifier}/ticket/{ticketIdentifier}", GET, "/publication/{publicationIdentifier}/ticket/{ticketIdentifier}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("PUT /publication/{publicationIdentifier}/ticket/{ticketIdentifier}", PUT, "/publication/{publicationIdentifier}/ticket/{ticketIdentifier}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("DELETE /publication/{publicationIdentifier}/ticket/{ticketIdentifier}", DELETE, "/publication/{publicationIdentifier}/ticket/{ticketIdentifier}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID)

      // argumentSet("GET /search/customer/import-candidates", GET, "/search/customer/import-candidates", UIB_CREATOR),
      // argumentSet("GET /search/customer/resources", GET, "/search/customer/resources", UIB_CREATOR),
      // argumentSet("GET /search/customer/tickets", POST, "/search/customer/tickets", UIB_CREATOR),
      // argumentSet("GET /search/user/resources", POST, "/handle/", UIB_CREATOR)

      // argumentSet("PUT /person-preferences/{cristinId+}", PUT, "/person-preferences/{cristinId}", UIB_CREATOR, PRIMARY_ID),

      // argumentSet("POST /handle/", POST, "/handle/", UIB_CREATOR),
      // argumentSet("PUT /handle/{prefix}/{suffix}", PUT, "/handle/{prefix}/{suffix}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID)

      // argumentSet("POST /doi-fetch", POST, "/doi-fetch", UIB_CREATOR),
      // argumentSet("POST /doi-fetch/preview/", POST, "/doi-fetch/preview/", UIB_CREATOR),
      // argumentSet("POST /doi-registrar/draft/{doiPrefix}/{doiSuffix}", POST, "/doi-registrar/draft/{doiPrefix}/{doiSuffix}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("DELETE /doi-registrar/draft/{doiPrefix}/{doiSuffix}", DELETE, "/doi-registrar/draft/{doiPrefix}/{doiSuffix}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("POST /doi-registrar/findable", POST, "/doi-registrar/findable", UIB_CREATOR)

      // argumentSet("POST /customer", POST, "/customer", UIB_CREATOR),
      // argumentSet("GET /customer/{identifier}", GET, "/customer/{identifier}", UIB_CREATOR, SECONDARY_ID),
      // argumentSet("PUT /customer/{identifier}", PUT, "/customer/{identifier}", UIB_CREATOR, SECONDARY_ID),
      // // argumentSet("GET /customer/{identifier}/doiagent", GET, "/customer/{identifier}/doiagent", UIB_CREATOR, SECONDARY_ID),
      // // argumentSet("PUT /customer/{identifier}/doiagent", PUT, "/customer/{identifier}/doiagent", UIB_CREATOR, SECONDARY_ID),
      // // argumentSet("GET /customer/{identifier}/vocabularies", GET, "/customer/{identifier}/vocabularies", UIB_CREATOR, SECONDARY_ID),
      // // argumentSet("POST /customer/{identifier}/vocabularies", POST, "/customer/{identifier}/vocabularies", UIB_CREATOR, SECONDARY_ID),
      // // argumentSet("PUT /customer/{identifier}/vocabularies", PUT, "/customer/{identifier}/vocabularies", UIB_CREATOR, SECONDARY_ID),
      // // argumentSet("POST /customer/{identifier}/channel-claim", POST, "/customer/{identifier}/channel-claim", UIB_CREATOR, SECONDARY_ID),
      // // argumentSet("DELETE /customer/{identifier}/channel-claim/{claimIdentifier}", DELETE, "/customer/{identifier}/channel-claim/{claimIdentifier}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("GET /customer/channel-claims", GET, "/customer/channel-claims", UIB_CREATOR),
      // argumentSet("GET /customer/orgDomain/{orgDomain}", GET, "/customer/orgDomain/{orgDomain}", UIB_CREATOR, SECONDARY_ID),
      // argumentSet("GET /customer/cristinId/{cristinId}", GET, "/customer/cristinId/{cristinId}", UIB_CREATOR, PRIMARY_ID),

      // // argumentSet("POST /cristin/project", POST, "/cristin/project", UIB_CREATOR),
      // // argumentSet("POST /cristin/keywords", POST, "/cristin/keywords", UIB_CREATOR),
      // argumentSet("POST /cristin/person", POST, "/cristin/person", UIB_CREATOR),
      // argumentSet("PATCH /cristin/person/{id}", PATCH, "/cristin/person/{id}", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /cristin/person/identityNumber", POST, "/cristin/person/identityNumber", UIB_CREATOR),
      // argumentSet("GET /cristin/person/{id}/organization/{orgId}", GET, "/cristin/person/{id}/organization/{orgId}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("PATCH /cristin/person/{id}/organization/{orgId}", PATCH, "/cristin/person/{id}/organization/{orgId}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("GET /cristin/person/{id}/employment", GET, "/cristin/person/{id}/employment", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("POST /cristin/person/{id}/employment", POST, "/cristin/person/{id}/employment", UIB_CREATOR, PRIMARY_ID),
      // argumentSet("DELETE /cristin/person/{id}/employment/{employmentId}", DELETE, "/cristin/person/{id}/employment/{employmentId}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("PATCH /cristin/person/{id}/employment/{employmentId}", PATCH, "/cristin/person/{id}/employment/{employmentId}", UIB_CREATOR, PRIMARY_ID, SECONDARY_ID),
      // argumentSet("PUT /cristin/person/{id}/picture", PUT, "/cristin/person/{id}/picture", UIB_CREATOR, PRIMARY_ID),

      // argumentSet("POST /approval", POST, "/approval", UIB_CREATOR)
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
