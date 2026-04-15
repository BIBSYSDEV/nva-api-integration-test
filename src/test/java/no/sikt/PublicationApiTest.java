package no.sikt;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import no.sikt.Publication.Category;

public class PublicationApiTest {

    private static final String BASE_URI = "https://api.e2e.nva.aws.unit.no";
    private static final String PUBLICATION_ID = "019a293df98f-5e2f5aee-65af-4c22-9779-e79bbd584685";
    private static final String ACCESS_TOKEN =
    CognitoLogin.login("test-user-nvi@test.no")
    .get("accessToken");

    @BeforeAll
    public void createTestData() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Authorization", "Bearer " + ACCESS_TOKEN);

        String randomUuid = UUID.randomUUID().toString();
        Map<String, ?> body = Publication.createTestPublication("Integration test publication " + randomUuid, Category.ACADEMIC_ARTICLE);

        System.out.println(Category.ACADEMIC_ARTICLE);

        RestAssured.baseURI = BASE_URI;
        Response response = RestAssured.given()
                .headers(headers)
                .formParams(body)
                .post("/publication");
        
        
    }

    @Test
    public void testGetPublication() {
        given()
            .log().all()
            .filter(new AllureRestAssured())
            .baseUri(BASE_URI)
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .log().all()
            .get("/publication/" + PUBLICATION_ID)
        .then()
            .statusCode(200);
    }
}
