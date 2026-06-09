package no.sikt.nva.apitest.publication;

import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequest;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import no.sikt.nva.apitest.base.CognitoLogin;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class CreateApiTest extends PublicationTestBase {

  @InjectSoftAssertions private SoftAssertions softly;

  private static String customerUib;
  private static String creatorAccessToken;

  @BeforeAll
  static void init() {

    customerUib = RestAssured.baseURI + "/customer/a228aba6-932b-4f53-b2de-31ad8daf9f8d";

    creatorAccessToken = CognitoLogin.login(UIB_CREATOR.userId()).get("accessToken");
  }

  @Test
  @DisplayName("Creator create draft publication")
  @Description(
      "A Creator calling create publication should return publication metadata and statuscode 201"
          + " Created")
  void shouldCreateDraftPublicationOwnedByCreator() {
    var today =
        LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    var response =
        givenAuthenticatedRequest(creatorAccessToken)
            .accept(ContentType.JSON)
            .when()
            .post(PublicationPaths.createPublicationPath())
            .then()
            .statusCode(201)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("type")).isEqualTo("Publication");
    softly.assertThat(response.getString(IDENTIFIER_FIELD)).isNotNull();
    softly.assertThat(response.getString("status")).isEqualTo("DRAFT");
    softly.assertThat(response.getString("resourceOwner.owner")).isEqualTo(UIB_CREATOR.cristinId());
    softly
        .assertThat(response.getString("resourceOwner.ownerAffiliation"))
        .isEqualTo(UIB.getValue());
    softly.assertThat(response.getString("publisher.type")).isEqualTo("Organization");
    softly.assertThat(response.getString("publisher.id")).isEqualTo(customerUib);
    softly.assertThat(response.getString("createdDate")).startsWith(today);
    softly.assertThat(response.getString("modifiedDate")).startsWith(today);
  }
}
