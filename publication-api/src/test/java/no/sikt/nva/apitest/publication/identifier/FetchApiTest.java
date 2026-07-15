package no.sikt.nva.apitest.publication.identifier;

import static io.restassured.RestAssured.given;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;
import static no.sikt.nva.apitest.publication.PublicationFields.RESOURCE_OWNER_FIELD;
import static no.sikt.nva.apitest.publication.PublicationPaths.publicationPath;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.UUID;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class FetchApiTest extends PublicationTestBase {

  private static String creatorAccessToken;
  private static String customerUib;

  @BeforeAll
  static void init() {
    customerUib = RestAssured.baseURI + "/customer/a228aba6-932b-4f53-b2de-31ad8daf9f8d";
    creatorAccessToken = CognitoLogin.login(UIB_CREATOR.userId()).get("accessToken");
  }

  @Test
  @DisplayName("Fetch publication by identifier")
  @Description(
      "Fetch publication by identifier should return publication metadata and statuscode 200 Ok")
  void shouldReturnDraftPublicationWhenFetchedByIdentifier(SoftAssertions softly) {
    var identifier = setupDraftPublication();

    var response =
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .when()
            .get(publicationPath(identifier))
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString(IDENTIFIER_FIELD)).isEqualTo(identifier);
    softly.assertThat(response.getString("status")).isEqualTo("DRAFT");
    softly
        .assertThat(response.getString(RESOURCE_OWNER_FIELD + ".owner"))
        .isEqualTo(UIB_CREATOR.cristinId());
    softly
        .assertThat(response.getString(RESOURCE_OWNER_FIELD + ".ownerAffiliation"))
        .isEqualTo(UIB.getValue());
    softly.assertThat(response.getString("publisher.type")).isEqualTo("Organization");
    softly.assertThat(response.getString("publisher.id")).isEqualTo(customerUib);
  }

  @Test
  @DisplayName("Fetch non-existing publication")
  @Description("Fetch non-existing publication should return statuscode 404 Not Found")
  void shouldReturnNotFoundWhenFetchingUnknownIdentifier(SoftAssertions softly) {
    var randomIdentifier = UUID.randomUUID().toString();

    var response =
        givenAuthenticatedJsonRequest(creatorAccessToken)
            .when()
            .get(publicationPath(randomIdentifier))
            .then()
            .statusCode(404)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("title")).isEqualTo("Not Found");
    softly
        .assertThat(response.getString("detail"))
        .isEqualTo("Publication not found: " + randomIdentifier);
  }
}
