package no.sikt.nva.apitest.kanalregister;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

/** Request helpers and JSON paths for the Kanalregister nva-api. */
public final class ChannelRegistryRequests {

  public static final String LEVEL_PATH = "levelElementDto.level";
  public static final String LEVEL_YEAR_PATH = "levelElementDto.year";
  public static final String LEVEL_DISPLAY_PATH = "levelElementDto.levelDisplay";
  public static final String SEARCH_HITS_PATH = "entityResultSetDto.pageresult";

  private static final int SEARCH_PAGE_SIZE = 50;

  private ChannelRegistryRequests() {}

  public static Response lookUp(
      ChannelRegistryEnvironment environment, String resource, String pid, int year) {
    return fetch("%s/%s/%s/%d".formatted(environment.getApiHost(), resource, pid, year));
  }

  public static Response lookUpWithoutYear(
      ChannelRegistryEnvironment environment, String resource, String pid) {
    return fetch("%s/%s/%s".formatted(environment.getApiHost(), resource, pid));
  }

  public static Response searchChannels(
      ChannelRegistryEnvironment environment,
      String resource,
      String parameterName,
      String value,
      int year) {
    return given()
        .accept(ContentType.JSON)
        .queryParam(parameterName, value)
        .queryParam("year", year)
        .queryParam("pageno", 0)
        .queryParam("pagecount", SEARCH_PAGE_SIZE)
        .get("%s/%s/channels".formatted(environment.getApiHost(), resource))
        .then()
        .statusCode(200)
        .extract()
        .response();
  }

  /** GPath expression selecting the search hit with the given PID. */
  public static String hitByPid(String pid) {
    return "%s.find { it.pid == '%s' }".formatted(SEARCH_HITS_PATH, pid);
  }

  private static Response fetch(String absoluteUrl) {
    return given()
        .accept(ContentType.JSON)
        .get(absoluteUrl)
        .then()
        .statusCode(200)
        .extract()
        .response();
  }
}
