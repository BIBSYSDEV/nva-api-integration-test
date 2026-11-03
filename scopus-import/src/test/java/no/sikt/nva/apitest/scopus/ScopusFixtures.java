package no.sikt.nva.apitest.scopus;

import static no.sikt.nva.apitest.base.UserFixtures.APP_ADMIN;

import io.restassured.RestAssured;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.base.User;

/** Ids the suite owns, so the tests need no manual database writes in any environment. */
final class ScopusFixtures {

  /** Fake, and mapped to the fixture person by {@link FixtureAuthorMapping}. */
  static final String FIXTURE_AUID = "99999990001";

  /** "test testesen" at Sikt. Has a birth date, which pia-rest requires for corrections. */
  static final String FIXTURE_PERSON_IDENTIFIER = "1504353";

  static final String FIXTURE_PERSON_NAME = "test testesen";

  /** Real, and mapped to {@link #FIXTURE_UNIT} in PIA in both Cristin TEST and PROD. */
  static final String FIXTURE_AFID = "116240629";

  /** Sikt's top level, 20754.0.0.0. */
  static final Affiliation FIXTURE_UNIT = Affiliation.SIKT;

  /** The only Sikt test user with MANAGE_IMPORT. */
  static final User IMPORT_CURATOR = APP_ADMIN;

  private ScopusFixtures() {}

  static String fixturePersonId() {
    return "%s/cristin/person/%s".formatted(RestAssured.baseURI, FIXTURE_PERSON_IDENTIFIER);
  }

  static String fixtureUnitId() {
    return FIXTURE_UNIT.getValue();
  }
}
