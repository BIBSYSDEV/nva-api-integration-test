package no.sikt.nva.apitest.kanalregister;

/** Fixture channels, verified against both environments 2026-07-16. */
public final class ChannelFixtures {

  // Journal: level 2 every year 2022-2027 (2022-2026 in kar-test).
  public static final Channel ACP =
      new Channel(
          "A135A19F-4111-4184-AB2F-7C00AA24BA05", "Atmospheric Chemistry and Physics", "2", 2024);

  public static final String ACP_EISSN = "1680-7324";

  // Series: the name matches several channels, so search hits are picked by PID.
  public static final Channel LNCS =
      new Channel(
          "C8CCD71B-FD8B-47B4-B72A-905F6219D7D5", "Lecture Notes in Computer Science", "1", 2024);

  // Publisher: named differently per environment, so only used for lookups by PID.
  public static final Channel GYLDENDAL =
      new Channel("9B2EE655-49AC-48D8-B33E-DADA32CDDB40", "Gyldendal Forlag", "1", 2025);

  // Publisher: the name matches exactly one channel in both environments; used for search.
  public static final Channel GYLDENDAL_UNDERVISNING =
      new Channel("69843B08-E4DC-4D51-AA13-CFB03107AA92", "Gyldendal Undervisning", "0", 2025);

  // X-channel: levelDisplay "X" in 2026 with counting level 1; no level data in kar-test.
  public static final Channel JCM =
      new Channel(
          "30EAF3E3-A276-4E8E-86CC-9BCFFAFAA94E", "Journal of clinical medicine", "1", 2026);

  private ChannelFixtures() {}
}
