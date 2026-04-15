package no.sikt;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.qameta.allure.restassured.AllureRestAssured;
import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;
import io.restassured.http.ContentType;

public class PublicationApiTest {

    private static final String BASE_URI = "https://api.e2e.nva.aws.unit.no";
    private static final String USER_ID = "test-user-registrator-sintef@test.no";
    private static final String CURATOR_USER_ID = "test-user-publication-curator-messages-sintef@test.no";
    private static final String CREATOR_ACCESS_TOKEN = CognitoLogin.login(USER_ID).get("accessToken");
    private static final String CURATOR_ACCESS_TOKEN = CognitoLogin.login(CURATOR_USER_ID).get("accessToken");
    private static final Map<String, String> creatorHeaders = new HashMap<>();
    private static final Map<String, String> curatorHeaders = new HashMap<>();
    private static final Map<String, String> identifierMap = new HashMap<>();
    private static final String GET_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    private static final String PUBLISH_INCOMPLETE_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    private static final String DELETE_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();
    private static final String UNAUTHORIZED_DELETE_PUBLICATION_TITLE = "Integration test publication " + UUID.randomUUID().toString();

    static {
        creatorHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        creatorHeaders.put("Authorization", "Bearer " + CREATOR_ACCESS_TOKEN);
        curatorHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        curatorHeaders.put("Authorization", "Bearer " + CURATOR_ACCESS_TOKEN);
    }

    @BeforeAll
    public static void createTestData() {

        String getIdentifier = Publication.createDraftPublication(USER_ID);
        identifierMap.put(GET_PUBLICATION_TITLE, getIdentifier);

        String deleteIdentifier = Publication.createDraftPublication(USER_ID);
        identifierMap.put(DELETE_PUBLICATION_TITLE, deleteIdentifier);

        String deleteUnauthorizedIdentifier = Publication.createDraftPublication(USER_ID);
        identifierMap.put(UNAUTHORIZED_DELETE_PUBLICATION_TITLE, deleteUnauthorizedIdentifier);

        String publishIncompleteIdentifier = Publication.createDraftPublication(USER_ID);
        identifierMap.put(PUBLISH_INCOMPLETE_PUBLICATION_TITLE, publishIncompleteIdentifier);
    }

    @Test
    public void createReturnStatusCode201() {
        given()
            .log().all()
            .filter(new AllureRestAssured())
            .baseUri(BASE_URI)
            .headers(creatorHeaders)
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
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
    public void deleteWithUnauthorizedUserReturnStatusCode401() {
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
