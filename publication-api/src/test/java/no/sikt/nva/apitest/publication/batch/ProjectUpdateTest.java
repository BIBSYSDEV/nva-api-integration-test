package no.sikt.nva.apitest.publication.batch;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.base.Project;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * PROJECT, as documented: replaces one project with another, found through the project search
 * parameter.
 */
@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Manual update: PROJECT")
class ProjectUpdateTest extends ManualUpdateExampleTestBase {

  private static final String TYPE = "PROJECT";
  private static final String PROJECT_PARAM = "project";

  /**
   * Every publication in the set carries the same project, so the run should plan a replacement on
   * all of them.
   */
  @Test
  @DisplayName("Replaces a project with another")
  @Description(useJavaDoc = true)
  void shouldReplaceProjectWithAnother(SoftAssertions softly) {
    var report =
        runExample(
            ManualUpdateRequest.dryRunOf(
                TYPE,
                Project.CURRENT.getIdentifier(),
                Project.REPLACEMENT.getIdentifier(),
                set().searchParamsWith(PROJECT_PARAM, Project.CURRENT.getIdentifier())));

    assertMatchedAndChanged(softly, report, SharedPublicationSet.TOTAL_PUBLICATIONS);
    assertFieldChangedFromTo(
        softly, report, Project.CURRENT.getValue(), Project.REPLACEMENT.getValue());
  }
}
