package no.sikt.nva.apitest.scientificindex;

import io.restassured.RestAssured;
import java.util.List;
import no.sikt.Contributor;
import no.sikt.nva.apitest.base.User;

public record NviCandidate(
    String candidateIdentifier,
    String publicationIdentifier,
    String title,
    List<Contributor> contributors) {

  public String publicationId() {
    return RestAssured.baseURI + "/publication/" + publicationIdentifier;
  }

  public List<String> creatorNames() {
    return contributors.stream()
        .filter(Contributor::isCreator)
        .map(Contributor::user)
        .map(User::name)
        .toList();
  }
}
