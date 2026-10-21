package no.sikt.nva.apitest.scopus;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A generated Scopus record with one author group, rendered from the minimal template in {@code
 * src/test/resources/scopus}. Every record gets its own EID, so a test never passes on a candidate
 * left behind by an earlier run.
 */
record ScopusRecord(
    String eid, String title, List<ScopusAuthor> authors, String afid, String affiliationName) {

  /** Like the fake AUIDs, real EIDs never start with 99 after the 2-s2.0- prefix. */
  private static final String FAKE_EID_PREFIX = "2-s2.0-99";

  private static final long FAKE_EID_SUFFIX_BOUND = 1_000_000_000L;
  private static final String FAKE_ID_PREFIX = "99";
  private static final long FAKE_AFID_BOUND = 100_000_000L;
  private static final String RECORD_TEMPLATE = readTemplate("/scopus/record.xml");
  private static final String AUTHOR_TEMPLATE = readTemplate("/scopus/author.xml");
  private static final String XML_SUFFIX = ".xml";

  ScopusRecord {
    authors = List.copyOf(authors);
  }

  static ScopusRecord withAuthors(String afid, ScopusAuthor... authors) {
    var eid = randomEid();
    return new ScopusRecord(
        eid,
        "NVA API test Scopus import " + eid,
        List.of(authors),
        afid,
        "NVA API test affiliation " + UUID.randomUUID());
  }

  static String randomEid() {
    var suffix = ThreadLocalRandom.current().nextLong(FAKE_EID_SUFFIX_BOUND);
    return FAKE_EID_PREFIX + "%09d".formatted(suffix);
  }

  /** Real AFIDs are 8 digits starting with 6, so a 10 digit id starting with 99 is never mapped. */
  static String randomFakeAfid() {
    return FAKE_ID_PREFIX + "%08d".formatted(ThreadLocalRandom.current().nextLong(FAKE_AFID_BOUND));
  }

  /** The file name the unzipper would give this record in the XML bucket. */
  String fileName() {
    return eid + XML_SUFFIX;
  }

  String toXml() {
    return fill(
        RECORD_TEMPLATE,
        Map.of(
            "EID", eid,
            "TITLE", title,
            "AUTHORS", authors.stream().map(ScopusRecord::authorXml).collect(joining()),
            "AFID", afid,
            "AFFILIATION_NAME", affiliationName));
  }

  /** A zip with this record as its only entry, the way Scopus delivers records by email. */
  byte[] toZip() {
    var zipBytes = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(zipBytes)) {
      zip.putNextEntry(new ZipEntry(fileName()));
      zip.write(toXml().getBytes(UTF_8));
      zip.closeEntry();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    return zipBytes.toByteArray();
  }

  private static String authorXml(ScopusAuthor author) {
    return fill(
        AUTHOR_TEMPLATE,
        Map.of(
            "AUID", author.auid(),
            "SEQUENCE", Integer.toString(author.sequence()),
            "INITIALS", author.initials(),
            "SURNAME", author.surname(),
            "GIVEN_NAME", author.givenName()));
  }

  private static String fill(String template, Map<String, String> values) {
    var filled = template;
    for (var placeholder : values.entrySet()) {
      filled = filled.replace("{{" + placeholder.getKey() + "}}", placeholder.getValue());
    }
    return filled;
  }

  private static String readTemplate(String resourceName) {
    try (var templateStream = ScopusRecord.class.getResourceAsStream(resourceName)) {
      return new String(templateStream.readAllBytes(), UTF_8);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
