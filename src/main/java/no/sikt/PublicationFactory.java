package no.sikt;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

public class PublicationFactory {

    private static final String YEAR = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
    private static final String MONTH = Integer.toString(Calendar.getInstance().get(Calendar.MONTH) + 1);
    private static final String DAY = Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)); 

    private static final String REGION = Objects.nonNull(System.getenv("AWS_REGION")) ? System.getenv("AWS_REGION")
            : "eu-west-1";

    public static String getBaseUriFromParameterStore() {

        try (SsmClient ssm = SsmClient.builder()
            .region(Region.of(REGION))
            .build()) {

            GetParameterRequest request = GetParameterRequest.builder()
                .name("/NVA/ApiDomain")
                .withDecryption(false)
                .build();
                    
            GetParameterResponse response = ssm.getParameter(request);

            if (Objects.isNull(response) || Objects.isNull(response.parameter()) || Objects.isNull(response.parameter().value())
                    || response.parameter().value().isEmpty()) {
                throw new RuntimeException("Parameter '/NVA/ApiDomain' was not found or contains no value.");
            }

            String value = response.parameter().value();

            return "https://" + value;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch BASE_URI from Parameter Store", e);
        }

    }

    public static Response createDraftPublication(TestUser user) {

        String ACCESS_TOKEN = CognitoLogin.login(user.userId).get("accessToken");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Authorization", "Bearer " + ACCESS_TOKEN);

        String baseUri = getBaseUriFromParameterStore();

        RestAssured.baseURI = baseUri;
        Response createResponse = 
            given()
                .log().all()
                .headers(headers)
                .post("/publication")
            .then()
                .log().all()
                .statusCode(201)
            .extract()
                .response();

        return createResponse;
    }

    public static Response updatePublication(TestUser user, Map<String, Object> payload) {
        String CREATOR_ACCESS_TOKEN = CognitoLogin.login(user.userId).get("accessToken");
        Map<String, String> creatorHeaders = new HashMap<>();
        creatorHeaders.put("Authorization", "Bearer " + CREATOR_ACCESS_TOKEN);
        creatorHeaders.put("Content-Type", "application/json");
        creatorHeaders.put("Accept", "application/json");

        Response updateResponse = 
            given()
                .headers(creatorHeaders)
                .body(payload)
                .put("/publication/" + payload.get("identifier"))
            .then()
                .statusCode(200)
            .extract()
                .response();

        return updateResponse;
    }

    public static String createPublishedPublication(TestUser user, String title, Category category, List<TestUser> contributorList, String curator) {

        Response createResponse = createDraftPublication(user);

        String identifier = createResponse.body().jsonPath().get("identifier");
        Map<String, Object> responseBody = createResponse.body().jsonPath().getMap("");
        responseBody.remove("@context");
        
        Map<String, Object> entityDescription = createEntityDescription(title, category, contributorList);
        responseBody.put("entityDescription", entityDescription);
        
        updatePublication(user, responseBody);

        publish(curator, identifier);

        return createResponse.jsonPath().get("identifier");
    }

    public static Map<String, Object> createEntityDescription(String title, Category category, List<TestUser> contributorList) {
        
        File entityDescriptionFile = new File(PublicationFactory.class.getResource("/metadata/EntityDescription.json").getFile());
        JsonPath entityDescriptionJsonPath = new JsonPath(entityDescriptionFile);
        
        Map<String, Object> entityDescription = entityDescriptionJsonPath.getMap("entityDescription");
        entityDescription.put("mainTitle", title);
        
        Map<String, Object> publicationDate = entityDescriptionJsonPath.getMap("entityDescription.publicationDate");
        publicationDate.put("day", DAY);
        publicationDate.put("month", MONTH);
        publicationDate.put("year", YEAR);
        entityDescription.put("publicationDate", publicationDate);

        Map<String, Object> reference = createReference(category);
        entityDescription.put("reference", reference);
        List<Map<String, Object>> contributors = createContributors(contributorList);
        entityDescription.put("contributors", contributors);

        return entityDescription;
    }

    private static Map<String, Object> createReference(Category category) {

        File referenceFile = new File(PublicationFactory.class.getResource("/metadata/" + category.value + "Reference.json").getFile());
        JsonPath referenceJsonPath = new JsonPath(referenceFile);
        Map<String, Object> publicationContext = referenceJsonPath.getMap("reference.publicationContext");
        publicationContext.put("id", publicationContext.get("id") + "/" + YEAR);

        Map<String, Object> reference = referenceJsonPath.getMap("reference");
        reference.put("publicationContext", publicationContext);

        return reference;
    }

    public static void publish(String curator, String identifier) {
        String CURATOR_ACCESS_TOKEN = CognitoLogin.login(curator).get("accessToken");
        Map<String, String> curatorHeaders = new HashMap<>();
        curatorHeaders.put("Authorization", "Bearer " + CURATOR_ACCESS_TOKEN);
        curatorHeaders.put("Content-Type", "application/json");
        curatorHeaders.put("Accept", "application/json");

        RestAssured.given()
                .headers(curatorHeaders)
                .post("/publication/" + identifier + "/publish");
    }

    public static List<Map<String, Object>> createContributors(List<TestUser> users) {

        List<Map<String, Object>> contributors = new ArrayList<>();
        final AtomicInteger sequence = new AtomicInteger(1);
        users.forEach(user -> {
            File contributorFile = new File(PublicationFactory.class.getResource("/metadata/Contributor.json").getFile());
            JsonPath contributorJsonPath = new JsonPath(contributorFile);
            Map<String, Object> contributor = contributorJsonPath.getMap("");
            Integer i = sequence.getAndIncrement();
            contributor.put("sequence", i.toString());
            Map<String, Object> identity = new HashMap<>();
            identity.put("type", "Identity");
            identity.put("id", user.cristinId);
            identity.put("verificationStatus", "Verified");
            identity.put("name", user.name);
            contributor.put("identity", identity);

            List<Map<String, Object>> affiliations = new ArrayList<>();
            user.affiliations.forEach(userAffiliation -> {

                Map<String, Object> affiliation = new HashMap<>();
                affiliation.put("type", "Organization");
                affiliation.put("id", userAffiliation);
                affiliations.add(affiliation);
                contributor.put("affiliations", affiliations);
            });

            contributors.add(contributor);
        });
        
        return contributors;
    }
}
