package no.sikt.nva.apitest.publication.batch;

import static no.sikt.nva.apitest.base.Affiliation.SIKT;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.publication.batch.ManuallyUpdatePublications.CONTRIBUTOR_AFFILIATION;
import static no.sikt.nva.apitest.publication.batch.ManuallyUpdatePublications.invoke;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.qameta.allure.Description;
import java.util.Map;
import java.util.stream.Stream;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The guards ManuallyUpdatePublicationsRequest in nva-publication-api puts on a run. These are what
 * stands between an operator invoking the handler by hand and a run that rewrites more of the
 * archive than intended, so the handler is expected to reject the request outright rather than fall
 * back to a default.
 *
 * <p>No publications are set up: a rejected request never reaches the search api, so these tests
 * need no data and cannot touch any.
 */
@DisplayName("Manually update publications, rejected requests (lambda)")
class ManuallyUpdatePublicationsValidationTest extends PublicationTestBase {

  private static final String MISSING_DRY_RUN_MESSAGE = "Field 'dryRun' is required";
  private static final String MISSING_VALUES_MESSAGE =
      "Fields 'oldValue' and 'newValue' are both required";
  private static final String MISSING_SEARCH_PARAMS_MESSAGE =
      "Field 'searchParams' must hold at least one search parameter";
  private static final String INVALID_LIMIT_MESSAGE = "Field 'limit' must be a positive number";
  private static final String INVALID_PAGE_SIZE_MESSAGE =
      "Field 'pageSize' must be between 1 and 1000 hits";

  private static final String BLANK_VALUE = " ";
  private static final String NOT_A_NUMBER = "many";
  private static final String SIZE_PARAM = "size";
  private static final String TITLE_PARAM = "title";
  private static final String ANY_TITLE = "irrelevant, the request never reaches search";
  private static final int LIMIT_BELOW_MINIMUM = 0;
  private static final int PAGE_SIZE_BELOW_MINIMUM = 0;
  private static final int PAGE_SIZE_ABOVE_MAXIMUM = 1001;

  /** An invalid request should be rejected with an error naming the field that is wrong. */
  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidRequests")
  @DisplayName("An invalid request is rejected")
  @Description(useJavaDoc = true)
  void shouldRejectInvalidRequest(ManualUpdateRequest request, String expectedMessage) {
    var invocation = invoke(request);

    assertThat(invocation.failed()).isTrue();
    assertThat(invocation.errorMessage()).contains(expectedMessage);
  }

  private static Stream<Arguments> invalidRequests() {
    return Stream.of(
        arguments(
            named("dryRun is missing", validRequest().withDryRun(null)), MISSING_DRY_RUN_MESSAGE),
        arguments(
            named("oldValue is blank", validRequest().withOldValue(BLANK_VALUE)),
            MISSING_VALUES_MESSAGE),
        arguments(
            named("newValue is blank", validRequest().withNewValue(BLANK_VALUE)),
            MISSING_VALUES_MESSAGE),
        arguments(
            named("searchParams is missing", validRequest().withSearchParams(null)),
            MISSING_SEARCH_PARAMS_MESSAGE),
        arguments(
            named("searchParams is empty", validRequest().withSearchParams(Map.of())),
            MISSING_SEARCH_PARAMS_MESSAGE),
        arguments(
            named("limit is below the minimum", validRequest().withLimit(LIMIT_BELOW_MINIMUM)),
            INVALID_LIMIT_MESSAGE),
        arguments(
            named("the size search param is not a number", requestWithSize(NOT_A_NUMBER)),
            INVALID_LIMIT_MESSAGE),
        arguments(
            named(
                "pageSize is below the minimum",
                validRequest().withPageSize(PAGE_SIZE_BELOW_MINIMUM)),
            INVALID_PAGE_SIZE_MESSAGE),
        arguments(
            named(
                "pageSize is above the maximum",
                validRequest().withPageSize(PAGE_SIZE_ABOVE_MAXIMUM)),
            INVALID_PAGE_SIZE_MESSAGE));
  }

  private static ManualUpdateRequest validRequest() {
    return ManualUpdateRequest.dryRunOf(
        CONTRIBUTOR_AFFILIATION, UIB.getValue(), SIKT.getValue(), Map.of(TITLE_PARAM, ANY_TITLE));
  }

  private static ManualUpdateRequest requestWithSize(String size) {
    return validRequest().withSearchParams(Map.of(TITLE_PARAM, ANY_TITLE, SIZE_PARAM, size));
  }
}
