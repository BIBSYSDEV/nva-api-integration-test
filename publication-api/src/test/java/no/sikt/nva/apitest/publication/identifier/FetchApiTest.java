package no.sikt.nva.apitest.publication.identifier;

import static io.restassured.RestAssured.given;
import static io.restassured.http.Method.GET;
import static java.net.HttpURLConnection.HTTP_GONE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.SettledCondition.settledWhen;
import static no.sikt.nva.apitest.base.UserFixtures.KRISTIANIA_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;
import static no.sikt.nva.apitest.publication.PublicationFields.RESOURCE_OWNER_FIELD;
import static no.sikt.nva.apitest.publication.PublicationPaths.publicationPath;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.UUID;
import java.util.concurrent.Callable;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("GET /publication/{identifier}")
class FetchApiTest extends PublicationTestBase {

  private static final String STATUS_FIELD = "status";
  private static final String UNPUBLISHED = "UNPUBLISHED";
  private static final String TOMBSTONE_RESOURCE_FIELD = "resource";
  private static String creatorAccessToken;
  private static String customerUib;

  @BeforeAll
  static void init() {
    customerUib = RestAssured.baseURI + "/customer/a228aba6-932b-4f53-b2de-31ad8daf9f8d";
    creatorAccessToken = CognitoLogin.login(UIB_CREATOR.userId()).get("accessToken");
  }

  /**
   * Fetch publication by identifier should return publication metadata and status {@code 200 OK}.
   */
  @Test
  @DisplayName("Fetch publication by identifier")
  @Description
  void shouldReturnDraftPublicationWhenFetchedByIdentifier(SoftAssertions softly) {
    var identifier = setupDraftPublication();

    var response =
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .when()
            .get(publicationPath(identifier))
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString(IDENTIFIER_FIELD)).isEqualTo(identifier);
    softly.assertThat(response.getString(STATUS_FIELD)).isEqualTo("DRAFT");
    softly
        .assertThat(response.getString(RESOURCE_OWNER_FIELD + ".owner"))
        .isEqualTo(UIB_CREATOR.cristinId());
    softly
        .assertThat(response.getString(RESOURCE_OWNER_FIELD + ".ownerAffiliation"))
        .isEqualTo(UIB.getValue());
    softly.assertThat(response.getString("publisher.type")).isEqualTo("Organization");
    softly.assertThat(response.getString("publisher.id")).isEqualTo(customerUib);
  }

  /** Fetch non-existing publication should return status {@code 404 Not Found}. */
  @Test
  @DisplayName("Fetch non-existing publication")
  @Description
  void shouldReturnNotFoundWhenFetchingUnknownIdentifier(SoftAssertions softly) {
    var randomIdentifier = UUID.randomUUID().toString();

    var response =
        givenAuthenticatedJsonRequest(creatorAccessToken)
            .when()
            .get(publicationPath(randomIdentifier))
            .then()
            .statusCode(HTTP_NOT_FOUND)
            .extract()
            .jsonPath();

    softly.assertThat(response.getString("title")).isEqualTo("Not Found");
    softly
        .assertThat(response.getString("detail"))
        .isEqualTo("Publication not found: " + randomIdentifier);
  }

  /** A non authorized user calling get should return status {@code 403 Forbidden}. */
  @Test
  @DisplayName("Non authorized user tries to fetch publication")
  @Disabled("FIXME: Returns 401, see NP-51618")
  @Description
  void shouldReturnForbiddenWhenNotOwnerFetchingDraftPublication() {
    var identifier = setupDraftPublication();

    requestShouldReturnForbidden(GET, UIB_CONTRIBUTOR, publicationPath(identifier));
  }

  /**
   * Fetching an unpublished publication without logging in should return a tombstone with status
   * {@code 410 Gone}, which still identifies the publication and says that it is unpublished.
   */
  @Test
  @DisplayName("Anonymous user fetches an unpublished publication")
  @Description
  void shouldReturnGoneWhenAnonymousUserFetchesUnpublishedPublication(SoftAssertions softly) {
    var identifier = setupUnpublishedPublication();

    var tombstone =
        awaitTombstone(() -> givenUnauthenticatedJsonRequest().get(publicationPath(identifier)));

    assertTombstoneFor(softly, tombstone, identifier);
  }

  /**
   * A user with no relation to an unpublished publication, at another institution, should also get
   * the tombstone with status {@code 410 Gone}.
   */
  @Test
  @DisplayName("Unrelated user fetches an unpublished publication")
  @Description
  void shouldReturnGoneWhenUnrelatedUserFetchesUnpublishedPublication(SoftAssertions softly) {
    var identifier = setupUnpublishedPublication();

    var tombstone =
        awaitTombstone(
            () ->
                givenAuthenticatedJsonRequestAsUser(KRISTIANIA_CREATOR)
                    .get(publicationPath(identifier)));

    assertTombstoneFor(softly, tombstone, identifier);
  }

  /**
   * Polls until the fetch returns the tombstone, since publication-api may still serve the
   * published publication right after it was unpublished (NP-52125).
   */
  private static JsonPath awaitTombstone(Callable<Response> fetchRequest) {
    return pollUntil(fetchRequest, settledWhen("status code", HTTP_GONE, Response::getStatusCode))
        .jsonPath();
  }

  /**
   * The tombstone is a problem details document whose {@code resource} field holds the publication
   * without its files.
   */
  private static void assertTombstoneFor(
      SoftAssertions softly, JsonPath tombstone, String identifier) {
    softly.assertThat(tombstone.getString("title")).isEqualTo("Gone");
    softly.assertThat(tombstone.getString("detail")).isEqualTo("Publication has been removed");
    softly
        .assertThat(tombstone.getString(TOMBSTONE_RESOURCE_FIELD + "." + IDENTIFIER_FIELD))
        .isEqualTo(identifier);
    softly
        .assertThat(tombstone.getString(TOMBSTONE_RESOURCE_FIELD + "." + STATUS_FIELD))
        .isEqualTo(UNPUBLISHED);
    softly
        .assertThat(tombstone.getList(TOMBSTONE_RESOURCE_FIELD + ".associatedArtifacts"))
        .as("files are not part of the tombstone")
        .isEmpty();
  }
}
