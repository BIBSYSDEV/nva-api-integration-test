package no.sikt;

import static java.util.Objects.isNull;
import static no.sikt.Requests.givenAuthenticatedFormRequestAsUser;
import static no.sikt.Requests.givenAuthenticatedJsonRequestAsUser;

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

  public void setBaseUriFromParameterStore() {

    if (isNull(baseUri)) {
      var value = CognitoLogin.getValueFromParameterStore("/NVA/ApiDomain");

      baseUri = "https://" + value;
      RestAssured.baseURI = baseUri;
    }
  }

  private JsonPath loadJsonResource(String resourcePath) {
    var resourceStream = PublicationFactory.class.getResourceAsStream(resourcePath);
    if (isNull(resourceStream)) {
      throw new IllegalArgumentException("Resource not found on classpath: " + resourcePath);
    }
    return new JsonPath(resourceStream);
  }

  public Response createDraftPublication(User user) {
    setBaseUriFromParameterStore();
    return givenAuthenticatedFormRequestAsUser(user)
        .post("/publication")
        .then()
        .statusCode(201)
        .extract()
        .response();
  }

  public Response updatePublication(User user, Map<String, Object> payload) {
    setBaseUriFromParameterStore();
    return givenAuthenticatedJsonRequestAsUser(user)
        .body(payload)
        .put("/publication/" + payload.get("identifier"))
        .then()
        .statusCode(200)
        .extract()
        .response();
  }

  public String createPublishedPublication(
      User user, String title, Category category, List<User> contributorList, String curator) {

    var createResponse = createDraftPublication(user);

    var identifier = createResponse.body().jsonPath().getString("identifier");
    Map<String, Object> responseBody = createResponse.body().jsonPath().getMap("");
    responseBody.remove("@context");

    var entityDescription = createEntityDescription(title, category, contributorList);
    responseBody.put("entityDescription", entityDescription);

    updatePublication(user, responseBody);

    publish(curator, identifier);

    return createResponse.jsonPath().get("identifier");
  }

  public Map<String, Object> createEntityDescription(
      String title, Category category, List<User> contributorList) {

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

  public Map<String, Object> createReference(Category category) {

    var referenceJsonPath = loadJsonResource("/metadata/" + category.getValue() + "Reference.json");
    Map<String, Object> publicationContext =
        referenceJsonPath.getMap("reference.publicationContext");
    publicationContext.put("id", publicationContext.get("id") + "/" + YEAR);

    Map<String, Object> reference = referenceJsonPath.getMap("reference");
    reference.put("publicationContext", publicationContext);

    return reference;
  }

  public void publish(String curator, String identifier) {
    setBaseUriFromParameterStore();
    givenAuthenticatedJsonRequestAsUser(curator)
        .post("/publication/" + identifier + "/publish")
        .then()
        .statusCode(202);
  }

  public List<Map<String, Object>> createContributors(List<User> users) {

    List<Map<String, Object>> contributors = new ArrayList<>();
    final var sequence = new AtomicInteger(1);
    users.forEach(
        user -> {
          var contributorJsonPath = loadJsonResource("/metadata/Contributor.json");
          Map<String, Object> contributor = contributorJsonPath.getMap("");
          contributor.put("sequence", String.valueOf(sequence.getAndIncrement()));
          Map<String, Object> identity = new HashMap<>();
          identity.put("type", "Identity");
          identity.put("id", user.cristinId());
          identity.put("verificationStatus", "Verified");
          identity.put("name", user.name());
          contributor.put("identity", identity);

          List<Map<String, Object>> affiliations = new ArrayList<>();
          user.affiliations()
              .forEach(
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
