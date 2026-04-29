package no.sikt;

import static io.restassured.RestAssured.given;
import static no.sikt.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.Requests.givenAuthenticatedRequest;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class PublicationApiTest {

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

  @BeforeAll
  static void init() {

    PUBLICATION_FACTORY.setBaseUriFromParameterStore();
    RestAssured.filters(new AllureRestAssured());
    customerUib = RestAssured.baseURI + "/customer/a228aba6-932b-4f53-b2de-31ad8daf9f8d";
    var logConfig =
        LogConfig.logConfig()
            .enableLoggingOfRequestAndResponseIfValidationFails()
            .blacklistHeaders(List.of("Authorization"));
    RestAssured.config = RestAssured.config().logConfig(logConfig);

    creatorAccessToken = CognitoLogin.login(UserFixtures.UIB_CREATOR.userId()).get("accessToken");
    curatorAccessToken =
        CognitoLogin.login(UserFixtures.UIB_PUBLISHING_CURATOR.userId()).get("accessToken");

    var getIdentifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);
    IDENTIFIER_MAP.put(GET_PUBLICATION_TITLE, getIdentifier);

    var deleteIdentifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);
    IDENTIFIER_MAP.put(DELETE_PUBLICATION_TITLE, deleteIdentifier);

    var deleteUnauthorizedIdentifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);
    IDENTIFIER_MAP.put(UNAUTHORIZED_DELETE_PUBLICATION_TITLE, deleteUnauthorizedIdentifier);

    var publishIncompleteIdentifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);
    IDENTIFIER_MAP.put(PUBLISH_INCOMPLETE_PUBLICATION_TITLE, publishIncompleteIdentifier);

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
  void shouldDeleteDraftWhenRequestedByOwner() {
    var identifier = IDENTIFIER_MAP.get(DELETE_PUBLICATION_TITLE);

    givenAuthenticatedRequest(creatorAccessToken)
        .when()
        .delete(PUBLICATION_PATH + identifier)
        .then()
        .statusCode(202);
  }

  @Test
  void shouldReturnNotFoundWhenDeletingUnknownIdentifier() {

    givenAuthenticatedRequest(creatorAccessToken)
        .when()
        .delete(PUBLICATION_PATH + UUID.randomUUID())
        .then()
        .statusCode(404);
  }

  @Test
  void shouldReturnUnauthorizedWhenDeletingWithoutAuthentication() {
    var identifier = IDENTIFIER_MAP.get(UNAUTHORIZED_DELETE_PUBLICATION_TITLE);

    given()
        .when()
        .delete(PUBLICATION_PATH + identifier)
        .then()
        .statusCode(401)
        .body("message", equalTo("Unauthorized"));
  }

  @Test
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
