package no.sikt;

import static io.restassured.RestAssured.given;
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

  private static final Map<String, String> CREATOR_HEADERS = new HashMap<>();
  private static final Map<String, String> CURATOR_HEADERS = new HashMap<>();
  private static final Map<String, String> IDENTIFIER_MAP = new HashMap<>();
  private static final String IDENTIFIER = "identifier";

  private static final String TITLE_ROOT = "Integration test publication ";
  private static final String GET_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID().toString();
  private static final String PUBLISH_INCOMPLETE_PUBLICATION_TITLE =
      TITLE_ROOT + UUID.randomUUID().toString();
  private static final String DELETE_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID().toString();
  private static final String UNAUTHORIZED_DELETE_PUBLICATION_TITLE =
      TITLE_ROOT + UUID.randomUUID().toString();
  private static final String PUBLISH_PUBLICATION_TITLE = TITLE_ROOT + UUID.randomUUID().toString();
  private static String customerUib;

  private static final PublicationFactory PUBLICATION_FACTORY = new PublicationFactory();
  private static final String PUBLICATION_PATH = "/publication/";

  @SuppressWarnings({"unused"})
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

    final var creatorAccessToken =
        CognitoLogin.login(UserFixtures.UIB_CREATOR.userId()).get("accessToken");
    final var publishingCuratorAccessToken =
        CognitoLogin.login(UserFixtures.UIB_PUBLISHING_CURATOR.userId()).get("accessToken");
    CREATOR_HEADERS.put("Content-Type", "application/x-www-form-urlencoded");
    CREATOR_HEADERS.put("Authorization", "Bearer " + creatorAccessToken);
    CURATOR_HEADERS.put("Content-Type", "application/x-www-form-urlencoded");
    CURATOR_HEADERS.put("Authorization", "Bearer " + publishingCuratorAccessToken);

    String getIdentifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .get(IDENTIFIER);
    IDENTIFIER_MAP.put(GET_PUBLICATION_TITLE, getIdentifier);

    String deleteIdentifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .get(IDENTIFIER);
    IDENTIFIER_MAP.put(DELETE_PUBLICATION_TITLE, deleteIdentifier);

    String deleteUnauthorizedIdentifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .get(IDENTIFIER);
    IDENTIFIER_MAP.put(UNAUTHORIZED_DELETE_PUBLICATION_TITLE, deleteUnauthorizedIdentifier);

    String publishIncompleteIdentifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .get(IDENTIFIER);
    IDENTIFIER_MAP.put(PUBLISH_INCOMPLETE_PUBLICATION_TITLE, publishIncompleteIdentifier);

    var createResponse = PUBLICATION_FACTORY.createDraftPublication(UserFixtures.UIB_CREATOR);
    String publishIdentifier = createResponse.jsonPath().get(IDENTIFIER);
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
  void publishReturnStatusCode202() {

    var identifier = IDENTIFIER_MAP.get(PUBLISH_PUBLICATION_TITLE);

    given()
        .log()
        .all()
        .headers(CURATOR_HEADERS)
        .accept(ContentType.JSON)
        .when()
        .post(PUBLICATION_PATH + identifier + "/publish")
        .then()
        .log()
        .all()
        .statusCode(202);
  }

  @Test
  void createReturnStatusCode201() {
    var today =
        LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    given()
        .log()
        .all()
        .headers(CREATOR_HEADERS)
        .accept(ContentType.JSON)
        .when()
        .post(PUBLICATION_PATH)
        .then()
        .log()
        .all()
        .statusCode(201)
        .body("type", equalTo("Publication"))
        .body(IDENTIFIER, notNullValue())
        .body("status", equalTo("DRAFT"))
        .body("resourceOwner.owner", equalTo(UserFixtures.UIB_CREATOR.cristinId()))
        .body("resourceOwner.ownerAffiliation", equalTo(Affiliation.UIB.getValue()))
        .body("publisher.type", equalTo("Organization"))
        .body("publisher.id", equalTo(customerUib))
        .body("createdDate", startsWith(today))
        .body("modifiedDate", startsWith(today));
  }

  @Test
  void deleteReturnStatusCode202() {
    var identifier = IDENTIFIER_MAP.get(DELETE_PUBLICATION_TITLE);

    given()
        .log()
        .all()
        .headers(CREATOR_HEADERS)
        .when()
        .delete(PUBLICATION_PATH + identifier)
        .then()
        .log()
        .all()
        .statusCode(202);
  }

  @Test
  void deleteWithWrongIdentifierReturnStatusCode404() {

    given()
        .log()
        .all()
        .headers(CREATOR_HEADERS)
        .when()
        .delete(PUBLICATION_PATH + UUID.randomUUID().toString())
        .then()
        .log()
        .all()
        .statusCode(404);
  }

  @Test
  void deleteWithUnauthenticatedUserReturnStatusCode401() {
    var identifier = IDENTIFIER_MAP.get(UNAUTHORIZED_DELETE_PUBLICATION_TITLE);

    given()
        .log()
        .all()
        .when()
        .delete(PUBLICATION_PATH + identifier)
        .then()
        .log()
        .all()
        .statusCode(401)
        .body("message", equalTo("Unauthorized"));
  }

  @Test
  void retreiveDraftPublicationReturnStatusCode200() {
    var identifier = IDENTIFIER_MAP.get(GET_PUBLICATION_TITLE);

    given()
        .log()
        .all()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .when()
        .get(PUBLICATION_PATH + identifier)
        .then()
        .log()
        .all()
        .statusCode(200)
        .body(IDENTIFIER, equalTo(identifier))
        .body("status", equalTo("DRAFT"))
        .body("resourceOwner.owner", equalTo(UserFixtures.UIB_CREATOR.cristinId()))
        .body("resourceOwner.ownerAffiliation", equalTo(Affiliation.UIB.getValue()))
        .body("publisher.type", equalTo("Organization"))
        .body("publisher.id", equalTo(customerUib));
  }

  @Test
  void retreiveWithWrongIdentifierReturnStatusCode404() {
    var randomIdentifier = UUID.randomUUID().toString();

    given()
        .log()
        .all()
        .headers(CREATOR_HEADERS)
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .when()
        .get(PUBLICATION_PATH + randomIdentifier)
        .then()
        .log()
        .all()
        .statusCode(404)
        .body("title", equalTo("Not Found"))
        .body("detail", equalTo("Publication not found: " + randomIdentifier));
  }

  @Test
  void publishWithIncompleteMetadataReturnStatusCode400() {
    var identifier = IDENTIFIER_MAP.get(PUBLISH_INCOMPLETE_PUBLICATION_TITLE);

    given()
        .log()
        .all()
        .headers(CURATOR_HEADERS)
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .when()
        .post(PUBLICATION_PATH + identifier + "/publish")
        .then()
        .log()
        .all()
        .statusCode(400)
        .body("title", equalTo("Bad Request"))
        .body("detail", equalTo("Resource is not publishable!"));
  }
}
