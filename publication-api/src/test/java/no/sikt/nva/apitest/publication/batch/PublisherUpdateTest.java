package no.sikt.nva.apitest.publication.batch;

import static no.sikt.nva.apitest.base.CurrentTimeConstants.CURRENT_YEAR;

import io.qameta.allure.Description;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * PUBLISHER, as documented: replaces a confirmed publisher id with another, found through the
 * publisher search parameter.
 */
@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Manual update: PUBLISHER")
class PublisherUpdateTest extends ManualUpdateExampleTestBase {

  private static final String TYPE = "PUBLISHER";
  private static final String PUBLISHER_PARAM = "publisher";
  private static final String PUBLICATION_CHANNELS_PATH = "publication-channels-v2";
  private static final String PUBLISHER_PATH = "publisher";

  /** The confirmed publisher in the monograph template. */
  private static final String TEMPLATE_PUBLISHER = "DC752087-7122-4D3A-9E4F-382AA2F39D2C";

  private static final String REPLACEMENT_PUBLISHER = "24621AE7-3128-42B2-99F6-A5E4DBBB3989";

  /**
   * Only the monographs with a confirmed publisher carry one at all, so the run should match that
   * group and leave the journal articles alone.
   */
  @Test
  @DisplayName("Replaces a confirmed publisher id on the publications that have one")
  @Description(useJavaDoc = true)
  void shouldReplacePublisherIdOnPublicationsThatHaveOne(SoftAssertions softly) {
    var report =
        runExample(
            ManualUpdateRequest.dryRunOf(
                TYPE,
                TEMPLATE_PUBLISHER,
                REPLACEMENT_PUBLISHER,
                set().searchParamsWith(PUBLISHER_PARAM, TEMPLATE_PUBLISHER)));

    assertMatchedAndChanged(softly, report, SharedPublicationSet.PUBLICATIONS_PER_GROUP);
    // The handler swaps the identifier inside the existing channel uri rather than rebuilding it,
    // so the year the template pins the channel to survives the update.
    assertFieldChangedFromTo(
        softly, report, publisherUri(TEMPLATE_PUBLISHER), publisherUri(REPLACEMENT_PUBLISHER));
  }

  private static String publisherUri(String identifier) {
    return apiUri(PUBLICATION_CHANNELS_PATH, PUBLISHER_PATH, identifier, CURRENT_YEAR);
  }
}
