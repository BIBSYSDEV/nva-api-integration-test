package no.sikt;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import io.restassured.specification.RequestSpecification;

public final class Requests {

  private static final String BEARER_PREFIX = "Bearer ";

  private Requests() {}

  public static RequestSpecification givenAuthenticatedRequest(String accessToken) {
    return given().header(AUTHORIZATION, BEARER_PREFIX + accessToken);
  }

  public static RequestSpecification givenAuthenticatedRequestAsUser(User user) {
    return givenAuthenticatedRequestAsUser(user.userId());
  }

  public static RequestSpecification givenAuthenticatedRequestAsUser(String userId) {
    var accessToken = CognitoLogin.login(userId).get("accessToken");
    return givenAuthenticatedRequest(accessToken);
  }

  public static RequestSpecification givenAuthenticatedJsonRequest(String accessToken) {
    return givenAuthenticatedRequest(accessToken)
        .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
        .header(ACCEPT, APPLICATION_JSON.getMimeType());
  }

  public static RequestSpecification givenAuthenticatedJsonRequestAsUser(User user) {
    return givenAuthenticatedJsonRequestAsUser(user.userId());
  }

  public static RequestSpecification givenAuthenticatedJsonRequestAsUser(String userId) {
    var accessToken = CognitoLogin.login(userId).get("accessToken");
    return givenAuthenticatedJsonRequest(accessToken);
  }

  public static RequestSpecification givenAuthenticatedFormRequestAsUser(User user) {
    return givenAuthenticatedRequestAsUser(user)
        .header(CONTENT_TYPE, APPLICATION_FORM_URLENCODED.getMimeType());
  }
}
