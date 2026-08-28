package no.sikt.nva.apitest.publication.batch;

import io.qameta.allure.Description;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * CONTRIBUTOR_IDENTIFIER, as documented: replaces one contributor's cristin identifier with
 * another, found through the contributor search parameter.
 */
@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Manual update: CONTRIBUTOR_IDENTIFIER")
class ContributorIdentifierUpdateTest extends ManualUpdateExampleTestBase {

  private static final String TYPE = "CONTRIBUTOR_IDENTIFIER";
  private static final String CONTRIBUTOR_PARAM = "contributor";
  private static final String CRISTIN_PATH = "cristin";
  private static final String PERSON_PATH = "person";

  /** The cristin identifiers of UIB_CREATOR and UIB_PUBLISHING_CURATOR in UserFixtures. */
  private static final String CREATOR_IDENTIFIER = "1862458";

  private static final String REPLACEMENT_IDENTIFIER = "1862459";

  /**
   * Every publication in the set was registered by the same creator, so the run should plan a
   * replacement of that contributor's identifier on all of them.
   */
  @Test
  @DisplayName("Replaces a contributor identifier with another")
  @Description(useJavaDoc = true)
  void shouldReplaceContributorIdentifierWithAnother(SoftAssertions softly) {
    var report =
        runExample(
            ManualUpdateRequest.dryRunOf(
                TYPE,
                CREATOR_IDENTIFIER,
                REPLACEMENT_IDENTIFIER,
                set().searchParamsWith(CONTRIBUTOR_PARAM, personUri(CREATOR_IDENTIFIER))));

    assertMatchedAndChanged(softly, report, set().indexedCount());
    assertFieldChangedFromTo(
        softly, report, personUri(CREATOR_IDENTIFIER), personUri(REPLACEMENT_IDENTIFIER));
  }

  private static String personUri(String identifier) {
    return apiUri(CRISTIN_PATH, PERSON_PATH, identifier);
  }
}
