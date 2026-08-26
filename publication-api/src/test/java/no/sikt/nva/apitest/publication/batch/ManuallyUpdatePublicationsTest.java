package no.sikt.nva.apitest.publication.batch;

import static no.sikt.nva.apitest.base.Affiliation.KRISTIANIA;
import static no.sikt.nva.apitest.base.Affiliation.SIKT;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.publication.batch.IndexedPublications.contributorAffiliationOf;
import static no.sikt.nva.apitest.publication.batch.ManuallyUpdatePublications.CONTRIBUTOR_AFFILIATION;
import static no.sikt.nva.apitest.publication.batch.ManuallyUpdatePublications.run;

import io.qameta.allure.Description;
import java.util.HashMap;
import java.util.Map;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Exercises the flags and settings of ManuallyUpdatePublicationsHandler in nva-publication-api
 * against a deployed environment. The handler has no API: it is invoked directly with the AWS CLI
 * or SDK, so these tests invoke the lambda and read the report it writes to its output stream.
 *
 * <p>The paging logic is what a deployed run adds over the unit tests in nva-publication-api, which
 * mock the search api and therefore cannot show that the search_after cursor, the identifier sort
 * and the page size actually carry a run across pages. The set is sized to span several real pages.
 *
 * <p>Every test here runs as a dry run, so the shared set is never written to and the tests are
 * free to run concurrently against it. Persisting is covered by {@link
 * ManuallyUpdatePublicationsCommitTest}, which owns a set of its own.
 *
 * <p>Prerequisites: the test runner role has lambda:InvokeFunction on the handler and
 * tag:GetResources in the account.
 */
@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Manually update publications (lambda)")
class ManuallyUpdatePublicationsTest extends PublicationTestBase {

  private static final int MATCHING_PUBLICATIONS = 100;
  private static final int PAGE_SIZE = 20;
  private static final int PAGES_IN_FULL_SERIES = MATCHING_PUBLICATIONS / PAGE_SIZE;
  private static final int LIMIT_ABOVE_ALL_HITS = 2 * MATCHING_PUBLICATIONS;

  /**
   * The limit lands in the middle of the third of five pages: two pages are changed in full, the
   * third is fetched whole but changed only up to the limit, and the run stops before page four.
   */
  private static final int LIMIT_MID_SERIES = 50;

  private static final int PAGES_UNTIL_LIMIT = 3;
  private static final int HITS_UNTIL_LIMIT = PAGES_UNTIL_LIMIT * PAGE_SIZE;

  /**
   * The limit the handler falls back to when a request names none, mirrored from DEFAULT_LIMIT in
   * ManuallyUpdatePublicationsRequest in nva-publication-api.
   */
  private static final int DEFAULT_LIMIT = 10;

  private static final int LIMIT_BELOW_PAGE_SIZE = 5;
  private static final int LIMIT_AS_SIZE_PARAM = 3;
  private static final String SIZE_PARAM = "size";
  private static final int SINGLE_PAGE = 1;

  private static String titleToken;

  @BeforeAll
  static void createSearchablePublications() {
    titleToken = IndexedPublications.randomTitleToken();
    IndexedPublications.createSearchable(MATCHING_PUBLICATIONS, titleToken);
  }

  /**
   * A dry run should report the change it would make to every matching resource, and leave the
   * resources themselves untouched.
   */
  @Test
  @DisplayName("Dry run reports the planned changes without persisting them")
  @Description(useJavaDoc = true)
  void shouldReportPlannedChangesWithoutPersistingThemWhenDryRunIsRequested(SoftAssertions softly) {
    var report = run(affiliationUpdate().withLimit(LIMIT_ABOVE_ALL_HITS).withPageSize(PAGE_SIZE));

    softly.assertThat(report.dryRun()).isTrue();
    softly.assertThat(report.resourcesChanged()).isEqualTo(MATCHING_PUBLICATIONS);
    softly
        .assertThat(report.changes())
        .allSatisfy(
            change ->
                softly
                    .assertThat(change.fieldChanges())
                    .anySatisfy(
                        fieldChange -> {
                          softly.assertThat(fieldChange.oldValue()).isEqualTo(UIB.getValue());
                          softly.assertThat(fieldChange.newValue()).isEqualTo(SIKT.getValue());
                        }));
    softly
        .assertThat(contributorAffiliationOf(report.changedIdentifiers().getFirst()))
        .isEqualTo(UIB.getValue());
  }

  /**
   * A run whose limit exceeds the number of hits should follow the search cursor through every page
   * and change every matching resource.
   */
  @Test
  @DisplayName("Pagination is followed until every matching resource is changed")
  @Description(useJavaDoc = true)
  void shouldFollowPaginationUntilEveryMatchingResourceIsChanged(SoftAssertions softly) {
    var report = run(affiliationUpdate().withLimit(LIMIT_ABOVE_ALL_HITS).withPageSize(PAGE_SIZE));

    softly.assertThat(report.totalHits()).isEqualTo(MATCHING_PUBLICATIONS);
    softly.assertThat(report.hitsReturned()).isEqualTo(MATCHING_PUBLICATIONS);
    softly.assertThat(report.resourcesFetched()).isEqualTo(MATCHING_PUBLICATIONS);
    softly.assertThat(report.resourcesMatched()).isEqualTo(MATCHING_PUBLICATIONS);
    softly.assertThat(report.resourcesChanged()).isEqualTo(MATCHING_PUBLICATIONS);
    softly.assertThat(report.pageSize()).isEqualTo(PAGE_SIZE);
    softly.assertThat(report.limitReached()).isFalse();
    // Whether the run also fetches a trailing empty page depends on the search api handing out a
    // cursor after a full last page, so only the pages carrying the hits are asserted on.
    softly.assertThat(report.pagesFetched()).isGreaterThanOrEqualTo(PAGES_IN_FULL_SERIES);
  }

