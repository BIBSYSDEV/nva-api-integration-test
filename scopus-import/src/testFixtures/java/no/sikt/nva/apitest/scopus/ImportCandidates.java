package no.sikt.nva.apitest.scopus;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Collections.emptyList;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.SettledCondition.settledWhen;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.FIXTURE_PERSON_NAME;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.IMPORT_CURATOR;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.fixturePersonId;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.fixtureUnitId;

import io.restassured.path.json.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import nva.commons.core.paths.UriWrapper;

/**
 * Candidates are found by search and then fetched, because the search hit lacks {@code
 * associatedCustomers} and has at most five contributors.
 */
public final class ImportCandidates {

  private static final String SEARCH_PATH = "/search/customer/import-candidates";
  private static final String CANDIDATE_PATH = "/publication/import-candidate/{identifier}";
  private static final Duration INDEXING_TIMEOUT = Duration.ofMinutes(5);
  private static final String TOTAL_HITS = "totalHits";
  private static final String FIRST_HIT_ID = "hits[0].id";
  private static final String IDENTIFIER = "identifier";
  private static final String TYPE = "type";
  private static final String SCOPUS_AUID_SOURCE = "scopus-auid";
  private static final String CONTRIBUTOR_WITH_AUID =
      "entityDescription.contributors.find {"
          + " it.identity.additionalIdentifiers.any { identifier ->"
          + " identifier.sourceName == source && identifier.value == auid } }";

  private ImportCandidates() {}

  public static JsonPath awaitCandidate(String scopusIdentifier) {
    return fetch(awaitCandidateIdentifier(scopusIdentifier));
  }

  public static int totalHits(String scopusIdentifier) {
    return search(scopusIdentifier).getInt(TOTAL_HITS);
  }

  /** The import stores the person set on an author as the mapping for that author's AUID. */
  public static void importWithFixturePersonAsOnlyAuthor(JsonPath candidate, String auid) {
    var body =
        Map.of(
            "entityDescription",
            entityDescriptionWithFixturePerson(candidate, auid),
            "associatedArtifacts",
            emptyList());
    givenAuthenticatedJsonRequestAsUser(IMPORT_CURATOR)
        .body(body)
        .when()
        .post(CANDIDATE_PATH, candidate.getString(IDENTIFIER))
        .then()
        .statusCode(HTTP_CREATED);
  }

  public static String identityIdOfAuthor(JsonPath candidate, String auid) {
    return withAuthorParams(candidate, auid).getString(CONTRIBUTOR_WITH_AUID + ".identity.id");
  }

  public static String nameOfAuthor(JsonPath candidate, String auid) {
    return withAuthorParams(candidate, auid).getString(CONTRIBUTOR_WITH_AUID + ".identity.name");
  }

  public static List<String> affiliationIdsOfAuthor(JsonPath candidate, String auid) {
    return withAuthorParams(candidate, auid)
        .getList(CONTRIBUTOR_WITH_AUID + ".affiliations.targetOrganization.id", String.class);
  }

  private static JsonPath withAuthorParams(JsonPath candidate, String auid) {
    return candidate.param("source", SCOPUS_AUID_SOURCE).param("auid", auid);
  }

  private static JsonPath search(String scopusIdentifier) {
    return givenAuthenticatedJsonRequestAsUser(IMPORT_CURATOR)
        .queryParam("scopusIdentifier", scopusIdentifier)
        .queryParam("aggregation", "none")
        .when()
        .get(SEARCH_PATH)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath();
  }

  private static String awaitCandidateIdentifier(String scopusIdentifier) {
    var searchResult =
        pollUntil(
            INDEXING_TIMEOUT,
            () -> search(scopusIdentifier),
            settledWhen(TOTAL_HITS, 1, result -> result.getInt(TOTAL_HITS)));
    return UriWrapper.fromUri(searchResult.getString(FIRST_HIT_ID)).getLastPathElement();
  }

  private static JsonPath fetch(String candidateIdentifier) {
    return givenAuthenticatedJsonRequestAsUser(IMPORT_CURATOR)
        .when()
        .get(CANDIDATE_PATH, candidateIdentifier)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath();
  }

  private static Map<String, Object> entityDescriptionWithFixturePerson(
      JsonPath candidate, String auid) {
    return Map.of(
        TYPE,
        "EntityDescription",
        "mainTitle",
        candidate.getString("entityDescription.mainTitle"),
        "publicationDate",
        candidate.getMap("entityDescription.publicationDate"),
        "reference",
        candidate.getMap("entityDescription.reference"),
        "contributors",
        List.of(fixturePersonContributor(auid)));
  }

  private static Map<String, Object> fixturePersonContributor(String auid) {
    return Map.of(
        TYPE,
        "Contributor",
        "sequence",
        1,
        "role",
        Map.of(TYPE, "Creator"),
        "identity",
        fixturePersonIdentity(auid),
        "affiliations",
        List.of(Map.of(TYPE, "Organization", "id", fixtureUnitId())));
  }

  private static Map<String, Object> fixturePersonIdentity(String auid) {
    var scopusAuthorIdentifier =
        Map.of(TYPE, "AdditionalIdentifier", "sourceName", SCOPUS_AUID_SOURCE, "value", auid);
    return Map.of(
        TYPE,
        "Identity",
        "id",
        fixturePersonId(),
        "name",
        FIXTURE_PERSON_NAME,
        "additionalIdentifiers",
        List.of(scopusAuthorIdentifier));
  }
}
