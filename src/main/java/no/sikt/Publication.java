package no.sikt;

import java.util.HashMap;
import java.util.Map;

import io.restassured.RestAssured;
import io.restassured.response.Response;

public class Publication {

    private static final String BASE_URI = "https://api.e2e.nva.aws.unit.no";

    public enum Category {
        ACADEMIC_ARTICLE
    }
    
    public static String createDraftPublication(String user) {

        String ACCESS_TOKEN = CognitoLogin.login(user).get("accessToken");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Authorization", "Bearer " + ACCESS_TOKEN);

        RestAssured.baseURI = BASE_URI;
        Response getResponse = RestAssured.given()
            .log().all()
            .headers(headers)
            .post("/publication");

        return getResponse.jsonPath().get("identifier");
    }

    public static String createPublishedPublication(String user, String title, Category category) {

        String ACCESS_TOKEN = CognitoLogin.login(user).get("accessToken");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Authorization", "Bearer " + ACCESS_TOKEN);

        RestAssured.baseURI = BASE_URI;
        Response getResponse = RestAssured.given()
            .log().all()
            .headers(headers)
            .post("/publication");

        getResponse.as(HashMap.class);

        return getResponse.jsonPath().get("identifier");
    }

}
