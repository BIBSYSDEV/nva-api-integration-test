package no.sikt.nva.apitest.publication.batch;

import static no.sikt.nva.apitest.base.Affiliation.SIKT;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.publication.batch.IndexedPublications.contributorAffiliationOf;
import static no.sikt.nva.apitest.publication.batch.ManuallyUpdatePublications.CONTRIBUTOR_AFFILIATION;
import static no.sikt.nva.apitest.publication.batch.ManuallyUpdatePublications.run;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import java.util.List;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
@DisplayName("Manually update publications, persisted (lambda)")
class ManuallyUpdatePublicationsCommitTest extends PublicationTestBase {

  private static final int PUBLICATIONS_IN_SET = 2;
  private static final int LIMIT = 1;

  private static String titleToken;
  private static List<String> identifiers;

  @BeforeAll
  static void createSearchablePublications() {
    titleToken = IndexedPublications.randomTitleToken();
    identifiers = givenSearchablePublicationsAffiliatedWithUib(PUBLICATIONS_IN_SET, titleToken);
  }

  /**
   * A run with dry run turned off should persist the change to the resources it reports as changed,
   * and leave the resources beyond the limit as they were.
   */
  @Test
  @DisplayName("Turning off dry run persists the changes up to the limit")
  @Description(useJavaDoc = true)
  void shouldPersistChangesUpToTheLimitWhenDryRunIsTurnedOff() {
    var report = whenAffiliationIsChangedFromUibToSiktWithDryRunOff(LIMIT, titleToken);

    thenReportShowsPersistedRunThatStoppedAtLimit(report);
    var changedIdentifier = report.changedIdentifiers().getFirst();
    andChangedPublicationHasSiktAffiliation(changedIdentifier);
    andPublicationBeyondLimitStillHasUibAffiliation(untouchedIdentifier(changedIdentifier));
  }

  @Step("Given {count} searchable publications with a contributor affiliated with UiB")
  private static List<String> givenSearchablePublicationsAffiliatedWithUib(
      int count, String titleToken) {
    return IndexedPublications.createSearchable(count, titleToken);
  }

  @Step("When the affiliation is changed from UiB to Sikt with dry run off and a limit of {limit}")
  private static ManualUpdateReport whenAffiliationIsChangedFromUibToSiktWithDryRunOff(
      int limit, String titleToken) {
    return run(
        ManualUpdateRequest.dryRunOf(
                CONTRIBUTOR_AFFILIATION,
                UIB.getValue(),
                SIKT.getValue(),
                IndexedPublications.searchParamsFor(titleToken))
            .withDryRun(false)
            .withLimit(limit));
  }

  @Step("Then the report shows a persisted run that stopped at the limit")
  private static void thenReportShowsPersistedRunThatStoppedAtLimit(ManualUpdateReport report) {
    assertThat(report)
        .extracting(
            ManualUpdateReport::dryRun,
            ManualUpdateReport::resourcesChanged,
            ManualUpdateReport::limitReached)
        .containsExactly(false, LIMIT, true);
  }

  @Step("And the changed publication has the Sikt affiliation")
  private static void andChangedPublicationHasSiktAffiliation(String changedIdentifier) {
    assertThat(contributorAffiliationOf(changedIdentifier)).isEqualTo(SIKT.getValue());
  }

  @Step("And the publication beyond the limit still has the UiB affiliation")
  private static void andPublicationBeyondLimitStillHasUibAffiliation(String untouchedIdentifier) {
    assertThat(contributorAffiliationOf(untouchedIdentifier)).isEqualTo(UIB.getValue());
  }

  private static String untouchedIdentifier(String changedIdentifier) {
    return identifiers.stream()
        .filter(identifier -> !identifier.equals(changedIdentifier))
        .findFirst()
        .orElseThrow();
  }
}
