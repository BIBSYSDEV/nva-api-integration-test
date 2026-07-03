package no.sikt.nva.apitest.search.resources.bibtex;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import no.sikt.Contributor;
import no.sikt.Role;
import no.sikt.nva.apitest.base.CognitoLogin;
import no.sikt.nva.apitest.search.SearchTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class BibTexVolumeTest extends SearchTestBase {

  @InjectSoftAssertions private SoftAssertions softly;

  private static final int NUMBER_OF_TEST_PUBLICATIONS = 100;
  private static final String VOLUME_UUID = UUID.randomUUID().toString();

  private static final String TEXT_X_BIBTEX = "text/x-bibtex";
  private static final String X_TOTAL_COUNT = "X-Total-Count";
  private static final String LINK = "Link";

  @BeforeAll
  @Timeout(value = 15, unit = MINUTES)
  static void init() {

    var userAccessToken = CognitoLogin.login(UIB_CREATOR.userId()).get("accessToken");
    var curatorAccessToken = CognitoLogin.login(UIB_PUBLISHING_CURATOR.userId()).get("accessToken");

    IntStream.range(0, NUMBER_OF_TEST_PUBLICATIONS)
        .parallel()
        .forEach(
            i -> {
              var title =
                  "BibTex volume test using tokens "
                      + i
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

  private Response getResponse(String query, String size) {

    RestAssured.registerParser(TEXT_X_BIBTEX, Parser.TEXT);

    return given()
        .param("query", query)
        .param("size", size)
        .accept(TEXT_X_BIBTEX)
        .when()
        .get("/search/resources")
        .then()
        .statusCode(200)
        .contentType(TEXT_X_BIBTEX)
        .extract()
        .response();
  }

  @Test
  @DisplayName("Publication in BibTex format has correct headers")
  @Description(
      "A publication in BibTex format should have headers X-Total-Count and"
          + " Access-Control-Expose-Headers")
  void shouldReturnAllPublicationsInBibTexFormat() {

    with()
        .pollInterval(fibonacci().with().unit(SECONDS))
        .await()
        .atMost(60, SECONDS)
        .until(
            () ->
                getResponse(VOLUME_UUID, "10")
                    .header(X_TOTAL_COUNT)
                    .equals(Integer.toString(NUMBER_OF_TEST_PUBLICATIONS)));

    var response = getResponse(VOLUME_UUID, "10");

    softly.assertThat(response.header(X_TOTAL_COUNT)).isNotEmpty();
    softly
        .assertThat(response.header(X_TOTAL_COUNT))
        .isEqualTo(Integer.toString(NUMBER_OF_TEST_PUBLICATIONS));
    softly.assertThat(response.header(LINK)).isNotEmpty();
    softly.assertThat(response.header(LINK)).contains("rel=\"first\"", "rel=\"next\"");
    softly
        .assertThat(response.header("Access-Control-Expose-Headers"))
        .isEqualTo("Link, X-Total-Count");
  }

  @Test
  @DisplayName("A search that returns hits less than 'size'")
  @Description(
      "A search that returns a number of hits less than the size parameter should not return a"
          + " 'Link' header")
  void shouldNotReturnLinkHeaderWhenSearchReturnNumberOfHitsLessThanSize() {

    var response = getResponse(VOLUME_UUID, Integer.toString(NUMBER_OF_TEST_PUBLICATIONS * 2));

    assertThat(response.header(LINK)).isNullOrEmpty();
  }

  @Test
  @DisplayName("A search that returns no hits")
  @Description(
      "A search that returns no hits should return an empty body, size=0 and no 'Link' header")
  void shouldReturnEmptyBodyWhenSearchReturnsNoHits() {

    var response = getResponse(UUID.randomUUID().toString(), "10");

    softly.assertThat(response.body().asString()).isEqualTo("");
    softly.assertThat(response.header(X_TOTAL_COUNT)).isEqualTo("0");
    softly.assertThat(response.header(LINK)).isNullOrEmpty();
  }
}
