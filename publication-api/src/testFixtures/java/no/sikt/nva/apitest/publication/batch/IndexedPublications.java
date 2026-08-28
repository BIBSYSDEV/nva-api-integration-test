package no.sikt.nva.apitest.publication.batch;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.stream.Collectors.toSet;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.SettledCondition.settledWhen;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.publication.PublicationPaths.publicationPath;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import no.sikt.nva.PublicationFactory;
import nva.commons.core.paths.UriWrapper;

/**
 * Creates published publications that the batch handlers can find through the search api. The
 * handler pages through search results, so its tests need a set of publications that is both large
 * enough to span several pages and scoped tightly enough that a run cannot touch anything else in
 * the environment.
 *
 * <p>Scoping is by title: every publication in a set shares one random title token, and the search
 * parameter matching that token is what the handler is given as its filter.
 */
public final class IndexedPublications {

  private static final PublicationFactory PUBLICATION_FACTORY = new PublicationFactory();
  private static final String SEARCH_RESOURCES_PATH = "/search/resources";
  private static final String TITLE_PARAM = "title";
  private static final String AGGREGATION_PARAM = "aggregation";
  private static final String NO_AGGREGATION = "none";
  private static final String TOTAL_HITS_FIELD = "totalHits";
  private static final String TOKEN_PREFIX = "batchupdate";
  private static final String UUID_SEPARATOR = "-";
  private static final String SIZE_PARAM = "size";
  private static final String HIT_IDS_FIELD = "hits.id";
  private static final int NO_COUNT_YET = -1;
  private static final String FIRST_AFFILIATION_FIELD =
      "entityDescription.contributors[0].affiliations[0].id";

  // Indexing a set of this size lags well behind the create requests, and the whole test class is
  // blocked until every publication is searchable, so the budget is generous compared to the
  // default poll timeout.
  private static final Duration INDEXING_TIMEOUT = Duration.ofMinutes(8);

  private IndexedPublications() {}

  /**
   * A title token that no other publication in the environment can have, so that a search on it
   * returns this set and nothing else. The hyphens are stripped because the token has to survive
   * indexing as a single searchable term.
   */
  public static String randomTitleToken() {
    return TOKEN_PREFIX + UUID.randomUUID().toString().replace(UUID_SEPARATOR, "");
  }

  /**
   * Creates published publications sharing the given title token and returns once all of them are
   * searchable. The creation runs in parallel because each publication costs three sequential
   * requests, which for a set of any size dominates the setup time.
   */
  public static List<String> createSearchable(int count, String titleToken) {
    var identifiers =
        IntStream.range(0, count)
            .parallel()
            .mapToObj(index -> createPublication(titleToken))
            .toList();
    awaitSearchable(count, titleToken);
    return identifiers;
  }

  /** Blocks until the search api reports exactly the expected number of publications. */
  public static void awaitSearchable(int count, String titleToken) {
    pollUntil(INDEXING_TIMEOUT, () -> searchableCount(titleToken), hits -> hits == count);
  }

  /**
   * Blocks until the number of searchable publications has stopped growing, and returns it.
   *
   * <p>Publishing is only accepted synchronously, not completed: the request returns 202 and the
   * rest happens downstream. Under the load of a whole test run a publication occasionally never
   * arrives in the index, so waiting for an exact count fails the entire suite over one or two
   * publications no test needed individually. Waiting for the count to settle instead lets the
   * tests work from what is actually there.
   *
   * @param minimumCount the point below which the set is too small to be worth testing against
   */
  public static int awaitStableCount(String titleToken, int minimumCount) {
    var previousCount = new AtomicInteger(NO_COUNT_YET);
    return pollUntil(
        INDEXING_TIMEOUT,
        () -> searchableCount(titleToken),
        settledWhen(
            count -> hasSettled(count, minimumCount, previousCount),
            count ->
                "%d searchable, waiting for %d or more to hold across two checks"
                    .formatted(count, minimumCount)));
  }

  /**
   * Carries the previous reading, since settling is about two readings rather than one. Polling
   * evaluates the condition once per attempt, and the description is derived separately and stays
   * free of this.
   */
  private static boolean hasSettled(int count, int minimumCount, AtomicInteger previousCount) {
    return count >= minimumCount && count == previousCount.getAndSet(count);
  }

  /**
   * The identifiers of the publications the search api returns for this title token. What this set
   * is missing, compared to what was created, is what never made it through indexing.
   */
  public static Set<String> searchableIdentifiers(String titleToken, int maxResults) {
    return givenAuthenticatedJsonRequestAsUser(UIB_CREATOR)
        .queryParam(TITLE_PARAM, titleToken)
        .queryParam(AGGREGATION_PARAM, NO_AGGREGATION)
        .queryParam(SIZE_PARAM, String.valueOf(maxResults))
        .get(SEARCH_RESOURCES_PATH)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath()
        .getList(HIT_IDS_FIELD, String.class)
        .stream()
        .map(IndexedPublications::identifierOf)
        .collect(toSet());
  }

  /** The search parameters that scope a handler run to the publications with this title token. */
  public static Map<String, String> searchParamsFor(String titleToken) {
    return Map.of(TITLE_PARAM, titleToken);
  }

  /**
   * The affiliation of the single contributor the factory gives a publication, read from the
   * publication itself rather than from search, so that it reflects what the handler wrote.
   */
  public static String contributorAffiliationOf(String identifier) {
    return givenAuthenticatedJsonRequestAsUser(UIB_CREATOR)
        .get(publicationPath(identifier))
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath()
        .getString(FIRST_AFFILIATION_FIELD);
  }

  private static String identifierOf(String publicationId) {
    return UriWrapper.fromUri(publicationId).getLastPathElement();
  }

  private static String createPublication(String titleToken) {
    return PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, titleToken);
  }

  private static int searchableCount(String titleToken) {
    return givenAuthenticatedJsonRequestAsUser(UIB_CREATOR)
        .queryParam(TITLE_PARAM, titleToken)
        .queryParam(AGGREGATION_PARAM, NO_AGGREGATION)
        .get(SEARCH_RESOURCES_PATH)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath()
        .getInt(TOTAL_HITS_FIELD);
  }
}
