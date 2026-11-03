package no.sikt.nva.apitest.scopus;

import static no.sikt.nva.apitest.scopus.ImportCandidates.awaitCandidate;
import static no.sikt.nva.apitest.scopus.ImportCandidates.identityIdOfAuthor;
import static no.sikt.nva.apitest.scopus.ImportCandidates.importWithFixturePersonAsOnlyAuthor;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.FIXTURE_AFID;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.FIXTURE_AUID;
import static no.sikt.nva.apitest.scopus.ScopusFixtures.fixturePersonId;

import io.qameta.allure.Allure;
import io.restassured.path.json.JsonPath;

/** Maps the fixture AUID to the fixture person the way a curator does, by importing a candidate. */
public final class FixtureAuthorMapping {

  private static final String MAPPING_PARAMETER = "Fixture author mapping";

  private FixtureAuthorMapping() {}

  public static void createIfMissing() {
    var scopusRecord =
        ScopusRecord.withAuthors(FIXTURE_AFID, new ScopusAuthor(FIXTURE_AUID, "Fixture", "Author"));
    ScopusImportPipeline.submitRecord(scopusRecord);
    var candidate = awaitCandidate(scopusRecord.eid());

    if (authorResolvesToFixturePerson(candidate)) {
      Allure.parameter(MAPPING_PARAMETER, "already existed");
    } else {
      importWithFixturePersonAsOnlyAuthor(candidate, FIXTURE_AUID);
      Allure.parameter(MAPPING_PARAMETER, "created by importing " + scopusRecord.eid());
    }
  }

  private static boolean authorResolvesToFixturePerson(JsonPath candidate) {
    return fixturePersonId().equals(identityIdOfAuthor(candidate, FIXTURE_AUID));
  }
}
