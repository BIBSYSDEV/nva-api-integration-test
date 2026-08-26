package no.sikt.nva.apitest.publication.batch;

import static no.sikt.nva.apitest.base.Affiliation.SIKT;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.publication.batch.ManuallyUpdatePublications.run;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import java.util.stream.Stream;
import no.sikt.nva.apitest.base.Project;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Runs each of the update types documented on the ManuallyUpdatePublicationHandler page in
 * Confluence, in the shape the page gives them: the same type, the same kind of oldValue and
 * newValue, and the same search parameter used to find the publications.
 *
 * <p>Every example is narrowed by the shared set's title token in addition to its own search
 * parameter. The documented examples search on `publisher`, `topLevelOrganization`, `contributor`
 * and `project` alone, which in a live environment matches far beyond a test's own data. Keeping
 * the documented parameter proves it selects what the example claims; adding the token keeps the
 * counters predictable and the run off everything else.
 *
 * <p>All of these are dry runs, as the documented examples are. Persisting is covered by {@link
 * ManuallyUpdatePublicationsCommitTest}.
 */
@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Manually update publications, documented examples (lambda)")
class ManuallyUpdatePublicationsExamplesTest extends PublicationTestBase {

  private static final String PUBLISHER_PARAM = "publisher";
  private static final String TOP_LEVEL_ORGANIZATION_PARAM = "topLevelOrganization";
  private static final String CONTRIBUTOR_PARAM = "contributor";
  private static final String PROJECT_PARAM = "project";
  private static final String QUERY_PARAM = "query";

  private static final String CONTRIBUTOR_AFFILIATION = "CONTRIBUTOR_AFFILIATION";
  private static final String CONTRIBUTOR_IDENTIFIER = "CONTRIBUTOR_IDENTIFIER";
  private static final String PUBLISHER = "PUBLISHER";
  private static final String UNCONFIRMED_PUBLISHER = "UNCONFIRMED_PUBLISHER";
  private static final String PROJECT = "PROJECT";
  private static final String CONTAINS = "CONTAINS";
  private static final String UNCONFIRMED_PUBLISHER_QUERY = "UnconfirmedPublisher";

  private static final String UIB_TOP_LEVEL_IDENTIFIER = "184.0.0.0";
  private static final String CRISTIN_PERSON_PATH = "/cristin/person/";

  /** The cristin identifiers of UIB_CREATOR and UIB_PUBLISHING_CURATOR in UserFixtures. */
  private static final String CREATOR_CRISTIN_IDENTIFIER = "1862458";

  private static final String REPLACEMENT_CRISTIN_IDENTIFIER = "1862459";

  /** From the monograph template, which carries a confirmed publisher. */
  private static final String TEMPLATE_PUBLISHER_IDENTIFIER =
      "DC752087-7122-4D3A-9E4F-382AA2F39D2C";

  private static final String REPLACEMENT_PUBLISHER_IDENTIFIER =
      "24621AE7-3128-42B2-99F6-A5E4DBBB3989";

  /** The size of a group in the shared set, which is what a group-specific example can match. */
  private static final int PUBLICATIONS_PER_GROUP = SharedPublicationSet.TOTAL_PUBLICATIONS / 3;

  private static final int LIMIT_ABOVE_ALL_HITS = 2 * SharedPublicationSet.TOTAL_PUBLICATIONS;

  /**
   * Each documented example should match the publications it claims to and report a change for
   * every one of them, without writing anything.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("documentedExamples")
  @DisplayName("A documented example matches the publications it targets")
  @Description(useJavaDoc = true)
  void shouldMatchTheTargetedPublications(
      ManualUpdateRequest request, int expectedMatches, SoftAssertions softly) {
    var report = run(request.withLimit(LIMIT_ABOVE_ALL_HITS));

    softly.assertThat(report.dryRun()).isTrue();
    softly.assertThat(report.resourcesMatched()).isEqualTo(expectedMatches);
    softly.assertThat(report.resourcesChanged()).isEqualTo(expectedMatches);
    softly.assertThat(report.limitReached()).isFalse();
  }

  private static Stream<Arguments> documentedExamples() {
    return Stream.of(
        arguments(
            named("CONTRIBUTOR_AFFILIATION", contributorAffiliationExample()),
            SharedPublicationSet.TOTAL_PUBLICATIONS),
        arguments(
            named("CONTRIBUTOR_IDENTIFIER", contributorIdentifierExample()),
            SharedPublicationSet.TOTAL_PUBLICATIONS),
        arguments(named("PROJECT", projectExample()), SharedPublicationSet.TOTAL_PUBLICATIONS),
        arguments(named("PUBLISHER", publisherExample()), PUBLICATIONS_PER_GROUP),
        arguments(
            named("UNCONFIRMED_PUBLISHER", unconfirmedPublisherExample()), PUBLICATIONS_PER_GROUP));
  }

  /** Moves contributor affiliations from one organization to another. */
  private static ManualUpdateRequest contributorAffiliationExample() {
    return ManualUpdateRequest.dryRunOf(
        CONTRIBUTOR_AFFILIATION,
        UIB.getValue(),
        SIKT.getValue(),
        set().searchParamsWith(TOP_LEVEL_ORGANIZATION_PARAM, UIB_TOP_LEVEL_IDENTIFIER));
  }

  /** Replaces one contributor's cristin identifier with another. */
  private static ManualUpdateRequest contributorIdentifierExample() {
    return ManualUpdateRequest.dryRunOf(
        CONTRIBUTOR_IDENTIFIER,
        CREATOR_CRISTIN_IDENTIFIER,
        REPLACEMENT_CRISTIN_IDENTIFIER,
        set().searchParamsWith(CONTRIBUTOR_PARAM, contributorUri()));
  }

  /** Replaces one project with another. */
  private static ManualUpdateRequest projectExample() {
    return ManualUpdateRequest.dryRunOf(
        PROJECT,
        Project.CURRENT.getIdentifier(),
        Project.REPLACEMENT.getIdentifier(),
        set().searchParamsWith(PROJECT_PARAM, Project.CURRENT.getIdentifier()));
  }

  /** Replaces a confirmed publisher id with another, which only the monographs carry. */
  private static ManualUpdateRequest publisherExample() {
    return ManualUpdateRequest.dryRunOf(
        PUBLISHER,
        TEMPLATE_PUBLISHER_IDENTIFIER,
        REPLACEMENT_PUBLISHER_IDENTIFIER,
        set().searchParamsWith(PUBLISHER_PARAM, TEMPLATE_PUBLISHER_IDENTIFIER));
  }

  /**
   * Converts an unconfirmed publisher name into a confirmed id, matching the name as a substring
   * the way the documented example does.
   */
  private static ManualUpdateRequest unconfirmedPublisherExample() {
    var searchParams = set().searchParamsWith(PUBLISHER_PARAM, set().unconfirmedPublisherName());
    searchParams.put(QUERY_PARAM, UNCONFIRMED_PUBLISHER_QUERY);

    return ManualUpdateRequest.dryRunOf(
            UNCONFIRMED_PUBLISHER,
            set().titleToken(),
            REPLACEMENT_PUBLISHER_IDENTIFIER,
            searchParams)
        .withComparator(CONTAINS);
  }

  private static String contributorUri() {
    return RestAssured.baseURI + CRISTIN_PERSON_PATH + CREATOR_CRISTIN_IDENTIFIER;
  }

  private static SharedPublicationSet set() {
    return SharedPublicationSet.get();
  }
}
