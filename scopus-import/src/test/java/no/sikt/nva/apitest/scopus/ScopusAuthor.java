package no.sikt.nva.apitest.scopus;

import java.util.concurrent.ThreadLocalRandom;

/** An author in a generated Scopus record, identified in Scopus by the AUID. */
record ScopusAuthor(String auid, int sequence, String givenName, String surname) {

  /**
   * Real AUIDs are 10 or 11 digits and currently start well below 99, so an 11 digit id starting
   * with 99 is unmapped in PIA and cannot collide with a real author.
   */
  private static final String FAKE_AUID_PREFIX = "99";

  private static final long FAKE_AUID_SUFFIX_BOUND = 1_000_000_000L;

  static String randomFakeAuid() {
    var suffix = ThreadLocalRandom.current().nextLong(FAKE_AUID_SUFFIX_BOUND);
    return FAKE_AUID_PREFIX + "%09d".formatted(suffix);
  }

  /** The name the import gives an author it cannot identify: given name, then surname. */
  String fullName() {
    return givenName + " " + surname;
  }

  String initials() {
    return givenName.charAt(0) + ".";
  }
}
