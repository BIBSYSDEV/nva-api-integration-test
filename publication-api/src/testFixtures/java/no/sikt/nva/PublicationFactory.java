package no.sikt.nva;

import static io.restassured.RestAssured.baseURI;
import static no.sikt.Category.ACADEMIC_CHAPTER;
import static no.sikt.Category.BOOK_ANTHOLOGY;
import static no.sikt.Role.CREATOR;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_DAY;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_MONTH;
import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_THESIS_CURATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.CONTEXT_FIELD;
import static no.sikt.nva.apitest.publication.PublicationFields.CONTRIBUTORS;
import static no.sikt.nva.apitest.publication.PublicationFields.ENTITY_DESCRIPTION_FIELD;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;
import static no.sikt.nva.apitest.publication.PublicationFields.PUBLICATION_CONTEXT;
import static no.sikt.nva.apitest.publication.PublicationFields.PUBLICATION_INSTANCE;
import static no.sikt.nva.apitest.publication.PublicationFields.REFERENCE;
import static no.sikt.nva.apitest.publication.PublicationFields.TYPE;
import static no.sikt.nva.apitest.publication.PublicationPaths.publicationPath;
import static no.sikt.nva.apitest.publication.PublicationPaths.publishPublicationPath;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import no.sikt.Category;
import no.sikt.Contributor;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.publication.PublicationPaths;

public class PublicationFactory {

  private static final String ACCESS_TOKEN = "accessToken";
  private static final String YEAR_PLACEHOLDER = "{year}";

  private JsonPath loadMetadataResource(String filename) {
    var template = stringFromResources(Path.of("metadata", filename));
    return new JsonPath(template.replace(YEAR_PLACEHOLDER, CURRENT_YEAR));
  }

  public Response createDraftPublication(User user) {
    return createDraftPublicationUsingToken(CognitoLogin.loginUser(user).get(ACCESS_TOKEN));
  }

  public Response createDraftPublicationUsingToken(String accessToken) {
    return givenAuthenticatedJsonRequest(accessToken)
        .post(PublicationPaths.createPublicationPath())
        .then()
        .statusCode(201)
        .extract()
        .response();
  }

  public Response updatePublication(User user, Map<String, Object> payload) {
    return updatePublicationUsingToken(CognitoLogin.loginUser(user).get(ACCESS_TOKEN), payload);
  }

  public Response updatePublicationUsingToken(String accessToken, Map<String, Object> payload) {
    return givenAuthenticatedJsonRequest(accessToken)
        .body(payload)
        .put(publicationPath(payload.get(IDENTIFIER_FIELD).toString()))
        .then()
        .statusCode(200)
        .extract()
        .response();
  }

  /**
   * Creates a published publication of the given category with default UIB users, handling
   * category-specific requirements: chapters get a containing anthology and degrees are created and
   * published by a thesis curator. The anthology gets its own random title so that searches scoped
   * to the chapter title do not match it.
   *
   * @return the identifier of the created publication
   */
  public String createPublishedPublication(Category category, String title) {
    var contributors = List.of(new Contributor(UIB_CREATOR, CREATOR));
    return switch (category) {
      case ACADEMIC_CHAPTER -> createChapterWithAnthology(title, contributors);
      case DEGREE_PHD, DEGREE_MASTER ->
          createPublishedPublication(
              UIB_THESIS_CURATOR, title, category, contributors, UIB_THESIS_CURATOR);
      default ->
          createPublishedPublication(
              UIB_CREATOR, title, category, contributors, UIB_PUBLISHING_CURATOR);
    };
  }

  private String createChapterWithAnthology(String title, List<Contributor> contributors) {
    var anthologyIdentifier =
        createAnthologyForChapter(
            UIB_CREATOR, "chapter " + UUID.randomUUID(), UIB_PUBLISHING_CURATOR, contributors);

    return createChapterInAnthology(
        UIB_CREATOR,
        title,
        ACADEMIC_CHAPTER,
        contributors,
        UIB_PUBLISHING_CURATOR,
        anthologyIdentifier);
  }

  public String createPublishedPublication(
      User user, String title, Category category, List<Contributor> contributorList, User curator) {
    return createPublishedPublicationUsingTokens(
        CognitoLogin.loginUser(user).get(ACCESS_TOKEN),
        title,
        category,
        contributorList,
        CognitoLogin.loginUser(curator).get(ACCESS_TOKEN));
  }

  public String createPublishedPublicationUsingTokens(
      String accessToken,
      String title,
      Category category,
      List<Contributor> contributorList,
      String curatorAccessToken) {

    return createPublishedPublicationWithReferenceUsingTokens(
        accessToken, title, category, contributorList, curatorAccessToken, new HashMap<>());
  }

  public String createPublishedPublicationWithReference(
      User user,
      String title,
      Category category,
      List<Contributor> contributorList,
      User curator,
      Map<String, Object> reference) {

    return createPublishedPublicationWithReferenceUsingTokens(
        CognitoLogin.loginUser(user).get(ACCESS_TOKEN),
        title,
        category,
        contributorList,
        CognitoLogin.loginUser(curator).get(ACCESS_TOKEN),
        reference);
  }

