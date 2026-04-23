package no.sikt;

import static io.restassured.RestAssured.given;
import static java.util.Objects.isNull;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PublicationFactory {

  private static final String YEAR = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
  private static final String MONTH =
      Integer.toString(Calendar.getInstance().get(Calendar.MONTH) + 1);
  private static final String DAY =
      Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

  private static String baseUri;

  public static void setBaseUriFromParameterStore() {

    if (isNull(baseUri)) {
      var value = CognitoLogin.getValueFromParameterStore("/NVA/ApiDomain");

      baseUri = "https://" + value;
      RestAssured.baseURI = baseUri;
    }
  }

  private static JsonPath loadJsonResource(String resourcePath) {
    var resourceStream = PublicationFactory.class.getResourceAsStream(resourcePath);
    if (isNull(resourceStream)) {
      throw new RuntimeException("Resource not found on classpath: " + resourcePath);
    }
    return new JsonPath(resourceStream);
  }

  public static Response createDraftPublication(TestUser user) {

    var ACCESS_TOKEN = CognitoLogin.login(user.userId).get("accessToken");
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Authorization", "Bearer " + ACCESS_TOKEN);

    if (isNull(baseUri)) {
      setBaseUriFromParameterStore();
    }
    var createResponse =
        given().headers(headers).post("/publication").then().statusCode(201).extract().response();

    return createResponse;
  }

  public static Response updatePublication(TestUser user, Map<String, Object> payload) {
    var CREATOR_ACCESS_TOKEN = CognitoLogin.login(user.userId).get("accessToken");
    Map<String, String> creatorHeaders = new HashMap<>();
    creatorHeaders.put("Authorization", "Bearer " + CREATOR_ACCESS_TOKEN);
    creatorHeaders.put("Content-Type", "application/json");
    creatorHeaders.put("Accept", "application/json");

    setBaseUriFromParameterStore();
    var updateResponse =
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

  public static String createPublishedPublication(
      TestUser user,
      String title,
      Category category,
      List<TestUser> contributorList,
      String curator) {

    var createResponse = createDraftPublication(user);

    String identifier = createResponse.body().jsonPath().get("identifier");
    Map<String, Object> responseBody = createResponse.body().jsonPath().getMap("");
    responseBody.remove("@context");

    Map<String, Object> entityDescription =
        createEntityDescription(title, category, contributorList);
    responseBody.put("entityDescription", entityDescription);

    updatePublication(user, responseBody);

    publish(curator, identifier);

    return createResponse.jsonPath().get("identifier");
  }

  public static Map<String, Object> createEntityDescription(
      String title, Category category, List<TestUser> contributorList) {

    var entityDescriptionJsonPath = loadJsonResource("/metadata/EntityDescription.json");

    Map<String, Object> entityDescription = entityDescriptionJsonPath.getMap("entityDescription");
    entityDescription.put("mainTitle", title);

    Map<String, Object> publicationDate =
        entityDescriptionJsonPath.getMap("entityDescription.publicationDate");
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

    var referenceJsonPath = loadJsonResource("/metadata/" + category.value + "Reference.json");
    Map<String, Object> publicationContext =
        referenceJsonPath.getMap("reference.publicationContext");
    publicationContext.put("id", publicationContext.get("id") + "/" + YEAR);

    Map<String, Object> reference = referenceJsonPath.getMap("reference");
    reference.put("publicationContext", publicationContext);

    return reference;
  }

  public static void publish(String curator, String identifier) {
    var CURATOR_ACCESS_TOKEN = CognitoLogin.login(curator).get("accessToken");
    Map<String, String> curatorHeaders = new HashMap<>();
    curatorHeaders.put("Authorization", "Bearer " + CURATOR_ACCESS_TOKEN);
    curatorHeaders.put("Content-Type", "application/json");
    curatorHeaders.put("Accept", "application/json");

    setBaseUriFromParameterStore();
    RestAssured.given().headers(curatorHeaders).post("/publication/" + identifier + "/publish");
  }

  public static List<Map<String, Object>> createContributors(List<TestUser> users) {

    List<Map<String, Object>> contributors = new ArrayList<>();
    final var sequence = new AtomicInteger(1);
    users.forEach(
        user -> {
          var contributorJsonPath = loadJsonResource("/metadata/Contributor.json");
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
          user.affiliations.forEach(
              userAffiliation -> {
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
