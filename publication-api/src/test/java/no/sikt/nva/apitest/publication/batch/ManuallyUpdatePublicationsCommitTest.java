package no.sikt.nva.apitest.publication.batch;

import static no.sikt.nva.apitest.base.Affiliation.SIKT;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.publication.batch.IndexedPublications.contributorAffiliationOf;
import static no.sikt.nva.apitest.publication.batch.ManuallyUpdatePublications.CONTRIBUTOR_AFFILIATION;
import static no.sikt.nva.apitest.publication.batch.ManuallyUpdatePublications.run;

import io.qameta.allure.Description;
import java.util.List;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The one case where ManuallyUpdatePublicationsHandler writes: a run with dry run turned off. It
 * owns a small set of its own rather than sharing the set in {@link
 * ManuallyUpdatePublicationsTest}, because changing a resource removes it from what the old value
 * matches and would leave the shared set in a different state for whichever test ran next.
 *
 * <p>The set holds two publications and the run is limited to one, so the test covers both halves
 * of what the limit has to do when a run actually writes: change up to the limit, and leave the
 * rest alone.
 */
@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Manually update publications, persisted (lambda)")
class ManuallyUpdatePublicationsCommitTest extends PublicationTestBase {

  private static final int PUBLICATIONS_IN_SET = 2;
  private static final int LIMIT = 1;

  private static String titleToken;
  private static List<String> identifiers;

  @BeforeAll
  static void createSearchablePublications() {
    titleToken = IndexedPublications.randomTitleToken();
    identifiers = IndexedPublications.createSearchable(PUBLICATIONS_IN_SET, titleToken);
  }

  /**
   * A run with dry run turned off should persist the change to the resources it reports as changed,
   * and leave the resources beyond the limit as they were.
   */
  @Test
  @DisplayName("Turning off dry run persists the changes up to the limit")
  @Description(useJavaDoc = true)
  void shouldPersistChangesUpToTheLimitWhenDryRunIsTurnedOff(SoftAssertions softly) {
    var report =
        run(
            ManualUpdateRequest.dryRunOf(
                    CONTRIBUTOR_AFFILIATION,
                    UIB.getValue(),
                    SIKT.getValue(),
                    IndexedPublications.searchParamsFor(titleToken))
                .withDryRun(false)
                .withLimit(LIMIT));

    softly.assertThat(report.dryRun()).isFalse();
    softly.assertThat(report.resourcesChanged()).isEqualTo(LIMIT);
    softly.assertThat(report.limitReached()).isTrue();

    var changedIdentifier = report.changedIdentifiers().getFirst();
    softly.assertThat(contributorAffiliationOf(changedIdentifier)).isEqualTo(SIKT.getValue());
    softly
        .assertThat(contributorAffiliationOf(untouchedIdentifier(changedIdentifier)))
        .isEqualTo(UIB.getValue());
  }

  private static String untouchedIdentifier(String changedIdentifier) {
    return identifiers.stream()
        .filter(identifier -> !identifier.equals(changedIdentifier))
        .findFirst()
        .orElseThrow();
  }
}
