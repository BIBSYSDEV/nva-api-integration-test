package no.sikt.nva.apitest.search.resources.jsonld;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import no.sikt.Contributor;
import no.sikt.Role;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.search.SearchApiTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

// The search endpoint path and the schema.org profile IRI are the fixed API contract under test.
@SuppressWarnings("java:S1075")
@ExtendWith(SoftAssertionsExtension.class)
class JsonLdVolumeTest extends SearchApiTestBase {

  @InjectSoftAssertions private SoftAssertions softly;

  private static final int NUMBER_OF_TEST_PUBLICATIONS = 100;
  private static final int PAGE_SIZE = 10;
  private static final String VOLUME_UUID = UUID.randomUUID().toString();

  private static final String APPLICATION_LD_JSON = "application/ld+json";
  private static final String X_TOTAL_COUNT = "X-Total-Count";
  private static final String LINK = "Link";
  private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
  private static final String EXPECTED_EXPOSED_HEADERS = "Link, X-Total-Count";

  private static final String SCHEMA_ORG_PROFILE_LINK = "<https://schema.org>; rel=\"profile\"";
  private static final String REL_FIRST = "rel=\"first\"";
  private static final String REL_NEXT = "rel=\"next\"";

  private static final String NUMBER_OF_ITEMS_POINTER = "numberOfItems";
  private static final String ITEM_LIST_ELEMENT_POINTER = "itemListElement";

  @BeforeAll
  @Timeout(value = 15, unit = MINUTES)
  static void init() {

    var userAccessToken = CognitoLogin.login(UIB_CREATOR.userId()).get("accessToken");
    var curatorAccessToken = CognitoLogin.login(UIB_PUBLISHING_CURATOR.userId()).get("accessToken");

    IntStream.range(0, NUMBER_OF_TEST_PUBLICATIONS)
        .parallel()
        .forEach(
            index -> {
              var title =
                  "JsonLd volume test using tokens "
                      + index
                      + " "
                      + VOLUME_UUID
                      + " "
                      + UUID.randomUUID();
              PUBLICATION_FACTORY.createPublishedPublicationUsingTokens(
                  userAccessToken,
                  title,
                  ACADEMIC_ARTICLE,
                  List.of(new Contributor(UIB_CREATOR, Role.CREATOR)),
                  curatorAccessToken);
            });
  }

  @Test
  @DisplayName("Paginated JSON-LD response has profile and pagination headers")
  @Description(
      "A multi-page schema.org response exposes X-Total-Count, Access-Control-Expose-Headers and a"
          + " Link header with the schema.org profile plus first/next pagination links")
  void shouldReturnProfileAndPaginationHeadersForPaginatedJsonLd() {

    with()
        .pollInterval(fibonacci().with().unit(SECONDS))
        .ignoreExceptions()
        .await()
        .atMost(120, SECONDS)
        .until(
            () ->
                getResponse(VOLUME_UUID, PAGE_SIZE)
                    .header(X_TOTAL_COUNT)
                    .equals(Integer.toString(NUMBER_OF_TEST_PUBLICATIONS)));

    var response = getResponse(VOLUME_UUID, PAGE_SIZE);
    var body = itemList(response);

    softly
        .assertThat(response.header(X_TOTAL_COUNT))
        .isEqualTo(Integer.toString(NUMBER_OF_TEST_PUBLICATIONS));
    softly
        .assertThat(response.header(ACCESS_CONTROL_EXPOSE_HEADERS))
        .isEqualTo(EXPECTED_EXPOSED_HEADERS);
    softly.assertThat(response.header(LINK)).contains(SCHEMA_ORG_PROFILE_LINK, REL_FIRST, REL_NEXT);
    softly.assertThat(body.getInt(NUMBER_OF_ITEMS_POINTER)).isEqualTo(NUMBER_OF_TEST_PUBLICATIONS);
    softly.assertThat(body.getList(ITEM_LIST_ELEMENT_POINTER)).hasSize(PAGE_SIZE);
  }

  @Test
  @DisplayName("Single-page JSON-LD response has profile link but no pagination links")
  @Description(
      "A schema.org response that fits on a single page still exposes the profile Link but no"
          + " first/next pagination links")
  void shouldReturnProfileLinkWithoutPaginationWhenSinglePage() {

    var response = getResponse(VOLUME_UUID, NUMBER_OF_TEST_PUBLICATIONS * 2);
    var body = itemList(response);

    softly.assertThat(response.header(LINK)).contains(SCHEMA_ORG_PROFILE_LINK);
    softly.assertThat(response.header(LINK)).doesNotContain(REL_NEXT);
    softly.assertThat(body.getInt(NUMBER_OF_ITEMS_POINTER)).isEqualTo(NUMBER_OF_TEST_PUBLICATIONS);
    softly.assertThat(body.getList(ITEM_LIST_ELEMENT_POINTER)).hasSize(NUMBER_OF_TEST_PUBLICATIONS);
  }

  @Test
  @DisplayName("Empty JSON-LD result is an empty ItemList carrying the profile link")
  @Description(
      "A schema.org search with no hits returns an empty ItemList, X-Total-Count 0 and the profile"
          + " Link header")
  void shouldReturnEmptyItemListWhenSearchReturnsNoHits() {

    var response = getResponse(UUID.randomUUID().toString(), PAGE_SIZE);
    var body = itemList(response);

    softly.assertThat(response.header(X_TOTAL_COUNT)).isEqualTo("0");
    softly.assertThat(response.header(LINK)).contains(SCHEMA_ORG_PROFILE_LINK);
    softly.assertThat(response.header(LINK)).doesNotContain(REL_NEXT);
    softly.assertThat(body.getInt(NUMBER_OF_ITEMS_POINTER)).isEqualTo(0);
    softly.assertThat(body.getList(ITEM_LIST_ELEMENT_POINTER)).isEmpty();
  }

  private JsonPath itemList(Response response) {
    return JsonPath.from(response.body().asString());
  }

  private Response getResponse(String query, int size) {
    return given()
        .param("query", query)
        .param("size", Integer.toString(size))
        .accept(APPLICATION_LD_JSON)
        .when()
        .get("/search/resources")
        .then()
        .statusCode(200)
        .extract()
        .response();
  }
}