  /**
   * A limit that falls inside a page should stop the run on that page: the page is fetched whole,
   * changes stop at the limit, and no further page is requested.
   */
  @Test
  @DisplayName("The limit stops the run midway through the page series")
  @Description(useJavaDoc = true)
  void shouldStopMidwayThroughPageSeriesWhenLimitIsReached(SoftAssertions softly) {
    var report = run(affiliationUpdate().withLimit(LIMIT_MID_SERIES).withPageSize(PAGE_SIZE));

    softly.assertThat(report.limitReached()).isTrue();
    softly.assertThat(report.limit()).isEqualTo(LIMIT_MID_SERIES);
    softly.assertThat(report.pagesFetched()).isEqualTo(PAGES_UNTIL_LIMIT);
    softly.assertThat(report.hitsReturned()).isEqualTo(HITS_UNTIL_LIMIT);
    softly.assertThat(report.resourcesFetched()).isEqualTo(HITS_UNTIL_LIMIT);
    softly.assertThat(report.resourcesMatched()).isEqualTo(LIMIT_MID_SERIES);
    softly.assertThat(report.resourcesChanged()).isEqualTo(LIMIT_MID_SERIES);
    softly.assertThat(report.totalHits()).isEqualTo(MATCHING_PUBLICATIONS);
  }

  /**
   * A request without a limit should fall back to the default limit, so that a run started by
   * mistake cannot sweep the whole archive.
   */
  @Test
  @DisplayName("A request without a limit changes no more than the default")
  @Description(useJavaDoc = true)
  void shouldChangeNoMoreThanTheDefaultLimitWhenNoLimitIsRequested(SoftAssertions softly) {
    var report = run(affiliationUpdate());

    softly.assertThat(report.limit()).isEqualTo(DEFAULT_LIMIT);
    softly.assertThat(report.resourcesChanged()).isEqualTo(DEFAULT_LIMIT);
    softly.assertThat(report.limitReached()).isTrue();
    softly.assertThat(report.pagesFetched()).isEqualTo(SINGLE_PAGE);
    softly.assertThat(report.totalHits()).isEqualTo(MATCHING_PUBLICATIONS);
  }

  /**
   * A page larger than the limit would fetch resources the run can never change, so the page size
   * should be capped to the limit.
   */
  @Test
  @DisplayName("Pages are never larger than the limit")
  @Description(useJavaDoc = true)
  void shouldNotRequestPagesLargerThanTheLimit(SoftAssertions softly) {
    var report = run(affiliationUpdate().withLimit(LIMIT_BELOW_PAGE_SIZE).withPageSize(PAGE_SIZE));

    softly.assertThat(report.pageSize()).isEqualTo(LIMIT_BELOW_PAGE_SIZE);
    softly.assertThat(report.hitsReturned()).isEqualTo(LIMIT_BELOW_PAGE_SIZE);
    softly.assertThat(report.resourcesChanged()).isEqualTo(LIMIT_BELOW_PAGE_SIZE);
    softly.assertThat(report.limitReached()).isTrue();
  }

  /**
   * The search parameter {@code size} should act as an alias for the limit, since that is what the
   * operators running these updates by hand reach for.
   */
  @Test
  @DisplayName("The size search parameter acts as the limit")
  @Description(useJavaDoc = true)
  void shouldTreatSizeSearchParamAsLimit(SoftAssertions softly) {
    var report =
        run(affiliationUpdate().withSearchParams(searchParamsWithSize(LIMIT_AS_SIZE_PARAM)));

    softly.assertThat(report.limit()).isEqualTo(LIMIT_AS_SIZE_PARAM);
    softly.assertThat(report.resourcesChanged()).isEqualTo(LIMIT_AS_SIZE_PARAM);
    softly.assertThat(report.limitReached()).isTrue();
  }

  /**
   * When no resource holds the old value there is nothing to change, so the limit never stops the
   * run and every page is fetched.
   */
  @Test
  @DisplayName("Every page is fetched and nothing is changed when no resource matches")
  @Description(useJavaDoc = true)
  void shouldFetchEveryPageWithoutChangingAnythingWhenNoResourceMatchesOldValue(
      SoftAssertions softly) {
    var report =
        run(
            affiliationUpdate()
                .withOldValue(KRISTIANIA.getValue())
                .withLimit(LIMIT_ABOVE_ALL_HITS)
                .withPageSize(PAGE_SIZE));

    softly.assertThat(report.hitsReturned()).isEqualTo(MATCHING_PUBLICATIONS);
    softly.assertThat(report.resourcesFetched()).isEqualTo(MATCHING_PUBLICATIONS);
    softly.assertThat(report.resourcesMatched()).isZero();
    softly.assertThat(report.resourcesChanged()).isZero();
    softly.assertThat(report.changes()).isEmpty();
    softly.assertThat(report.limitReached()).isFalse();
  }

  private static ManualUpdateRequest affiliationUpdate() {
    return ManualUpdateRequest.dryRunOf(
        CONTRIBUTOR_AFFILIATION,
        UIB.getValue(),
        SIKT.getValue(),
        IndexedPublications.searchParamsFor(titleToken));
  }

  private static Map<String, String> searchParamsWithSize(int size) {
    var searchParams = new HashMap<>(IndexedPublications.searchParamsFor(titleToken));
    searchParams.put(SIZE_PARAM, String.valueOf(size));
    return searchParams;
  }
}
