package no.sikt.nva.apitest.publication.batch;

import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;

import io.qameta.allure.Description;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * UNCONFIRMED_PUBLISHER, as documented: turns a publisher that is only a name into a confirmed
 * channel id, matching the name as a substring.
 *
 * <p>This is the only documented example that uses a comparator, because only the unconfirmed types
 * read one. The publisher name in the shared set embeds the title token, so matching on the token
 * alone is a substring match that still cannot reach beyond this run's publications.
 *
 * <p>The documented example also passes {@code query: UnconfirmedPublisher}, which is dropped here.
 * That parameter is an alias for the free text search over the aggregated q field, which carries
 * titles and names rather than the type of a publication context, so it selects nothing and empties
 * the whole result. In the documented setting it narrows a search that has nothing else to narrow
 * it; here the title token already does that job.
 */
@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Manual update: UNCONFIRMED_PUBLISHER")
class UnconfirmedPublisherUpdateTest extends ManualUpdateTestBase {

  private static final String TYPE = "UNCONFIRMED_PUBLISHER";
  private static final String PUBLISHER_PARAM = "publisher";
  private static final String CONTAINS = "CONTAINS";
  private static final String PUBLICATION_CHANNELS_PATH = "publication-channels-v2";
  private static final String PUBLISHER_PATH = "publisher";
  private static final String CONFIRMED_PUBLISHER = "24621AE7-3128-42B2-99F6-A5E4DBBB3989";

  /**
   * Only one group in the set has a publisher without an id, so the run should match that group and
   * plan a channel uri in place of the name.
   */
  @Test
  @DisplayName("Turns an unconfirmed publisher name into a confirmed channel")
  @Description(useJavaDoc = true)
  void shouldTurnUnconfirmedPublisherNameIntoConfirmedChannel(SoftAssertions softly) {
    var report =
        run(
            ManualUpdateRequest.dryRunOf(
                    TYPE,
                    set().titleToken(),
                    CONFIRMED_PUBLISHER,
                    set().searchParamsWith(PUBLISHER_PARAM, set().unconfirmedPublisherName()))
                .withComparator(CONTAINS));

    assertMatchedAndChanged(softly, report, SharedPublicationSet.MINIMUM_PER_GROUP);
    // The name is replaced by a channel uri rather than edited, so only the arriving value is
    // asserted on: what it replaced is spread across the fields that made up the unconfirmed
    // publisher.
    assertFieldChangedTo(softly, report, confirmedPublisherUri());
  }

  private static String confirmedPublisherUri() {
    return apiUri(PUBLICATION_CHANNELS_PATH, PUBLISHER_PATH, CONFIRMED_PUBLISHER, CURRENT_YEAR);
  }
}