  public String createPublishedPublicationWithReferenceUsingTokens(
      String accessToken,
      String title,
      Category category,
      List<Contributor> contributorList,
      String curatorAccessToken,
      Map<String, Object> reference) {

    var createResponse = createDraftPublicationUsingToken(accessToken);

    var identifier = createResponse.body().jsonPath().getString(IDENTIFIER_FIELD);
    Map<String, Object> responseBody = createResponse.body().jsonPath().getMap("");
    responseBody.remove(CONTEXT_FIELD);

    var entityDescription = createEntityDescription(title, category, contributorList);
    if (reference.containsKey(PUBLICATION_CONTEXT)) {
      ((Map<String, Object>) reference.get(PUBLICATION_CONTEXT))
          .forEach(
              (key, value) ->
                  ((Map<String, Object>)
                          ((Map<String, Object>) entityDescription.get(REFERENCE))
                              .get(PUBLICATION_CONTEXT))
                      .put(key, value));
    }
    if (reference.containsKey(PUBLICATION_INSTANCE)) {
      ((Map<String, Object>) reference.get(PUBLICATION_INSTANCE))
          .forEach(
              (key, value) ->
                  ((Map<String, Object>)
                          ((Map<String, Object>) entityDescription.get(REFERENCE))
                              .get(PUBLICATION_INSTANCE))
                      .put(key, value));
    }

    responseBody.put(ENTITY_DESCRIPTION_FIELD, entityDescription);

    updatePublicationUsingToken(accessToken, responseBody);

    publishUsingToken(curatorAccessToken, identifier);

    return createResponse.jsonPath().get(IDENTIFIER_FIELD);
  }

  public Map<String, Object> buildReferenceMap(
      Map<String, Object> publicationContextMap, Map<String, Object> publicationInstanceMap) {
    return Map.of(
        PUBLICATION_CONTEXT, publicationContextMap, PUBLICATION_INSTANCE, publicationInstanceMap);
  }

  public String createChapterInAnthology(
      User user,
      String title,
      Category category,
      List<Contributor> contributorList,
      User curator,
      String anthologyIdentifier) {
    return createChapterInAnthologyUsingToken(
        CognitoLogin.loginUser(user).get(ACCESS_TOKEN),
        title,
        category,
        contributorList,
        CognitoLogin.loginUser(curator).get(ACCESS_TOKEN),
        anthologyIdentifier);
  }

  public String createChapterInAnthologyUsingToken(
      String accessToken,
      String title,
      Category category,
      List<Contributor> contributorList,
      String curatorAccessToken,
      String anthologyIdentifier) {

    var referenceMap =
        buildReferenceMap(
            new HashMap<>(Map.of("id", baseURI + publicationPath(anthologyIdentifier))),
            new HashMap<>());

    return createPublishedPublicationWithReferenceUsingTokens(
        accessToken, title, category, contributorList, curatorAccessToken, referenceMap);
  }

  public String createAnthologyForChapter(
      User user, String title, User curator, List<Contributor> anthologyEditorList) {
    return createAnthologyForChapterUsingTokens(
        CognitoLogin.loginUser(user).get(ACCESS_TOKEN),
        title,
        CognitoLogin.loginUser(curator).get(ACCESS_TOKEN),
        anthologyEditorList);
  }

  public String createAnthologyForChapterUsingTokens(
      String accessToken,
      String title,
      String curatorAccessToken,
      List<Contributor> anthologyEditorList) {
    var anthologyTitle = "Anthology for " + title;

    return createPublishedPublicationUsingTokens(
        accessToken, anthologyTitle, BOOK_ANTHOLOGY, anthologyEditorList, curatorAccessToken);
  }

  public Map<String, Object> createEntityDescription(
      String title, Category category, List<Contributor> contributorList) {

    var entityDescriptionJsonPath = loadMetadataResource("EntityDescription.json");

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
    entityDescription.put(REFERENCE, reference);
    List<Map<String, Object>> contributors = createContributors(contributorList);
    entityDescription.put(CONTRIBUTORS, contributors);

    return entityDescription;
  }

  public Map<String, Object> createReference(Category category) {
    var referenceJsonPath = loadMetadataResource(category.getValue() + "Reference.json");
    return referenceJsonPath.getMap(REFERENCE);
  }

  public void publish(User curator, String identifier) {
    publishUsingToken(CognitoLogin.loginUser(curator).get(ACCESS_TOKEN), identifier);
  }

  public void publishUsingToken(String curatorAccessToken, String identifier) {
    givenAuthenticatedJsonRequest(curatorAccessToken)
        .post(publishPublicationPath(identifier))
        .then()
        .statusCode(202);
  }

  public List<Map<String, Object>> createContributors(List<Contributor> contributors) {

    List<Map<String, Object>> newContributors = new ArrayList<>();
    final var sequence = new AtomicInteger(1);
    var contributorJsonPath = loadMetadataResource("Contributor.json");
    contributors.forEach(
        contributor -> {
          Map<String, Object> newContributor =
              createContributor(sequence, contributorJsonPath, contributor);

          newContributors.add(newContributor);
        });

    return newContributors;
  }

  private Map<String, Object> createContributor(
      final AtomicInteger sequence, JsonPath contributorJsonPath, Contributor contributor) {
    Map<String, Object> newContributor = new HashMap<>();
    newContributor.putAll(contributorJsonPath.getMap(""));
    ((Map<String, Object>) newContributor.get("role")).put(TYPE, contributor.role());
    newContributor.put("sequence", String.valueOf(sequence.getAndIncrement()));
    newContributor.put("identity", createIdentity(contributor.user()));

    List<Map<String, Object>> affiliations = new ArrayList<>();
    contributor
        .user()
        .affiliations()
        .forEach(
            userAffiliation -> {
              Map<String, Object> affiliation = new HashMap<>();
              affiliation.put(TYPE, "Organization");
              affiliation.put("id", userAffiliation);
              affiliations.add(affiliation);
            });
    newContributor.put("affiliations", affiliations);
    return newContributor;
  }

  private Map<String, Object> createIdentity(User user) {
    Map<String, Object> identity = new HashMap<>();
    identity.put(TYPE, "Identity");
    identity.put("id", baseURI + "/cristin/person/" + user.cristinId().split("@")[0]);
    identity.put("verificationStatus", "Verified");
    identity.put("name", user.name());
    return identity;
  }
}
