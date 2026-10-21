package no.sikt.nva.apitest.scopus;

import io.restassured.RestAssured;
import java.util.stream.Stream;

/**
 * The real, existing PIA mapping the read-path tests rely on: one Scopus author (AUID) mapped to a
 * Cristin person, and one Scopus affiliation (AFID) mapped to a Cristin unit at an institution that
 * is a customer in e2e. The tests only read this mapping and never correct it.
 *
 * <p>The values are not chosen yet. NP-52110 picks them, with a one-time check that the AUID has
 * exactly one candidate person in Cristin PROD, that the mapping also exists in PIA in Cristin
 * TEST, and that the person is active in Cristin TEST. Until then every value is {@link
 * #NOT_CHOSEN} and the tests that need the mapping are skipped. Filling them in here is the only
 * change needed.
 */
final class ScopusFixtures {

  static final String NOT_CHOSEN = "NOT-CHOSEN-SEE-NP-52110";

  /** The Scopus author id that PIA maps to {@link #MAPPED_CRISTIN_PERSON_IDENTIFIER}. */
  static final String MAPPED_AUID = NOT_CHOSEN;

  /** The numeric Cristin person identifier, as in {@code /cristin/person/<identifier>}. */
  static final String MAPPED_CRISTIN_PERSON_IDENTIFIER = NOT_CHOSEN;

  /** The Scopus affiliation id that PIA maps to {@link #MAPPED_CRISTIN_UNIT_IDENTIFIER}. */
  static final String MAPPED_AFID = NOT_CHOSEN;

  /** The Cristin unit identifier, as in {@code /cristin/organization/<identifier>}. */
  static final String MAPPED_CRISTIN_UNIT_IDENTIFIER = NOT_CHOSEN;

  /** The e2e customer identifier of the unit's institution, as in {@code /customer/<uuid>}. */
  static final String MAPPED_CUSTOMER_IDENTIFIER = NOT_CHOSEN;

  static final String MAPPING_NOT_CHOSEN_MESSAGE =
      "The fixture AUID, AFID, person, unit and customer are not chosen yet (NP-52110)";

  private ScopusFixtures() {}

  static boolean mappingIsChosen() {
    return Stream.of(
            MAPPED_AUID,
            MAPPED_CRISTIN_PERSON_IDENTIFIER,
            MAPPED_AFID,
            MAPPED_CRISTIN_UNIT_IDENTIFIER,
            MAPPED_CUSTOMER_IDENTIFIER)
        .noneMatch(NOT_CHOSEN::equals);
  }

  static String mappedPersonId() {
    return "%s/cristin/person/%s".formatted(RestAssured.baseURI, MAPPED_CRISTIN_PERSON_IDENTIFIER);
  }

  static String mappedUnitId() {
    return "%s/cristin/organization/%s"
        .formatted(RestAssured.baseURI, MAPPED_CRISTIN_UNIT_IDENTIFIER);
  }

  static String mappedCustomerId() {
    return "%s/customer/%s".formatted(RestAssured.baseURI, MAPPED_CUSTOMER_IDENTIFIER);
  }
}
