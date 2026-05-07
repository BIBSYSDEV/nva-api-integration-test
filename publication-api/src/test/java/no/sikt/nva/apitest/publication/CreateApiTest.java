package no.sikt.nva.apitest.publication;

import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedRequest;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.base.UserFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class CreateApiTest extends PublicationTestBase {

  private static String customerUib;
  private static String creatorAccessToken;

  private static final String IDENTIFIER = "identifier";
  private static final String RESOURCE_OWNER = "resourceOwner";

  @BeforeAll
  static void init() {

    customerUib = RestAssured.baseURI + "/customer/a228aba6-932b-4f53-b2de-31ad8daf9f8d";

    creatorAccessToken = CognitoLogin.login(UserFixtures.UIB_CREATOR.userId()).get("accessToken");
  }

  @Test
  @DisplayName("Creator create draft publication")
  @Description(
      "A Creator calling create publication should return publication metadata and statuscode 201"
          + " Created")
  void shouldCreateDraftPublicationOwnedByCreator() {
    var today =
        LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    givenAuthenticatedRequest(creatorAccessToken)
        .accept(ContentType.JSON)
        .when()
        .post(PUBLICATION_PATH)
        .then()
        .statusCode(201)
        .body("type", equalTo("Publication"))
        .body(IDENTIFIER, notNullValue())
        .body("status", equalTo("DRAFT"))
        .appendRootPath(RESOURCE_OWNER)
        .body("owner", equalTo(UserFixtures.UIB_CREATOR.cristinId()))
        .body("ownerAffiliation", equalTo(Affiliation.UIB.getValue()))
        .detachRootPath(RESOURCE_OWNER)
        .appendRootPath("publisher")
        .body("type", equalTo("Organization"))
        .body("id", equalTo(customerUib))
        .detachRootPath("publisher")
        .body("createdDate", startsWith(today))
        .body("modifiedDate", startsWith(today));
  }
}
