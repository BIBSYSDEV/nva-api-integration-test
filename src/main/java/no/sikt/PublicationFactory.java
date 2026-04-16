package no.sikt;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.*;
import io.restassured.response.Response;

public class PublicationFactory {

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

    public static String createPublishedPublication(String user, String title, Category category, String curator) {

        String CREATOR_ACCESS_TOKEN = CognitoLogin.login(user).get("accessToken");
        Map<String, String> creatorHeaders = new HashMap<>();
        creatorHeaders.put("Authorization", "Bearer " + CREATOR_ACCESS_TOKEN);
        creatorHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        RestAssured.baseURI = BASE_URI;
        Response createResponse = RestAssured.given()
            .headers(creatorHeaders)
            .post("/publication");

            
        String identifier = createResponse.body().jsonPath().get("identifier");
        String creatorId = "https://api.e2e.nva.aws.unit.no/cristin/person/" + createResponse.body().jsonPath()
            .get("resourceOwner.owner").toString().split("@")[0];
        String ownerAffiliation = createResponse.body().jsonPath().get("resourceOwner.ownerAffiliation");
        Map<String, Object> responseBody = createResponse.body().jsonPath().getMap("");
        responseBody.remove("@context");

        Map<String, Object> entityDescription = new HashMap<>();
        entityDescription.put("type", "EntityDescription");
        entityDescription.put("mainTitle", title);

        Map<String, String> publicationDate = new HashMap<>();
        publicationDate.put("type", "PublicationDate");
        publicationDate.put("day", Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
        publicationDate.put("month", Integer.toString(Calendar.getInstance().get(Calendar.MONTH) + 1));
        publicationDate.put("year", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
        entityDescription.put("publicationDate", publicationDate);

        responseBody.put("entityDescription", entityDescription);

        Map<String, Object> reference = new HashMap<>();
        reference.put("type", "Reference");
        
        Map<String, Object> publicationContext = new HashMap<>();
        publicationContext.put("type", "Journal");
        publicationContext.put("id", "https://api.e2e.nva.aws.unit.no/publication-channels-v2/serial-publication/7ECF363E-84A8-4328-B8D0-38A9BF93E356/" + Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
        publicationContext.put("volume", "3");
        publicationContext.put("issue", "1");
        reference.put("publicationContext", publicationContext);

        Map<String, Object> publicationInstance = new HashMap<>();
        publicationInstance.put("type", "AcademicArticle");
        Map<String, Object> pages = new HashMap<>();
        pages.put("type", "Range");
        pages.put("begin", "10");
        pages.put("end", "20");
        pages.put("illustrated", "false");
        publicationInstance.put("pages", pages);
        publicationInstance.put("volume", "3");
        publicationInstance.put("issue", "1");
        reference.put("publicationInstance", publicationInstance);
        
        entityDescription.put("reference", reference);

        List<Map<String, Object>> contributors = new ArrayList<>();
        Map<String, Object> contributor = new HashMap<>();
        contributor.put("type", "Contributor");
        Map<String, Object> identity = new HashMap<>();
        identity.put("type", "Identity");
        identity.put("id", creatorId);
        identity.put("verificationStatus", "Verified");
        identity.put("name", "Registrator SINTEF TestUser");
        contributor.put("identity", identity);

        List<Map<String, Object>> affiliations = new ArrayList<>();
        Map<String, Object> affiliation = new HashMap<>();
        affiliation.put("type", "Organization");
        affiliation.put("id", ownerAffiliation);
        affiliations.add(affiliation);
        contributor.put("affiliations", affiliations);

        Map<String, Object> role = new HashMap<>();
        role.put("type", "Creator");
        contributor.put("role", role);
        contributor.put("sequence", "1");
        contributor.put("correspondingAuthor", "false");

        contributors.add(contributor);

        entityDescription.put("contributors", contributors);

        Response updateResponse = RestAssured.given()
            .log().all()
            .headers(creatorHeaders)
            .body(responseBody)
            .put("/publication/" + identifier);

        // String CURATOR_ACCESS_TOKEN = CognitoLogin.login(user).get("accessToken");
        Map<String, String> curatorHeaders = new HashMap<>();
        curatorHeaders.put("Authorization", "Bearer " + CREATOR_ACCESS_TOKEN);

        Map<String, Object> updateBody = updateResponse.body().jsonPath().getMap("");
        updateBody.remove("@context");
        

        given()
            .log().all()
            .headers(curatorHeaders)
        .when()
            .put("publication/" + identifier + "/publish")
        .then()
            .log().all()
            .statusCode(202);


        return createResponse.jsonPath().get("identifier");
    }

    public static Map<String, ?> createPublicationMetaData(String user, String title, Category category) {
        Map<String, ?> metadata = new HashMap<>();
        
        return metadata;
    }
}
