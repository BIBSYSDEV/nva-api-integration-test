package no.sikt.nva.apitest.publication.batch;

import io.restassured.RestAssured;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import nva.commons.core.paths.UriWrapper;
import org.assertj.core.api.SoftAssertions;

/**
 * Shared setup for the tests that run one documented update type each, against the publications in
 * {@link SharedPublicationSet}.
 *
 * <p>The types are split into a test class per type rather than parameterized over one, because
 * what each type does to a resource differs: the interesting assertion is on the field change it
 * plans, and that has a different shape for every type. What they have in common is only how the
 * run is set up and how the counters are read, which is what lives here.
 *
 * <p>Every subclass runs as a dry run. The set is shared and read by several classes at once, so a
 * test that persists would pull the ground out from under the others.
 */
abstract class ManualUpdateExampleTestBase extends PublicationTestBase {

  /** No example is about the limit, so it is set clear of every hit to leave the run unbounded. */
  private static final int LIMIT_ABOVE_ALL_HITS = 2 * SharedPublicationSet.TOTAL_PUBLICATIONS;

  protected static SharedPublicationSet set() {
    return SharedPublicationSet.get();
  }

  protected static ManualUpdateReport runExample(ManualUpdateRequest request) {
    return ManuallyUpdatePublications.run(request.withLimit(LIMIT_ABOVE_ALL_HITS));
  }

  /**
   * Asserts the counters every example shares: it is a dry run, the search found the publications
   * the example targets, the update matched all of them, it planned a change for each, and the
   * limit never cut it short.
   *
   * <p>totalHits is asserted first and separately from resourcesMatched, because the two fail for
   * opposite reasons. A search parameter that selects nothing leaves both at zero and says nothing
   * about which; separating them puts the search on one line and the matching on the next.
   */
  protected static void assertMatchedAndChanged(
      SoftAssertions softly, ManualUpdateReport report, int expectedPublications) {
    softly.assertThat(report.dryRun()).isTrue();
    softly
        .assertThat(report.totalHits())
        .as("publications found by the search parameters")
        .isEqualTo(expectedPublications);
    softly
        .assertThat(report.resourcesMatched())
        .as("publications the update applied to")
        .isEqualTo(expectedPublications);
    softly.assertThat(report.resourcesChanged()).isEqualTo(expectedPublications);
    softly.assertThat(report.limitReached()).isFalse();
  }

  /**
   * Asserts that every changed resource has a field going from one value to the other. The path is
   * deliberately not asserted on: it comes from how ResourceDiff walks the resource, which is an
   * implementation detail of the handler rather than something the examples promise.
   */
  protected static void assertFieldChangedFromTo(
      SoftAssertions softly, ManualUpdateReport report, String oldValue, String newValue) {
    softly
        .assertThat(report.changes())
        .isNotEmpty()
        .allSatisfy(
            change ->
                softly
                    .assertThat(change.fieldChanges())
                    .anySatisfy(
                        fieldChange -> {
                          softly.assertThat(fieldChange.oldValue()).isEqualTo(oldValue);
                          softly.assertThat(fieldChange.newValue()).isEqualTo(newValue);
                        }));
  }

  /** Asserts that every changed resource has a field arriving at the given value. */
  protected static void assertFieldChangedTo(
      SoftAssertions softly, ManualUpdateReport report, String newValue) {
    softly
        .assertThat(report.changes())
        .isNotEmpty()
        .allSatisfy(
            change ->
                softly
                    .assertThat(change.fieldChanges())
                    .anySatisfy(
                        fieldChange ->
                            softly.assertThat(fieldChange.newValue()).isEqualTo(newValue)));
  }

  /** An absolute api uri, which is the form the handler writes into a resource. */
  protected static String apiUri(String... pathElements) {
    return UriWrapper.fromUri(RestAssured.baseURI).addChild(pathElements).getUri().toString();
  }
}
