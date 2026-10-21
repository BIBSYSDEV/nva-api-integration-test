package no.sikt.nva.apitest.scopus;

import static java.util.concurrent.TimeUnit.MINUTES;
import static no.sikt.nva.apitest.base.Polling.pollUntil;
import static no.sikt.nva.apitest.base.UserFixtures.APP_ADMIN;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.scopus.ImportCandidates.affiliationIdsOfAuthor;
import static no.sikt.nva.apitest.scopus.ImportCandidates.awaitCandidateIdentifier;
import static no.sikt.nva.apitest.scopus.ImportCandidates.identityIdOfAuthor;
import static no.sikt.nva.apitest.scopus.ImportCandidates.nameOfAuthor;
import static no.sikt.nva.apitest.scopus.ScopusAuthor.randomFakeAuid;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.MAPPED_AFID;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.MAPPED_AUID;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.MAPPING_NOT_CHOSEN_MESSAGE;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.mappedCustomerId;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.mappedPersonId;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.mappedUnitId;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.mappingIsChosen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import no.sikt.nva.apitest.base.User;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Exercises the deployed Scopus import in nva-publication-api from the point where the unzipper
 * hands a record over: each test writes its own generated record to the XML bucket and sends the
 * matching message to ScopusImportQueue, then finds the import candidate through the search. One
 * test starts one step earlier, at the zip bucket. The email intake in front of that is not
 * covered.
 *
 * <p>The read-path tests rely on a real AUID and AFID mapping that NP-52110 picks (see {@link
 * ScopusFixtures}), and are skipped until it is filled in. They pass through PIA today and through
 * the DynamoDB mappings after the migration, unchanged.
 *
 * <p>Nothing is cleaned up: every record has its own EID, so its candidate never collides with one
 * from an earlier run.
 */
@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Scopus import (S3 and SQS)")
class ScopusImportTest extends IntegrationTestBase {

  /** The only e2e test user with MANAGE_IMPORT, through the App-admin role, at Sikt. */
  private static final User IMPORT_CURATOR = APP_ADMIN;

  /** The person a curator sets on an unmapped author in the correction test. */
  private static final User CORRECTED_PERSON = UIB_CONTRIBUTOR;

  private static final Duration ERROR_REPORT_TIMEOUT = Duration.ofMinutes(5);
  private static final String NO_CUSTOMER_EXCEPTION = "MissingNvaContributorException";
  private static final String AUTHOR_SURNAME = "Author";
  private static final String TYPE = "type";

  /**
   * A mapped AUID resolves to its Cristin person, and a mapped AFID to its unit and to the customer
   * of the unit's institution.
   *
   * <p>The unit is asserted on an unmapped co-author in the same author group, because a mapped
   * author gets the affiliations of their Cristin profile rather than the one in the record. The
   * AFID decides only the affiliation of unmapped authors and the customer.
   */
  @Test
  @DisplayName("Mapped author and affiliation resolve to Cristin person, unit and customer")
  @Description
  void shouldResolveMappedAuthorAndAffiliationWhenRecordIsImported(SoftAssertions softly) {
    assumeTrue(mappingIsChosen(), MAPPING_NOT_CHOSEN_MESSAGE);
    var coAuthorAuid = randomFakeAuid();
    var scopusRecord =
        ScopusRecord.withAuthors(
            MAPPED_AFID,
            new ScopusAuthor(MAPPED_AUID, 1, "Mapped", AUTHOR_SURNAME),
            new ScopusAuthor(coAuthorAuid, 2, "Unmapped", "Coauthor"));

    ScopusImportPipeline.submitRecord(scopusRecord);
    var candidate = awaitCandidate(scopusRecord);

    softly.assertThat(identityIdOfAuthor(candidate, MAPPED_AUID)).isEqualTo(mappedPersonId());
    softly
        .assertThat(affiliationIdsOfAuthor(candidate, coAuthorAuid))
        .containsExactly(mappedUnitId());
    softly
        .assertThat(candidate.getList("associatedCustomers", String.class))
        .containsExactly(mappedCustomerId());
  }

  /** An author whose AUID is not mapped and who has no ORCID is kept, but has no identity id. */
  @Test
  @DisplayName("Unknown author has no identity")
  @Description
  void shouldNotIdentifyAuthorWhenAuidIsNotMapped(SoftAssertions softly) {
    assumeTrue(mappingIsChosen(), MAPPING_NOT_CHOSEN_MESSAGE);
    var unknownAuthor = new ScopusAuthor(randomFakeAuid(), 1, "Unknown", AUTHOR_SURNAME);
    var scopusRecord = ScopusRecord.withAuthors(MAPPED_AFID, unknownAuthor);

    ScopusImportPipeline.submitRecord(scopusRecord);
    var candidate = awaitCandidate(scopusRecord);

    softly
        .assertThat(nameOfAuthor(candidate, unknownAuthor.auid()))
        .isEqualTo(unknownAuthor.fullName());
    softly.assertThat(identityIdOfAuthor(candidate, unknownAuthor.auid())).isNull();
  }

