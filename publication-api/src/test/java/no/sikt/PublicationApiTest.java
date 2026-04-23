package no.sikt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;

public class PublicationApiTest {

    private static final Map<String, String> creatorHeaders = new HashMap<>();
    private static final Map<String, String> curatorHeaders = new HashMap<>();
    private static final Map<String, String> identifierMap = new HashMap<>();
    private static final String GET_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    private static final String PUBLISH_INCOMPLETE_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    private static final String DELETE_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    private static final String UNAUTHORIZED_DELETE_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    private static final String PUBLISH_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    private static String CUSTOMER_UIB;
    
    @BeforeAll
    public static void init() {

        PublicationFactory.setBaseUriFromParameterStore();
        CUSTOMER_UIB = RestAssured.baseURI + "/customer/a228aba6-932b-4f53-b2de-31ad8daf9f8d";
        RestAssured.filters(new AllureRestAssured());
        var logConfig = LogConfig.logConfig()
            .enableLoggingOfRequestAndResponseIfValidationFails()
            .blacklistHeaders(List.of("Authorization"));
        RestAssured.config = RestAssured.config().logConfig(logConfig);
        
        final var creatorAccessToken = CognitoLogin.login(TestUser.UIB_CREATOR.userId).get("accessToken");
        final var publishingCuratorAccessToken = CognitoLogin.login(TestUser.UIB_PUBLISHING_CURATOR.userId).get("accessToken");
        creatorHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        creatorHeaders.put("Authorization", "Bearer " + creatorAccessToken);
        curatorHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        curatorHeaders.put("Authorization", "Bearer " + publishingCuratorAccessToken);

        String getIdentifier = PublicationFactory.createDraftPublication(TestUser.UIB_CREATOR).jsonPath().get("identifier");
        identifierMap.put(GET_PUBLICATION_TITLE, getIdentifier);

        String deleteIdentifier = PublicationFactory.createDraftPublication(TestUser.UIB_CREATOR).jsonPath().get("identifier");
        identifierMap.put(DELETE_PUBLICATION_TITLE, deleteIdentifier);

        String deleteUnauthorizedIdentifier = PublicationFactory.createDraftPublication(TestUser.UIB_CREATOR).jsonPath().get("identifier");
        identifierMap.put(UNAUTHORIZED_DELETE_PUBLICATION_TITLE, deleteUnauthorizedIdentifier);

        String publishIncompleteIdentifier = PublicationFactory.createDraftPublication(TestUser.UIB_CREATOR).jsonPath().get("identifier");
        identifierMap.put(PUBLISH_INCOMPLETE_PUBLICATION_TITLE, publishIncompleteIdentifier);

        var createResponse = PublicationFactory.createDraftPublication(TestUser.UIB_CREATOR);
        String publishIdentifier = createResponse.jsonPath().get("identifier");
        identifierMap.put(PUBLISH_PUBLICATION_TITLE, publishIdentifier);
        Map<String, Object> responseBody = createResponse.body().jsonPath().getMap("");

        Map<String, ?> entityDescription = PublicationFactory.createEntityDescription(PUBLISH_PUBLICATION_TITLE, Category.ACADEMIC_ARTICLE, List.of(TestUser.UIB_CREATOR));
        responseBody.put("entityDescription", entityDescription);

        PublicationFactory.updatePublication(TestUser.UIB_CREATOR, responseBody);

    }
    
    @Test
    public void publishReturnStatusCode202() {
        
        var identifier = identifierMap.get(PUBLISH_PUBLICATION_TITLE);

        given()
            .log().all()
            .headers(curatorHeaders)
            .accept(ContentType.JSON)
        .when()
            .post("/publication/" + identifier + "/publish")
        .then()
            .log().all()
            .statusCode(202);
    }


    @Test
    public void createReturnStatusCode201() {
        var today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        given()
            .log().all()
            .headers(creatorHeaders)
            .accept(ContentType.JSON)
        .when()
            .post("/publication/")
        .then()
            .log().all()
            .statusCode(201)
            .body("type", equalTo("Publication"))
            .body("identifier", notNullValue())
            .body("status", equalTo("DRAFT"))
            .body("resourceOwner.owner", equalTo(TestUser.UIB_CREATOR.cristinId))
            .body("resourceOwner.ownerAffiliation", equalTo(TestUser.UIB_CREATOR.affiliations.get(0)))
            .body("publisher.type", equalTo("Organization"))
            .body("publisher.id", equalTo(CUSTOMER_UIB))
            .body("createdDate", startsWith(today))
            .body("modifiedDate", startsWith(today));
    }

    @Test
    public void deleteReturnStatusCode202() {
        var identifier = identifierMap.get(DELETE_PUBLICATION_TITLE);

        given()
            .log().all()
            .headers(creatorHeaders)
        .when()
            .delete("/publication/" + identifier)
        .then()
            .log().all()
            .statusCode(202);
    }

    @Test
    public void deleteWithWrongIdentifierReturnStatusCode404() {

        given()
            .log().all()
            .headers(creatorHeaders)
        .when()
            .delete("/publication/" + UUID.randomUUID().toString())
        .then()
            .log().all()
            .statusCode(404);
    }

    @Test
    public void deleteWithUnauthenticatedUserReturnStatusCode401() {
        var identifier = identifierMap.get(UNAUTHORIZED_DELETE_PUBLICATION_TITLE);

        given()
            .log().all()
        .when()
            .delete("/publication/" + identifier)
        .then()
            .log().all()
            .statusCode(401)
            .body("message", equalTo("Unauthorized"));
    }

    @Test
    public void getDraftPublicationReturnStatusCode200() {
        var identifier = identifierMap.get(GET_PUBLICATION_TITLE);

        given()
            .log().all()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get("/publication/" + identifier)
        .then()
            .log().all()
            .statusCode(200)
            .body("identifier", equalTo(identifier))
            .body("status", equalTo("DRAFT"))
            .body("resourceOwner.owner", equalTo(TestUser.UIB_CREATOR.cristinId))
            .body("resourceOwner.ownerAffiliation", equalTo(TestUser.UIB_CREATOR.affiliations.get(0)))
            .body("publisher.type", equalTo("Organization"))
            .body("publisher.id", equalTo(CUSTOMER_UIB));
    }

    @Test
    public void getWithWrongIdentifierReturnStatusCode404() {
        var randomIdentifier = UUID.randomUUID().toString();

        given()
            .log().all()
            .headers(creatorHeaders)
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get("/publication/" + randomIdentifier)
        .then()
            .log().all()
            .statusCode(404)
            .body("title", equalTo("Not Found"))
            .body("detail", equalTo("Publication not found: " + randomIdentifier));
    }

    @Test
    public void publishWithIncompleteMetadataReturnStatusCode400() {
        var identifier = identifierMap.get(PUBLISH_INCOMPLETE_PUBLICATION_TITLE);

        given()
            .log().all()
            .headers(curatorHeaders)
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .post("/publication/" + identifier + "/publish")
        .then()
            .log().all()
            .statusCode(400)
            .body("title", equalTo("Bad Request"))
            .body("detail", equalTo("Resource is not publishable!"));

    }
}
