package no.sikt.nva.apitest.publication.identifier;

import static no.sikt.Role.CREATOR;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;
import static no.sikt.nva.apitest.publication.PublicationPaths.publishPublicationPath;

import io.qameta.allure.Description;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.Category;
import no.sikt.Contributor;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@ExtendWith(SoftAssertionsExtension.class)
class PublishApiTest extends PublicationTestBase {

  @InjectSoftAssertions private SoftAssertions softly;
  private static String curatorAccessToken;

  @BeforeAll
  static void init() {
    curatorAccessToken = CognitoLogin.login(UIB_CREATOR.userId()).get("accessToken");
  }

  /** A Curator calling publish should return statuscode 202 Accepted. */
  @ParameterizedTest
  @EnumSource(
      value = Category.class,
      names = {"ACADEMIC_ARTICLE", "ACADEMIC_MONOGRAPH"})
  @DisplayName("Curator publish draft publication")
  @Description(useJavaDoc = true)
  void shouldPublishDraftWhenRequestedByCurator(Category category) {

    var createResponse = PUBLICATION_FACTORY.createDraftPublication(UIB_CREATOR);
    var identifier = createResponse.jsonPath().getString(IDENTIFIER_FIELD);
    Map<String, Object> responseBody = createResponse.body().jsonPath().getMap("");

    String publishPublicationTitle =
        "Integration test publication " + category.name() + " " + UUID.randomUUID();
    Map<String, ?> entityDescription =
        PUBLICATION_FACTORY.createEntityDescription(
            publishPublicationTitle, category, List.of(new Contributor(UIB_CREATOR, CREATOR)));
    responseBody.put("entityDescription", entityDescription);

    PUBLICATION_FACTORY.updatePublication(UIB_CREATOR, responseBody);

    givenAuthenticatedRequest(curatorAccessToken)
        .accept(ContentType.JSON)
        .when()
        .post(publishPublicationPath(identifier))
        .then()
        .statusCode(202);
  }

  /** Publishing an incomplete publication should return 400 Bad Request. */
  @Test
  @DisplayName("Publish incomplete publication")
  @Description(useJavaDoc = true)
  void shouldRejectPublishWhenMetadataIsIncomplete() {
    var identifier = setupDraftPublication();

    var response =
        givenAuthenticatedJsonRequest(curatorAccessToken)
            .when()
            .post(publishPublicationPath(identifier))
            .then()
            .statusCode(400)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("title")).isEqualTo("Bad Request");
    softly.assertThat(response.getString("detail")).isEqualTo("Resource is not publishable!");
  }

  /** A non-curator user publishing a publication should return 401 Unauthorized. */
  @Test
  @DisplayName("Non-curator publish publication")
  @Description(useJavaDoc = true)
  void shouldRejectPublishWhenUserIsNotCurator() {
    var creatorAccessToken = CognitoLogin.login(UIB_CREATOR.userId()).get("accessToken");

    var identifier = setupDraftPublication();

    var response =
        givenAuthenticatedJsonRequest(creatorAccessToken)
            .when()
            .post(publishPublicationPath(identifier))
            .then()
            .statusCode(400)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("title")).isEqualTo("Bad Request");
    softly.assertThat(response.getString("detail")).isEqualTo("Resource is not publishable!");
  }
}
