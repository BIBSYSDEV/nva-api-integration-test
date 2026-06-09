package no.sikt.nva;

import static java.util.Objects.isNull;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_DAY;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_MONTH;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedFormRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.publication.PublicationFields.CONTEXT_FIELD;
import static no.sikt.nva.apitest.publication.PublicationFields.ENTITY_DESCRIPTION_FIELD;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;
import static no.sikt.nva.apitest.publication.PublicationPaths.publicationPath;
import static no.sikt.nva.apitest.publication.PublicationPaths.publishPublicationPath;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import no.sikt.Category;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.publication.PublicationPaths;

public class PublicationFactory {

  private JsonPath loadJsonResource(String resourcePath) {
    var resourceStream = PublicationFactory.class.getResourceAsStream(resourcePath);
    if (isNull(resourceStream)) {
      throw new IllegalArgumentException("Resource not found on classpath: " + resourcePath);
    }
    return new JsonPath(resourceStream);
  }

  public Response createDraftPublication(User user) {
    return givenAuthenticatedFormRequestAsUser(user)
        .post(PublicationPaths.createPublicationPath())
        .then()
        .statusCode(201)
        .extract()
        .response();
  }

  public Response updatePublication(User user, Map<String, Object> payload) {
    return givenAuthenticatedJsonRequestAsUser(user)
        .body(payload)
        .put(publicationPath(payload.get(IDENTIFIER_FIELD).toString()))
        .then()
        .statusCode(200)
        .extract()
        .response();
  }

  public String createPublishedPublication(
      User user, String title, Category category, List<User> contributorList, User curator) {

    var createResponse = createDraftPublication(user);

    var identifier = createResponse.body().jsonPath().getString(IDENTIFIER_FIELD);
    Map<String, Object> responseBody = createResponse.body().jsonPath().getMap("");
    responseBody.remove(CONTEXT_FIELD);

    var entityDescription = createEntityDescription(title, category, contributorList);
    responseBody.put(ENTITY_DESCRIPTION_FIELD, entityDescription);

    updatePublication(user, responseBody);

    publish(curator, identifier);

    return createResponse.jsonPath().get(IDENTIFIER_FIELD);
  }

  public String createChapterInAnthology(
      User user,
      String title,
      Category category,
      List<User> contributorList,
      User curator,
      String anthologyIdentifier,
      List<User> anthologyEditorList) {

    var createResponse = createDraftPublication(user);

    var identifier = createResponse.body().jsonPath().getString(IDENTIFIER_FIELD);
    Map<String, Object> responseBody = createResponse.body().jsonPath().getMap("");
    responseBody.remove(CONTEXT_FIELD);

    var entityDescription = createEntityDescription(title, category, contributorList);
    ((Map<String, Object>)
            ((Map<String, Object>) entityDescription.get("reference")).get("publicationContext"))
        .put("id", RestAssured.baseURI + publicationPath(anthologyIdentifier));
    responseBody.put(ENTITY_DESCRIPTION_FIELD, entityDescription);

    updatePublication(user, responseBody);

    publish(curator, identifier);

    return createResponse.jsonPath().get(IDENTIFIER_FIELD);
  }

  public String createAnthologyForChapter(
      User user, String title, User curator, List<User> anthologyEditorList) {
    var anthologyTitle = "Anthology for " + title;
    var anthologyCreateResponse = createDraftPublication(user);

    var anthologyIdentifier = anthologyCreateResponse.body().jsonPath().getString(IDENTIFIER_FIELD);
    Map<String, Object> anthologyResponseBody =
        anthologyCreateResponse.body().jsonPath().getMap("");
    anthologyResponseBody.remove(CONTEXT_FIELD);

    var anthologyEntityDescription =
        createEntityDescription(anthologyTitle, Category.BOOK_ANTHOLOGY, anthologyEditorList);
    var contributors = (List<Map<String, Object>>) anthologyEntityDescription.get("contributors");

    contributors.forEach(
        contributor -> {
          ((Map<String, Object>) contributor.get("role")).put("type", "Editor");
        });
    anthologyResponseBody.put(ENTITY_DESCRIPTION_FIELD, anthologyEntityDescription);
    updatePublication(user, anthologyResponseBody);

    publish(curator, anthologyIdentifier);
    return anthologyIdentifier;
  }

  public Map<String, Object> createEntityDescription(
      String title, Category category, List<User> contributorList) {

    var entityDescriptionJsonPath = loadJsonResource("/metadata/EntityDescription.json");

    Map<String, Object> entityDescription =
        entityDescriptionJsonPath.getMap(ENTITY_DESCRIPTION_FIELD);
    entityDescription.put("mainTitle", title);

    Map<String, Object> publicationDate =
        entityDescriptionJsonPath.getMap("entityDescription.publicationDate");
    publicationDate.put("day", CURRENT_DAY);
    publicationDate.put("month", CURRENT_MONTH);
    publicationDate.put("year", CURRENT_YEAR);
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
    if (publicationContext.containsKey("publisher")) {
      Map<String, Object> publisher =
          referenceJsonPath.get("reference.publicationContext.publisher");
      publisher.put("id", publisher.get("id") + "/" + CURRENT_YEAR);
      publicationContext.put("publisher", publisher);
    }
    Map<String, Object> reference = referenceJsonPath.getMap("reference");
    reference.put("publicationContext", publicationContext);

    return reference;
  }

  public void publish(User curator, String identifier) {
    givenAuthenticatedJsonRequestAsUser(curator.userId())
        .post(publishPublicationPath(identifier))
        .then()
        .statusCode(202);
  }

  public List<Map<String, Object>> createContributors(List<User> users) {

    List<Map<String, Object>> contributors = new ArrayList<>();
    final var sequence = new AtomicInteger(1);
    var contributorJsonPath = loadJsonResource("/metadata/Contributor.json");
    users.forEach(
        user -> {
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
                  });
          contributor.put("affiliations", affiliations);

          contributors.add(contributor);
        });

    return contributors;
  }
}
