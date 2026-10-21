package no.sikt.nva.apitest.scopus;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.SettledCondition.settledWhen;

import io.restassured.path.json.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import no.sikt.nva.apitest.base.User;

/**
 * The import candidate APIs. A candidate is found through the search, which is the only lookup by
 * Scopus identifier, and then fetched in full: the search document leaves out {@code
 * associatedCustomers} and holds at most five contributors.
 */
final class ImportCandidates {

  static final String SEARCH_PATH = "/search/customer/import-candidates";
  static final String CANDIDATE_PATH = "/publication/import-candidate/{identifier}";

  /**
   * The record passes the queue, the handler, the table stream, the event bus and the search
   * indexing before it can be found, which takes well over a minute in e2e.
   */
  private static final Duration INDEXING_TIMEOUT = Duration.ofMinutes(5);

  private static final String TOTAL_HITS = "totalHits";
  private static final String FIRST_HIT_ID = "hits[0].id";

  private ImportCandidates() {}

  /** Waits until exactly one candidate with the Scopus identifier is searchable. */
  static String awaitCandidateIdentifier(User user, String scopusIdentifier) {
    var searchResult =
        pollUntil(
            INDEXING_TIMEOUT,
            () -> search(user, scopusIdentifier),
            settledWhen(TOTAL_HITS, 1, result -> result.getInt(TOTAL_HITS)));
    var candidateId = searchResult.getString(FIRST_HIT_ID);
    return candidateId.substring(candidateId.lastIndexOf('/') + 1);
  }

  static JsonPath search(User user, String scopusIdentifier) {
    return givenAuthenticatedJsonRequestAsUser(user)
        .queryParam("scopusIdentifier", scopusIdentifier)
        .queryParam("aggregation", "none")
        .when()
        .get(SEARCH_PATH)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath();
  }

  static JsonPath fetch(User user, String candidateIdentifier) {
    return givenAuthenticatedJsonRequestAsUser(user)
        .when()
        .get(CANDIDATE_PATH, candidateIdentifier)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath();
  }

  /**
   * Imports the candidate as a publication with the given entity description, the way a curator
   * does after checking contributors and affiliations. Requires MANAGE_IMPORT.
   */
  static void importAsPublication(
      User curator, String candidateIdentifier, Map<String, Object> entityDescription) {
    givenAuthenticatedJsonRequestAsUser(curator)
        .body(Map.of("entityDescription", entityDescription, "associatedArtifacts", List.of()))
        .when()
        .post(CANDIDATE_PATH, candidateIdentifier)
        .then()
        .statusCode(HTTP_CREATED);
  }

  /** The id of the contributor identity that carries the Scopus AUID, or null if it has none. */
  static String identityIdOfAuthor(JsonPath candidate, String auid) {
    return candidate.param("auid", auid).getString(contributorWithAuid() + ".identity.id");
  }

  static String nameOfAuthor(JsonPath candidate, String auid) {
    return candidate.param("auid", auid).getString(contributorWithAuid() + ".identity.name");
  }

  /** The ids of the target organizations of the contributor that carries the Scopus AUID. */
  static List<String> affiliationIdsOfAuthor(JsonPath candidate, String auid) {
    return candidate
        .param("auid", auid)
        .getList(contributorWithAuid() + ".affiliations.targetOrganization.id", String.class);
  }

  private static String contributorWithAuid() {
    return "entityDescription.contributors.find {"
        + " it.identity.additionalIdentifiers.any { identifier ->"
        + " identifier.sourceName == 'scopus-auid' && identifier.value == auid } }";
  }
}
