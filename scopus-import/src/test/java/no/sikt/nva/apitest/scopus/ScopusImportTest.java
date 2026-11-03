package no.sikt.nva.apitest.scopus;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MINUTES;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.Requests.givenUnauthenticatedJsonRequest;
import static no.sikt.nva.apitest.scopus.FakeScopusIds.randomAfid;
import static no.sikt.nva.apitest.scopus.FakeScopusIds.randomAuid;
import static no.sikt.nva.apitest.scopus.ImportCandidates.affiliationIdsOfAuthor;
import static no.sikt.nva.apitest.scopus.ImportCandidates.awaitCandidate;
import static no.sikt.nva.apitest.scopus.ImportCandidates.identityIdOfAuthor;
import static no.sikt.nva.apitest.scopus.ImportCandidates.importWithFixturePersonAsOnlyAuthor;
import static no.sikt.nva.apitest.scopus.ImportCandidates.nameOfAuthor;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.FIXTURE_AFID;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.FIXTURE_AUID;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.fixturePersonId;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.fixtureUnitId;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.path.json.JsonPath;
import java.time.Duration;
import java.util.Optional;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Feeds generated Scopus records into the deployed import and checks the candidates it creates. */
@DisplayName("Scopus import (S3 and SQS)")
class ScopusImportTest extends IntegrationTestBase {

  private static final String CUSTOMERS_PATH = "/customer";
  private static final Duration ERROR_REPORT_TIMEOUT = Duration.ofMinutes(5);
  private static final String NO_CUSTOMER_EXCEPTION = "MissingNvaContributorException";
  private static final String AUTHOR_SURNAME = "Author";

  /**
   * The unit is checked on an unmapped co-author, since a mapped author gets the affiliations of
   * their Cristin profile instead.
   */
  @Test
  @DisplayName("Mapped author and affiliation resolve to Cristin person, unit and customer")
  @Description
  @Timeout(value = 15, unit = MINUTES)
  void shouldResolveMappedAuthorAndAffiliationWhenRecordIsImported() {
    givenFixtureAuthorIsMappedToFixturePerson();
    var fixtureAuthor = new ScopusAuthor(FIXTURE_AUID, "Mapped", AUTHOR_SURNAME);
    var coAuthor = new ScopusAuthor(randomAuid(), "Unmapped", "Coauthor");

    var candidate =
        whenRecordIsImported(ScopusRecord.withAuthors(FIXTURE_AFID, fixtureAuthor, coAuthor));

    thenAuthorResolvesToFixturePerson(candidate, fixtureAuthor);
    andUnmappedCoAuthorIsAffiliatedWithFixtureUnit(candidate, coAuthor);
    andCandidateBelongsToFixtureCustomer(candidate);
  }

  @Test
  @DisplayName("Unknown author has no identity")
  void shouldNotIdentifyAuthorWhenAuidIsNotMapped() {
    var unknownAuthor = new ScopusAuthor(randomAuid(), "Unknown", AUTHOR_SURNAME);

    var candidate = whenRecordIsImported(ScopusRecord.withAuthors(FIXTURE_AFID, unknownAuthor));

    thenAuthorIsKeptWithoutIdentity(candidate, unknownAuthor);
  }

  /** Uses a new fake AUID every run, so the fixture mapping is never touched. */
  @Test
  @DisplayName("Author correction made during import is used by the next record")
  @Description
  @Timeout(value = 15, unit = MINUTES)
  void shouldResolveAuthorToCorrectedPersonWhenSameAuidIsImportedAgain() {
    var correctedAuthor = new ScopusAuthor(randomAuid(), "Corrected", AUTHOR_SURNAME);
    var firstCandidate = givenCandidateWithUnidentifiedAuthor(correctedAuthor);

    whenCuratorImportsItWithAuthorSetToFixturePerson(firstCandidate, correctedAuthor);
    var secondCandidate = andNewRecordWithSameAuthorIsImported(correctedAuthor);

    thenAuthorResolvesToFixturePerson(secondCandidate, correctedAuthor);
  }

  @Test
  @DisplayName("Record delivered in a zip becomes an import candidate")
  void shouldCreateCandidateWhenRecordIsUploadedAsZip() {
    var scopusRecord =
        ScopusRecord.withAuthors(
            FIXTURE_AFID, new ScopusAuthor(randomAuid(), "Zipped", AUTHOR_SURNAME));

    var candidate = whenRecordIsUploadedAsZip(scopusRecord);

    thenCandidateHasTitleOfRecord(candidate, scopusRecord);
  }

  /** The affiliation has an unmapped AFID and a name that matches no Cristin organization. */
  @Test
  @DisplayName("Record with no customer affiliation gives an error report and no candidate")
  @Description
  void shouldNotCreateCandidateWhenNoAffiliationBelongsToCustomer() {
    var scopusRecord =
        ScopusRecord.withAuthors(
            randomAfid(), new ScopusAuthor(randomAuid(), "Customerless", AUTHOR_SURNAME));

    whenRecordIsSubmitted(scopusRecord);

    thenErrorReportSaysNoCustomerWasFound(scopusRecord);
    andNoCandidateIsSearchable(scopusRecord);
  }

