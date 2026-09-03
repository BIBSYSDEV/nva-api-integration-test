package no.sikt.nva.apitest.publication.batch;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Checks the ground the other batch tests stand on, so that a problem with the test data is
 * reported as itself rather than as a puzzling counter in an unrelated test.
 *
 * <p>The other tests deliberately tolerate a publication going missing: they anchor their counters
 * to what the search api actually returns, so one lost publication does not fail a test about
 * pagination. That tolerance would otherwise hide a real indexing failure, which is what this test
 * is here to surface.
 */
@DisplayName("Batch update test data")
class SharedPublicationSetTest extends PublicationTestBase {

  /**
   * Every publication the tests create is published, so every one of them should end up in the
   * search index. One that does not is either a publish that failed downstream of its 202, or an
   * indexing failure worth chasing on the dead letter queue.
   */
  @Test
  @DisplayName("Every created publication reached the search index")
  @Description(useJavaDoc = true)
  void shouldIndexEveryCreatedPublication() {
    var set = SharedPublicationSet.get();

    assertThat(set.missingFromIndex())
        .as(
            "publications created for title token %s that never became searchable",
            set.titleToken())
        .isEmpty();
  }
}
