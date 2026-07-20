package no.sikt.nva.apitest.search.resources.jsonld;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MINUTES;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import no.sikt.Contributor;
import no.sikt.Role;
import no.sikt.nva.apitest.base.CognitoLogin;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class JsonLdVolumeTest extends JsonLdTestBase {

  private static final int NUMBER_OF_TEST_PUBLICATIONS = 50;
  private static final int PAGE_SIZE = 10;
  private static final Duration VOLUME_INDEXING_TIMEOUT = Duration.ofMinutes(4);
  private static final String VOLUME_UUID = UUID.randomUUID().toString();

  private static final String X_TOTAL_COUNT = "X-Total-Count";
  private static final String LINK = "Link";
  private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
  private static final String EXPECTED_EXPOSED_HEADERS = "Link, X-Total-Count";

  private static final String SCHEMA_ORG_PROFILE_LINK = "<https://schema.org>; rel=\"profile\"";
  private static final String REL_FIRST = "rel=\"first\"";
  private static final String REL_NEXT = "rel=\"next\"";

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

  /**
   * A multi-page schema.org response exposes X-Total-Count, Access-Control-Expose-Headers and a
   * Link header with the schema.org profile plus first/next pagination links.
   */
  @Test
  @DisplayName("Paginated JSON-LD response has profile and pagination headers")
  @Description(useJavaDoc = true)
  @Timeout(value = 5, unit = MINUTES)
  void shouldReturnProfileAndPaginationHeadersForPaginatedJsonLd(SoftAssertions softly) {

    var response = awaitAllPublicationsIndexed(PAGE_SIZE);
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

  /**
   * A schema.org response that fits on a single page still exposes the profile Link but no
   * first/next pagination links.
   */
  @Test
  @DisplayName("Single-page JSON-LD response has profile link but no pagination links")
  @Description(useJavaDoc = true)
  @Timeout(value = 5, unit = MINUTES)
  void shouldReturnProfileLinkWithoutPaginationWhenSinglePage(SoftAssertions softly) {

    var response = awaitAllPublicationsIndexed(NUMBER_OF_TEST_PUBLICATIONS * 2);
    var body = itemList(response);

    softly.assertThat(response.header(LINK)).contains(SCHEMA_ORG_PROFILE_LINK);
    softly.assertThat(response.header(LINK)).doesNotContain(REL_NEXT);
    softly.assertThat(body.getInt(NUMBER_OF_ITEMS_POINTER)).isEqualTo(NUMBER_OF_TEST_PUBLICATIONS);
    softly.assertThat(body.getList(ITEM_LIST_ELEMENT_POINTER)).hasSize(NUMBER_OF_TEST_PUBLICATIONS);
  }

  /**
   * A schema.org search with no hits returns an empty ItemList, X-Total-Count 0 and the profile
   * Link header.
   */
  @Test
  @DisplayName("Empty JSON-LD result is an empty ItemList carrying the profile link")
  @Description(useJavaDoc = true)
  void shouldReturnEmptyItemListWhenSearchReturnsNoHits(SoftAssertions softly) {

    var response = getResponse(UUID.randomUUID().toString(), PAGE_SIZE);
    var body = itemList(response);

    softly.assertThat(response.header(X_TOTAL_COUNT)).isEqualTo("0");
    softly.assertThat(response.header(LINK)).contains(SCHEMA_ORG_PROFILE_LINK);
    softly.assertThat(response.header(LINK)).doesNotContain(REL_NEXT);
    softly.assertThat(body.getInt(NUMBER_OF_ITEMS_POINTER)).isEqualTo(0);
    softly.assertThat(body.getList(ITEM_LIST_ELEMENT_POINTER)).isEmpty();
  }

  private Response awaitAllPublicationsIndexed(int size) {
    return pollUntil(
        VOLUME_INDEXING_TIMEOUT,
        () -> getResponse(VOLUME_UUID, size),
        response ->
            Integer.toString(NUMBER_OF_TEST_PUBLICATIONS).equals(response.header(X_TOTAL_COUNT)));
  }

  private Response getResponse(String query, int size) {
    return given()
        .param("query", query)
        .param("size", Integer.toString(size))
        .accept(APPLICATION_LD_JSON)
        .when()
        .get(RESOURCES_PATH)
        .then()
        .statusCode(200)
        .extract()
        .response();
  }
}
