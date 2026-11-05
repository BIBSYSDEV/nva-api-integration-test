package no.sikt.nva.apitest.scopus;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Ids Scopus never issues: real AUIDs, AFIDs and the number in an EID all start well below 99.
 * Generated ids stay below 99999990000, the block reserved for {@link ScopusFixtures#FIXTURE_AUID}.
 */
public final class FakeScopusIds {

  private static final String FAKE_PREFIX = "99";
  private static final String EID_PREFIX = "2-s2.0-";
  private static final long SUFFIX_BOUND = 999_990_000L;

  private FakeScopusIds() {}

  public static String randomAuid() {
    return randomId();
  }

  public static String randomAfid() {
    return randomId();
  }

  public static String randomEid() {
    return EID_PREFIX + randomId();
  }

  private static String randomId() {
    return FAKE_PREFIX + "%09d".formatted(ThreadLocalRandom.current().nextLong(SUFFIX_BOUND));
  }
}
