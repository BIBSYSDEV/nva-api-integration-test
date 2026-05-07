package no.sikt;

import static io.restassured.RestAssured.given;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequest;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.base.UserFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class PublicationApiTest extends no.sikt.nva.apitest.base.IntegrationTestBase {

  private static final Map<String, String> IDENTIFIER_MAP = new HashMap<>();

  private static final String TITLE_ROOT = "Integration test publication ";
  private static final String GET_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID();
  private static final String PUBLISH_INCOMPLETE_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID();
  private static final String DELETE_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID();
  private static final String UNAUTHORIZED_DELETE_PUBLICATION_TITLE =
      TITLE_ROOT + UUID.randomUUID();
  private static final String PUBLISH_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID();

  private static String customerUib;
  private static String creatorAccessToken;
  private static String curatorAccessToken;

  private static final PublicationFactory PUBLICATION_FACTORY = new PublicationFactory();
  private static final String PUBLICATION_PATH = "/publication/";
  private static final String IDENTIFIER = "identifier";
  private static final String RESOURCE_OWNER = "resourceOwner";

  private static void createDraftTestPublication(String title, User user) {
    var identifier =
        PUBLICATION_FACTORY.createDraftPublication(user).jsonPath().getString(IDENTIFIER);
    IDENTIFIER_MAP.put(title, identifier);
  }

  @BeforeAll
  static void init() {

    customerUib = RestAssured.baseURI + "/customer/a228aba6-932b-4f53-b2de-31ad8daf9f8d";

    creatorAccessToken = CognitoLogin.login(UserFixtures.UIB_CREATOR.userId()).get("accessToken");
    curatorAccessToken =
        CognitoLogin.login(UserFixtures.UIB_PUBLISHING_CURATOR.userId()).get("accessToken");

    createDraftTestPublication(GET_PUBLICATION_TITLE, UserFixtures.UIB_CREATOR);
    createDraftTestPublication(DELETE_PUBLICATION_TITLE, UserFixtures.UIB_CREATOR);
    createDraftTestPublication(UNAUTHORIZED_DELETE_PUBLICATION_TITLE, UserFixtures.UIB_CREATOR);
    createDraftTestPublication(PUBLISH_INCOMPLETE_PUBLICATION_TITLE, UserFixtures.UIB_CREATOR);

    var createResponse = PUBLICATION_FACTORY.createDraftPublication(UserFixtures.UIB_CREATOR);
    var publishIdentifier = createResponse.jsonPath().getString(IDENTIFIER);
    IDENTIFIER_MAP.put(PUBLISH_PUBLICATION_TITLE, publishIdentifier);
    Map<String, Object> responseBody = createResponse.body().jsonPath().getMap("");

    Map<String, ?> entityDescription =
        PUBLICATION_FACTORY.createEntityDescription(
            PUBLISH_PUBLICATION_TITLE,
            Category.ACADEMIC_ARTICLE,
            List.of(UserFixtures.UIB_CREATOR));
    responseBody.put("entityDescription", entityDescription);

    PUBLICATION_FACTORY.updatePublication(UserFixtures.UIB_CREATOR, responseBody);
  }

  @Test
  @DisplayName("Curator publish draft publication")
  @Description("A Curator calling publish should return statuscode 202 Accepted")
  void shouldPublishDraftWhenRequestedByCurator() {

    var identifier = IDENTIFIER_MAP.get(PUBLISH_PUBLICATION_TITLE);

    givenAuthenticatedRequest(curatorAccessToken)
        .accept(ContentType.JSON)
        .when()
        .post(PUBLICATION_PATH + identifier + "/publish")
        .then()
        .statusCode(202);
  }

  @Test
  @DisplayName("Creator create draft publication")
  @Description(
      "A Creator calling create publication should return publication metadata and statuscode 201"
          + " Created")
  void shouldCreateDraftPublicationOwnedByCreator() {
    var today =
        LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    givenAuthenticatedRequest(creatorAccessToken)
        .accept(ContentType.JSON)
        .when()
        .post(PUBLICATION_PATH)
        .then()
        .statusCode(201)
        .body("type", equalTo("Publication"))
        .body(IDENTIFIER, notNullValue())
        .body("status", equalTo("DRAFT"))
        .appendRootPath(RESOURCE_OWNER)
        .body("owner", equalTo(UserFixtures.UIB_CREATOR.cristinId()))
        .body("ownerAffiliation", equalTo(Affiliation.UIB.getValue()))
        .detachRootPath(RESOURCE_OWNER)
        .appendRootPath("publisher")
        .body("type", equalTo("Organization"))
        .body("id", equalTo(customerUib))
        .detachRootPath("publisher")
        .body("createdDate", startsWith(today))
        .body("modifiedDate", startsWith(today));
  }

  @Test
  @DisplayName("Delete draft publication")
  @Description("A Creator calling delete on own publication should return 202 Accepted")
  void shouldDeleteDraftWhenRequestedByOwner() {
    var identifier = IDENTIFIER_MAP.get(DELETE_PUBLICATION_TITLE);

    givenAuthenticatedRequest(creatorAccessToken)
        .when()
        .delete(PUBLICATION_PATH + identifier)
        .then()
        .statusCode(202);
  }

  @Test
  @DisplayName("Deleting non-existing draft publication")
  @Description("A Creator calling delete on non-existing publication should return 404 Not Found")
  void shouldReturnNotFoundWhenDeletingUnknownIdentifier() {

    givenAuthenticatedRequest(creatorAccessToken)
        .when()
        .delete(PUBLICATION_PATH + UUID.randomUUID())
        .then()
        .statusCode(404);
  }

  @Test
  @DisplayName("Non authorized user tries to delete publication")
  @Description("A non authorized user calling delete should return 401 Unauthorized")
  void shouldReturnUnauthorizedWhenDeletingWithoutAuthentication() {
    var identifier = IDENTIFIER_MAP.get(UNAUTHORIZED_DELETE_PUBLICATION_TITLE);

    givenUnauthenticatedJsonRequest()
        .when()
        .delete(PUBLICATION_PATH + identifier)
        .then()
        .statusCode(401)
        .body("message", equalTo("Unauthorized"));
  }

  @Test
  @DisplayName("Fetch publication by identifier")
  @Description(
      "Fetch publication by identifier should return publication metadata and statuscode 200 Ok")
  void shouldReturnDraftPublicationWhenFetchedByIdentifier() {
    var identifier = IDENTIFIER_MAP.get(GET_PUBLICATION_TITLE);

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

  @Test
  @DisplayName("Publish incomplete publication")
  @Description("Publishing an incomplete publication should return 400 Bad Request")
  void shouldRejectPublishWhenMetadataIsIncomplete() {
    var identifier = IDENTIFIER_MAP.get(PUBLISH_INCOMPLETE_PUBLICATION_TITLE);

    givenAuthenticatedJsonRequest(curatorAccessToken)
        .when()
        .post(PUBLICATION_PATH + identifier + "/publish")
        .then()
        .statusCode(400)
        .body("title", equalTo("Bad Request"))
        .body("detail", equalTo("Resource is not publishable!"));
  }
}
