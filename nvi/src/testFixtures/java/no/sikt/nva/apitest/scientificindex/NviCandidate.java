package no.sikt.nva.apitest.scientificindex;

import io.restassured.RestAssured;

public record NviCandidate(
    String candidateIdentifier, String publicationIdentifier, String title, String creatorName) {

  public String publicationId() {
    return RestAssured.baseURI + "/publication/" + publicationIdentifier;
  }
}
