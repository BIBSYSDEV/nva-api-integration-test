package no.sikt.publication.identifier;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import no.sikt.Affiliation;
import no.sikt.CognitoLogin;
import static no.sikt.Requests.givenAuthenticatedJsonRequest;
import no.sikt.UserFixtures;
import no.sikt.publication.IntegrationTestBase;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class FetchApiTest extends IntegrationTestBase {

  private static final String IDENTIFIER = "identifier";
  private static final String RESOURCE_OWNER = "resourceOwner";
  private static String creatorAccessToken;
  private static String customerUib;

  @BeforeAll
  static void init() {
    customerUib = RestAssured.baseURI + "/customer/a228aba6-932b-4f53-b2de-31ad8daf9f8d";
    creatorAccessToken = CognitoLogin.login(UserFixtures.UIB_CREATOR.userId()).get("accessToken");
  }

  @Test
  @DisplayName("Fetch publication by identifier")
  @Description("Fetch publication by identifier should return publication metadata and statuscode 200 Ok")
  void shouldReturnDraftPublicationWhenFetchedByIdentifier() {
    var identifier = PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER);

    given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .when()
        .get(PUBLICATION_PATH + identifier)
        .then()
        .statusCode(200)
        .body(IDENTIFIER, equalTo(identifier))
        .body("status", equalTo("DRAFT"))
        .appendRootPath(RESOURCE_OWNER)
        .body("owner", equalTo(UserFixtures.UIB_CREATOR.cristinId()))
        .body("ownerAffiliation", equalTo(Affiliation.UIB.getValue()))
        .detachRootPath(RESOURCE_OWNER)
        .appendRootPath("publisher")
        .body("type", equalTo("Organization"))
        .body("id", equalTo(customerUib));
  }

  @Test
  @DisplayName("Fetch non-existing publication")
  @Description("Fetch non-existing publication should return statuscode 404 Not Found")
  void shouldReturnNotFoundWhenFetchingUnknownIdentifier() {
    var randomIdentifier = UUID.randomUUID().toString();

    givenAuthenticatedJsonRequest(creatorAccessToken)
        .when()
        .get(PUBLICATION_PATH + randomIdentifier)
        .then()
        .statusCode(404)
        .body("title", equalTo("Not Found"))
        .body("detail", equalTo("Publication not found: " + randomIdentifier));
  }
}