  /**
   * A curator who sets a person on an unmapped author while importing a candidate teaches the
   * import that mapping, so the next record with the same AUID resolves to that person.
   *
   * <p>The AUID is fake and new for every run, so the real fixture mapping is never corrected and
   * nothing needs resetting. Before the PIA migration this depends on the PIA write path working
   * (NP-51586) and on PIA accepting the test person.
   */
  @Test
  @DisplayName("Author correction made during import is used by the next record")
  @Description
  @Timeout(value = 15, unit = MINUTES)
  void shouldResolveAuthorToCorrectedPersonWhenSameAuidIsImportedAgain() {
    assumeTrue(mappingIsChosen(), MAPPING_NOT_CHOSEN_MESSAGE);
    var correctedAuthor = new ScopusAuthor(randomFakeAuid(), 1, "Corrected", AUTHOR_SURNAME);
    var firstRecord = ScopusRecord.withAuthors(MAPPED_AFID, correctedAuthor);

    ScopusImportPipeline.submitRecord(firstRecord);
    var firstCandidateIdentifier = awaitCandidateIdentifier(IMPORT_CURATOR, firstRecord.eid());
    var firstCandidate = ImportCandidates.fetch(IMPORT_CURATOR, firstCandidateIdentifier);
    ImportCandidates.importAsPublication(
        IMPORT_CURATOR,
        firstCandidateIdentifier,
        entityDescriptionWithCorrectedAuthor(firstCandidate, correctedAuthor.auid()));

    var secondRecord = ScopusRecord.withAuthors(MAPPED_AFID, correctedAuthor);
    ScopusImportPipeline.submitRecord(secondRecord);
    var secondCandidate = awaitCandidate(secondRecord);

    assertThat(identityIdOfAuthor(secondCandidate, correctedAuthor.auid()))
        .isEqualTo(CORRECTED_PERSON.cristinUri());
  }

  /** A zip with one record in the zip bucket is unzipped, queued and imported as a candidate. */
  @Test
  @DisplayName("Record delivered in a zip becomes an import candidate")
  @Description
  void shouldCreateCandidateWhenRecordIsUploadedAsZip() {
    assumeTrue(mappingIsChosen(), MAPPING_NOT_CHOSEN_MESSAGE);
    var scopusRecord =
        ScopusRecord.withAuthors(
            MAPPED_AFID, new ScopusAuthor(randomFakeAuid(), 1, "Zipped", AUTHOR_SURNAME));

    ScopusImportPipeline.submitZip(scopusRecord);
    var candidate = awaitCandidate(scopusRecord);

    assertThat(candidate.getString("entityDescription.mainTitle")).isEqualTo(scopusRecord.title());
  }

  /**
   * A record whose only affiliation has an unmapped AFID and a name that matches no Cristin
   * organization belongs to no customer, so the import fails with an error report and no candidate
   * is created.
   */
  @Test
  @DisplayName("Record with no customer affiliation gives an error report and no candidate")
  @Description
  void shouldNotCreateCandidateWhenNoAffiliationBelongsToCustomer(SoftAssertions softly) {
    var scopusRecord =
        ScopusRecord.withAuthors(
            ScopusRecord.randomFakeAfid(),
            new ScopusAuthor(randomFakeAuid(), 1, "Customerless", AUTHOR_SURNAME));

    ScopusImportPipeline.submitRecord(scopusRecord);
    var errorReport =
        pollUntil(
                ERROR_REPORT_TIMEOUT,
                () -> ScopusImportPipeline.findErrorReport(scopusRecord, NO_CUSTOMER_EXCEPTION),
                Optional::isPresent)
            .orElseThrow();

    softly.assertThat(errorReport).contains(scopusRecord.eid());
    softly
        .assertThat(ImportCandidates.search(IMPORT_CURATOR, scopusRecord.eid()).getInt("totalHits"))
        .isZero();
  }

  private static JsonPath awaitCandidate(ScopusRecord scopusRecord) {
    var candidateIdentifier = awaitCandidateIdentifier(IMPORT_CURATOR, scopusRecord.eid());
    return ImportCandidates.fetch(IMPORT_CURATOR, candidateIdentifier);
  }

  /**
   * The publication the curator submits, with the single author set to the corrected person. The
   * author keeps the AUID, which is how the import recognises the contributor as the same Scopus
   * author and writes the correction back as a mapping.
   */
  private static Map<String, Object> entityDescriptionWithCorrectedAuthor(
      JsonPath candidate, String correctedAuid) {
    var entityDescription = new LinkedHashMap<String, Object>();
    entityDescription.put(TYPE, "EntityDescription");
    entityDescription.put("mainTitle", candidate.getString("entityDescription.mainTitle"));
    entityDescription.put("publicationDate", candidate.getMap("entityDescription.publicationDate"));
    entityDescription.put("reference", candidate.getMap("entityDescription.reference"));
    entityDescription.put("contributors", List.of(correctedContributor(correctedAuid)));
    return entityDescription;
  }

  private static Map<String, Object> correctedContributor(String correctedAuid) {
    var contributor = new LinkedHashMap<String, Object>();
    contributor.put(TYPE, "Contributor");
    contributor.put("sequence", 1);
    contributor.put("role", Map.of(TYPE, "Creator"));
    contributor.put("identity", correctedIdentity(correctedAuid));
    contributor.put("affiliations", List.of(Map.of(TYPE, "Organization", "id", mappedUnitId())));
    return contributor;
  }

  private static Map<String, Object> correctedIdentity(String correctedAuid) {
    var scopusAuthorIdentifier = new LinkedHashMap<String, Object>();
    scopusAuthorIdentifier.put(TYPE, "AdditionalIdentifier");
    scopusAuthorIdentifier.put("sourceName", "scopus-auid");
    scopusAuthorIdentifier.put("value", correctedAuid);

    var identity = new LinkedHashMap<String, Object>();
    identity.put(TYPE, "Identity");
    identity.put("id", CORRECTED_PERSON.cristinUri());
    identity.put("name", CORRECTED_PERSON.name());
    identity.put("additionalIdentifiers", List.of(scopusAuthorIdentifier));
    return identity;
  }
}
