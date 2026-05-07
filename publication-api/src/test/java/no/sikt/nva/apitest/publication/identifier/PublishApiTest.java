package no.sikt.nva.apitest.publication.identifier;

import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequest;
import static org.hamcrest.Matchers.equalTo;

import io.qameta.allure.Description;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.Category;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.base.UserFixtures;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class PublishApiTest extends PublicationTestBase {

  private static final String IDENTIFIER = "identifier";
  private static String curatorAccessToken;

  @BeforeAll
  static void init() {
    curatorAccessToken = CognitoLogin.login(UserFixtures.UIB_CREATOR.userId()).get("accessToken");
  }

  @Test
  @DisplayName("Curator publish draft publication")
  @Description("A Curator calling publish should return statuscode 202 Accepted")
  void shouldPublishDraftWhenRequestedByCurator() {

    var createResponse = PUBLICATION_FACTORY.createDraftPublication(UserFixtures.UIB_CREATOR);
    var identifier = createResponse.jsonPath().getString(IDENTIFIER);
    Map<String, Object> responseBody = createResponse.body().jsonPath().getMap("");

    String publishPublicationTitle = "Integration test publication " + UUID.randomUUID();
    Map<String, ?> entityDescription =
        PUBLICATION_FACTORY.createEntityDescription(
            publishPublicationTitle, Category.ACADEMIC_ARTICLE, List.of(UserFixtures.UIB_CREATOR));
    responseBody.put("entityDescription", entityDescription);

    PUBLICATION_FACTORY.updatePublication(UserFixtures.UIB_CREATOR, responseBody);

    givenAuthenticatedRequest(curatorAccessToken)
        .accept(ContentType.JSON)
        .when()
        .post(PUBLICATION_PATH + identifier + "/publish")
        .then()
        .statusCode(202);
  }

  @Test
  @DisplayName("Publish incomplete publication")
  @Description("Publishing an incomplete publication should return 400 Bad Request")
  void shouldRejectPublishWhenMetadataIsIncomplete() {
    var identifier =
        PUBLICATION_FACTORY
            .createDraftPublication(UserFixtures.UIB_CREATOR)
            .jsonPath()
            .getString(IDENTIFIER);

    givenAuthenticatedJsonRequest(curatorAccessToken)
        .when()
        .post(PUBLICATION_PATH + identifier + "/publish")
        .then()
        .statusCode(400)
        .body("title", equalTo("Bad Request"))
        .body("detail", equalTo("Resource is not publishable!"));
  }
}