  @Step("Given the fixture AUID is mapped to the fixture person")
  private static void givenFixtureAuthorIsMappedToFixturePerson() {
    FixtureAuthorMapping.createIfMissing();
  }

  @Step("Given a candidate whose author has a new, unmapped AUID")
  private static JsonPath givenCandidateWithUnidentifiedAuthor(ScopusAuthor author) {
    var candidate = importRecord(ScopusRecord.withAuthors(FIXTURE_AFID, author));
    assertThat(identityIdOfAuthor(candidate, author.auid())).isNull();
    return candidate;
  }

  @Step("When a record is uploaded to the XML bucket, queued and imported as a candidate")
  private static JsonPath whenRecordIsImported(ScopusRecord scopusRecord) {
    return importRecord(scopusRecord);
  }

  @Step("When a record is uploaded to the XML bucket and queued for import")
  private static void whenRecordIsSubmitted(ScopusRecord scopusRecord) {
    ScopusImportPipeline.submitRecord(scopusRecord);
  }

  @Step("When a record is uploaded to the zip bucket")
  private static JsonPath whenRecordIsUploadedAsZip(ScopusRecord scopusRecord) {
    ScopusImportPipeline.submitZip(scopusRecord);
    return awaitCandidate(scopusRecord.eid());
  }

  @Step("When the curator imports it with the author set to the fixture person")
  private static void whenCuratorImportsItWithAuthorSetToFixturePerson(
      JsonPath candidate, ScopusAuthor author) {
    importWithFixturePersonAsOnlyAuthor(candidate, author.auid());
  }

  @Step("And a new record with the same author is imported")
  private static JsonPath andNewRecordWithSameAuthorIsImported(ScopusAuthor author) {
    return importRecord(ScopusRecord.withAuthors(FIXTURE_AFID, author));
  }

  @Step("Then the author resolves to the fixture person")
  private static void thenAuthorResolvesToFixturePerson(JsonPath candidate, ScopusAuthor author) {
    assertThat(identityIdOfAuthor(candidate, author.auid())).isEqualTo(fixturePersonId());
  }

  @Step("And the unmapped co-author is affiliated with the fixture unit")
  private static void andUnmappedCoAuthorIsAffiliatedWithFixtureUnit(
      JsonPath candidate, ScopusAuthor coAuthor) {
    assertThat(affiliationIdsOfAuthor(candidate, coAuthor.auid())).containsExactly(fixtureUnitId());
  }

  @Step("And the candidate belongs to the fixture unit's customer")
  private static void andCandidateBelongsToFixtureCustomer(JsonPath candidate) {
    assertThat(candidate.getList("associatedCustomers", String.class))
        .contains(fixtureCustomerId());
  }

  @Step("Then the author is kept by name, without an identity")
  private static void thenAuthorIsKeptWithoutIdentity(JsonPath candidate, ScopusAuthor author) {
    assertThat(nameOfAuthor(candidate, author.auid())).isEqualTo(author.fullName());
    assertThat(identityIdOfAuthor(candidate, author.auid())).isNull();
  }

  @Step("Then the candidate has the title of the record")
  private static void thenCandidateHasTitleOfRecord(JsonPath candidate, ScopusRecord scopusRecord) {
    assertThat(candidate.getString("entityDescription.mainTitle")).isEqualTo(scopusRecord.title());
  }

  @Step("Then an error report says no customer was found")
  private static void thenErrorReportSaysNoCustomerWasFound(ScopusRecord scopusRecord) {
    var errorReport =
        pollUntil(
                ERROR_REPORT_TIMEOUT,
                () -> ScopusImportPipeline.findErrorReport(scopusRecord, NO_CUSTOMER_EXCEPTION),
                Optional::isPresent)
            .orElseThrow();
    assertThat(errorReport).contains(scopusRecord.eid());
  }

  @Step("And no candidate is searchable")
  private static void andNoCandidateIsSearchable(ScopusRecord scopusRecord) {
    assertThat(ImportCandidates.totalHits(scopusRecord.eid())).isZero();
  }

  private static JsonPath importRecord(ScopusRecord scopusRecord) {
    ScopusImportPipeline.submitRecord(scopusRecord);
    return awaitCandidate(scopusRecord.eid());
  }

  private static String fixtureCustomerId() {
    var customerId =
        givenUnauthenticatedJsonRequest()
            .get(CUSTOMERS_PATH)
            .then()
            .statusCode(HTTP_OK)
            .extract()
            .jsonPath()
            .param("unitId", fixtureUnitId())
            .getString("customers.find { it.cristinId == unitId }.id");
    return requireNonNull(
        customerId,
        () -> "No customer with cristinId %s in GET %s".formatted(fixtureUnitId(), CUSTOMERS_PATH));
  }
}
