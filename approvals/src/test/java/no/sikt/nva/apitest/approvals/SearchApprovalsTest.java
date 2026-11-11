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
import static no.sikt.nva.apitest.approvals.ClinicalTrialSource.INVESTIGATOR_PERSON_ID;
import static no.sikt.nva.apitest.approvals.ClinicalTrialSource.INVESTIGATOR_PERSON_URI;
import static no.sikt.nva.apitest.approvals.ClinicalTrialSource.publishClinicalTrial;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.base.SettledCondition.settledWhenAtLeast;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import java.net.URI;
import java.time.Duration;
import java.util.stream.Stream;
import no.sikt.nva.apitest.base.IntegrationTestBase;
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
  private static final String HANDLE_FIELD = "handle";
  private static final String HITS_FIELD = "hits";
  private static final String FIRST_HIT_ID_FIELD = "hits[0].id";
  private static final Duration APPROVAL_AVAILABLE_TIMEOUT = Duration.ofSeconds(30);
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
                    UIB_IDENTIFIER_NAME, namedIdentifierValue, publishClinicalTrial())));
    var approvalIdentifier = identifierOf(approvalId.toString());

    var approval =
        pollUntil(
                APPROVAL_AVAILABLE_TIMEOUT,
                () -> fetchApproval(approvalIdentifier, JSON_MEDIA_TYPE),
                response -> response.statusCode() == HTTP_OK)
            .jsonPath();
    handle = URI.create(approval.getString(HANDLE_FIELD));
  }

  /** A query matching nothing is an empty result. */
  @Test
  @DisplayName("Search by a query that matches no approval")
  @Description
  void shouldReturnOkWithEmptyHitsWhenQueryMatchesNoApproval() {
    assertThat(searchApprovals(uniqueValue()).getList(HITS_FIELD)).isEmpty();
  }

  /** Search approval by handle, identifier and person returns approval in hits */
  @ParameterizedTest
  @MethodSource("queriesNamingTheApproval")
  @DisplayName("Search approval by query")
  @Description
  void shouldReturnApprovalWhenQueryNamesIt(String query) {
    assertThat(awaitSearchable(query).getString(FIRST_HIT_ID_FIELD))
        .isEqualTo(approvalId.toString());
  }

  private static Stream<Arguments> queriesNamingTheApproval() {
    return Stream.of(
        argumentSet("Approval uri", approvalId.toString()),
        argumentSet("Approval identifier", identifierOf(approvalId)),
        argumentSet("Handle", handle.toString()),
        argumentSet("Named identifier", namedIdentifierValue),
        argumentSet("Person uri", narrowedToThisApproval(INVESTIGATOR_PERSON_URI.toString())),
        argumentSet("Person identifier", narrowedToThisApproval(INVESTIGATOR_PERSON_ID)));
  }

  /**
   * Pairs a term with the identifier value only this run's approval carries. The trial document is
   * the same one every run publishes, so its persons name every approval this test has ever
   * created, while the four terms above name one approval on their own. Query terms are combined
   * with AND, so a person term matching nothing would leave the query without hits.
   */
  private static String narrowedToThisApproval(String term) {
    return "%s:%s".formatted(term, namedIdentifierValue);
  }

  /**
   * Indexing an approval takes fetching and expanding its source first, so a search for one just
   * created answers truthfully with nothing until that has happened.
   */
  private static JsonPath awaitSearchable(String query) {
    return pollUntil(
        INDEXING_TIMEOUT,
        () -> searchApprovals(query),
        settledWhenAtLeast(HITS_FIELD, 1, jsonPath -> jsonPath.getList(HITS_FIELD).size()));
  }

  private static JsonPath searchApprovals(String query) {
    return givenUnauthenticatedJsonRequest()
        .queryParam(QUERY_PARAMETER, query)
        .get(BASE_PATH)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath();
  }
}
