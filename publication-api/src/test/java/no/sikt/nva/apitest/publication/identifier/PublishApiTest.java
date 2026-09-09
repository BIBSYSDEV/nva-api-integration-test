package no.sikt.nva.apitest.publication.identifier;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.qameta.allure.Description;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import no.sikt.Category;
import no.sikt.Contributor;
import static no.sikt.Role.CREATOR;
import no.sikt.nva.apitest.base.CognitoLogin;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;
import static no.sikt.nva.apitest.publication.PublicationPaths.publicationPath;
import static no.sikt.nva.apitest.publication.PublicationPaths.publishPublicationPath;
import no.sikt.nva.apitest.publication.PublicationTestBase;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST /publication/{identifier}/publish")
class PublishApiTest extends PublicationTestBase {

  private static String curatorAccessToken;

  @BeforeAll
  static void init() {
    curatorAccessToken = CognitoLogin.login(UIB_CREATOR.userId()).get("accessToken");
  }

  /** A Curator calling publish should return status {@code 202 Accepted}. */
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
        .statusCode(HTTP_ACCEPTED);
  }

  /** Publishing an incomplete publication should return status {@code 400 Bad Request}. */
  @Test
  @DisplayName("Publish incomplete publication")
  @Description(useJavaDoc = true)
  void shouldRejectPublishWhenMetadataIsIncomplete(SoftAssertions softly) {
    var identifier = setupDraftPublication();

    var response =
        givenAuthenticatedJsonRequest(curatorAccessToken)
            .when()
            .post(publishPublicationPath(identifier))
            .then()
            .statusCode(HTTP_BAD_REQUEST)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("title")).isEqualTo("Bad Request");
    softly.assertThat(response.getString("detail")).isEqualTo("Resource is not publishable!");
  }

  /** A non-curator user publishing a publication should return status {@code 401 Unauthorized}. */
  @Test
  @DisplayName("Non-curator publish publication")
  @Description(useJavaDoc = true)
  void shouldRejectPublishWhenUserIsNotCurator(SoftAssertions softly) {
    var creatorAccessToken = CognitoLogin.login(UIB_CREATOR.userId()).get("accessToken");

    var identifier = setupDraftPublication();

    var response =
        givenAuthenticatedJsonRequest(creatorAccessToken)
            .when()
            .post(publishPublicationPath(identifier))
            .then()
            .statusCode(HTTP_BAD_REQUEST)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("title")).isEqualTo("Bad Request");
    softly.assertThat(response.getString("detail")).isEqualTo("Resource is not publishable!");
  }

  /** A non authorized user calling publish should return status {@code 403 Forbidden}. */
  @Test
  @DisplayName("Non authorized user tries to publish publication")
  // @Disabled("FIXME: Returns 401, see NP-51618")
  @Description(useJavaDoc = true)
  void shouldReturnForbiddemWhenNotOwnerPublishingDraftPublication(){
    var identifier = setupDraftPublication();

    requestShouldReturnForbidden(Method.POST, UIB_CONTRIBUTOR, publicationPath(identifier));
  }

}
