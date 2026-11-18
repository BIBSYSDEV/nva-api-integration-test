package no.sikt.nva.apitest.approvals;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_CLIENT_SECRET;
import static no.sikt.nva.apitest.approvals.ApprovalClients.UIB_IDENTIFIER_NAME;
import static no.sikt.nva.apitest.approvals.ApprovalPaths.BASE_PATH;
import static no.sikt.nva.apitest.approvals.Approvals.approvalPayload;
import static no.sikt.nva.apitest.approvals.Approvals.createApproval;
import static no.sikt.nva.apitest.approvals.Approvals.fetchApproval;
import static no.sikt.nva.apitest.approvals.Approvals.identifierOf;
import static no.sikt.nva.apitest.approvals.Approvals.uniqueValue;
import static no.sikt.nva.apitest.approvals.ClinicalTrialSource.INVESTIGATOR_PERSON_IDENTIFIER;
import static no.sikt.nva.apitest.approvals.ClinicalTrialSource.INVESTIGATOR_PERSON_URI;
import static no.sikt.nva.apitest.approvals.ClinicalTrialSource.TRIAL_SITE_ORGANIZATION_IDENTIFIER;
import static no.sikt.nva.apitest.approvals.ClinicalTrialSource.TRIAL_SITE_ORGANIZATION_URI;
import static no.sikt.nva.apitest.approvals.ClinicalTrialSource.publishClinicalTrial;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.SettledCondition.settledWhen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("GET " + BASE_PATH + " by query")
class SearchApprovalsTest extends IntegrationTestBase {

  private static final String JSON_MEDIA_TYPE = "application/json";
  private static final String QUERY_PARAMETER = "query";
  private static final String HANDLE_PARAMETER = "handle";
  private static final String IDENTIFIER_PARAMETER = "identifier";
  private static final String PERSON_PARAMETER = "person";
  private static final String ORGANIZATION_PARAMETER = "organization";
  private static final String NAMESPACED_IDENTIFIER_FORMAT = "%s:%s";
  private static final String HANDLE_FIELD = "handle";
  private static final String HITS_FIELD = "hits";
  private static final String HIT_IDS_FIELD = "hits.id";
  private static final Duration INDEXING_TIMEOUT = Duration.ofMinutes(3);

  private static URI approvalId;
  private static URI handle;
  private static String namedIdentifierValue;

  @BeforeAll
  static void createApprovalToQueryFor() {
    namedIdentifierValue = uniqueValue();
    approvalId =
        URI.create(
            createApproval(
                UIB_CLIENT_SECRET,
                approvalPayload(
                    UIB_IDENTIFIER_NAME, namedIdentifierValue, publishClinicalTrial().toString())));
    handle =
        URI.create(
            fetchApproval(identifierOf(approvalId), JSON_MEDIA_TYPE)
                .jsonPath()
                .getString(HANDLE_FIELD));
  }

  /** A query matching nothing is an empty result. */
  @Test
  @DisplayName("Search by a query that matches no approval")
  @Description
  void shouldReturnOkWithEmptyHitsWhenQueryMatchesNoApproval() {
    assertThat(searchApprovals(QUERY_PARAMETER, uniqueValue()).getList(HITS_FIELD)).isEmpty();
  }

  /**
   * Search approval by handle, identifiers and person returns approval in hits. A person query also
   * matches the approvals left by earlier runs, so the hit this test is about is looked for among
   * the hits rather than expected to be the first one.
   */
  @ParameterizedTest
  @MethodSource("queries")
  @DisplayName("Search approval by query")
  @Description
  void shouldReturnApprovalWhenQueryNamesIt(String query) {
    assertThat(hitIds(awaitSearchable(QUERY_PARAMETER, query))).contains(approvalId.toString());
  }

  /** Each filter matches on an exact value rather than on free text. */
  @ParameterizedTest
  @MethodSource("filters")
  @DisplayName("Search approval by filter")
  @Description
  void shouldReturnApprovalWhenFilterNamesIt(String parameterName, String value) {
    assertThat(hitIds(awaitSearchable(parameterName, value))).contains(approvalId.toString());
  }

  private static Stream<Arguments> queries() {
    return Stream.of(
        argumentSet("Approval uri", approvalId.toString()),
        argumentSet("Approval identifier", identifierOf(approvalId)),
        argumentSet("Handle", handle.toString()),
        argumentSet("Named identifier", namedIdentifierValue),
        argumentSet("Person uri", INVESTIGATOR_PERSON_URI.toString()),
        argumentSet("Person identifier", INVESTIGATOR_PERSON_IDENTIFIER));
  }

  private static Stream<Arguments> filters() {
    return Stream.of(
        argumentSet("Handle uri", HANDLE_PARAMETER, handle.toString()),
        argumentSet("Handle identifier", HANDLE_PARAMETER, handleIdentifier()),
        argumentSet("Named identifier", IDENTIFIER_PARAMETER, namespacedIdentifier()),
        argumentSet("Person uri", PERSON_PARAMETER, INVESTIGATOR_PERSON_URI.toString()),
        argumentSet("Person identifier", PERSON_PARAMETER, INVESTIGATOR_PERSON_IDENTIFIER),
        argumentSet(
            "Organization uri", ORGANIZATION_PARAMETER, TRIAL_SITE_ORGANIZATION_URI.toString()),
        argumentSet(
            "Organization identifier", ORGANIZATION_PARAMETER, TRIAL_SITE_ORGANIZATION_IDENTIFIER));
  }

  private static String handleIdentifier() {
    return UriWrapper.fromUri(handle).getPath().removeRoot().toString();
  }

  private static String namespacedIdentifier() {
    return NAMESPACED_IDENTIFIER_FORMAT.formatted(UIB_IDENTIFIER_NAME, namedIdentifierValue);
  }

  private static JsonPath awaitSearchable(String parameterName, String value) {
    return pollUntil(
        INDEXING_TIMEOUT,
        () -> searchApprovals(parameterName, value),
        settledWhen(
            jsonPath -> hitIds(jsonPath).contains(approvalId.toString()),
            jsonPath ->
                "%s was %s, expected to contain %s"
                    .formatted(HIT_IDS_FIELD, hitIds(jsonPath), approvalId)));
  }

  private static List<String> hitIds(JsonPath jsonPath) {
    return jsonPath.getList(HIT_IDS_FIELD, String.class);
  }

  private static JsonPath searchApprovals(String parameterName, String value) {
    return givenUnauthenticatedJsonRequest()
        .queryParam(parameterName, value)
        .get(BASE_PATH)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath();
  }
}
