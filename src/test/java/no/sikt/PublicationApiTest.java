package no.sikt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class PublicationApiTest {

    private static final String BASE_URI = "https://api.e2e.nva.aws.unit.no";
    private static final Map<String, String> creatorHeaders = new HashMap<>();
    private static final Map<String, String> curatorHeaders = new HashMap<>();
    private static final Map<String, String> identifierMap = new HashMap<>();
    private static final String GET_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    private static final String PUBLISH_INCOMPLETE_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    private static final String DELETE_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    private static final String UNAUTHORIZED_DELETE_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    private static final String PUBLISH_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    
    @BeforeAll
    public static void init() {

        RestAssured.filters(new AllureRestAssured());
        LogConfig logConfig = LogConfig.logConfig()
            .enableLoggingOfRequestAndResponseIfValidationFails()
            .blacklistHeaders(List.of("Authorization"));
        RestAssured.config = RestAssured.config().logConfig(logConfig);
        
        final String creatorAccessToken = CognitoLogin.login(TestUser.UIB_CREATOR.userId).get("accessToken");
        final String publishingCuratorAccessToken = CognitoLogin.login(TestUser.UIB_PUBLISHING_CURATOR.userId).get("accessToken");
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

        Response createResponse = PublicationFactory.createDraftPublication(TestUser.UIB_CREATOR);
        String publishIdentifier = createResponse.jsonPath().get("identifier");
        identifierMap.put(PUBLISH_PUBLICATION_TITLE, publishIdentifier);
        Map<String, Object> responseBody = createResponse.body().jsonPath().getMap("");

        Map<String, ?> entityDescription = PublicationFactory.createEntityDescription(PUBLISH_PUBLICATION_TITLE, Category.ACADEMIC_ARTICLE, List.of(TestUser.UIB_CREATOR));
        responseBody.put("entityDescription", entityDescription);

        PublicationFactory.updatePublication(TestUser.UIB_CREATOR, responseBody);

    }
    
    @Test
    public void publishReturnStatusCode202() {
        
        String identifier = identifierMap.get(PUBLISH_PUBLICATION_TITLE);

        given()
            .log().all()
            .filter(new AllureRestAssured())
            .baseUri(BASE_URI)
            .headers(curatorHeaders)
            .accept(ContentType.JSON)
            // .contentType(ContentType.JSON)
        .when()
            .post("/publication/" + identifier + "/publish")
        .then()
            .statusCode(202);
    }


    @Test
    public void createReturnStatusCode201() {
        given()
            .log().all()
            .filter(new AllureRestAssured())
            .baseUri(BASE_URI)
            .headers(creatorHeaders)
            .accept(ContentType.JSON)
            // .contentType(ContentType.JSON)
        .when()
            .post("/publication/")
        .then()
            .statusCode(201);

    }

    @Test
    public void deleteReturnStatusCode202() {
        String identifier = identifierMap.get(DELETE_PUBLICATION_TITLE);

        given()
            .log().all()
            .filter(new AllureRestAssured())
            .headers(creatorHeaders)
        .when()
            .delete("/publication/" + identifier)
        .then()
            .statusCode(202);
    }

    @Test
    public void deleteWithWrongIdentifierReturnStatusCode404() {

        given()
            .log().all()
            .filter(new AllureRestAssured())
            .headers(creatorHeaders)
        .when()
            .delete("/publication/" + UUID.randomUUID().toString())
        .then()
            .statusCode(404);
    }

    @Test
    public void deleteWithUnauthenticatedUserReturnStatusCode401() {
        String identifier = identifierMap.get(UNAUTHORIZED_DELETE_PUBLICATION_TITLE);

        given()
            .log().all()
            .filter(new AllureRestAssured())
            // .headers(headers)
        .when()
            .delete("/publication/" + identifier)
        .then()
            .statusCode(401);
    }

    @Test
    public void getReturnStatusCode200() {
        String identifier = identifierMap.get(GET_PUBLICATION_TITLE);

        given()
            .log().all()
            .filter(new AllureRestAssured())
            .baseUri(BASE_URI)
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get("/publication/" + identifier)
        .then()
            .statusCode(200);
    }

    @Test
    public void getWithWrongIdentifierReturnStatusCode404() {

        given()
            .log().all()
            .filter(new AllureRestAssured())
            .baseUri(BASE_URI)
            .headers(creatorHeaders)
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get("/publication/" + UUID.randomUUID().toString())
        .then()
            .statusCode(404);
    }

    @Test
    public void publishWithIncompleteMetadataReturnStatusCode400() {
        String identifier = identifierMap.get(PUBLISH_INCOMPLETE_PUBLICATION_TITLE);

        given()
            .log().all()
            .filter(new AllureRestAssured())
            .baseUri(BASE_URI)
            .headers(curatorHeaders)
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .post("/publication/" + identifier + "/publish")
        .then()
            .body("detail", equalTo("Resource is not publishable!"))
            .statusCode(400);

    }
}
