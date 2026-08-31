package no.sikt.nva.apitest.publication.batch;

import static no.sikt.nva.apitest.base.Affiliation.SIKT;
import static no.sikt.nva.apitest.base.Affiliation.UIB;

import io.qameta.allure.Description;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * CONTRIBUTOR_AFFILIATION, as documented: moves contributors from one organization to another,
 * found through the topLevelOrganization search parameter.
 */
@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Manual update: CONTRIBUTOR_AFFILIATION")
class ContributorAffiliationUpdateTest extends ManualUpdateTestBase {

  private static final String TYPE = "CONTRIBUTOR_AFFILIATION";
  private static final String TOP_LEVEL_ORGANIZATION_PARAM = "topLevelOrganization";
  private static final String UIB_TOP_LEVEL_IDENTIFIER = "184.0.0.0";

  /**
   * Every publication in the set has a contributor affiliated with UiB, so the run should plan a
   * move to the new organization on all of them.
   */
  @Test
  @DisplayName("Moves contributor affiliations to another organization")
  @Description(useJavaDoc = true)
  void shouldMoveContributorAffiliationsToAnotherOrganization(SoftAssertions softly) {
    var report =
        run(
            ManualUpdateRequest.dryRunOf(
                TYPE,
                UIB.getValue(),
                SIKT.getValue(),
                set().searchParamsWith(TOP_LEVEL_ORGANIZATION_PARAM, UIB_TOP_LEVEL_IDENTIFIER)));

    assertMatchedAndChanged(softly, report, set().indexedCount());
    assertFieldChangedFromTo(softly, report, UIB.getValue(), SIKT.getValue());
  }
}
