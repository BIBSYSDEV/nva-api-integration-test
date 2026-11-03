package no.sikt.nva.apitest.scopus;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A record with one author group and its own EID, rendered from {@code resources/scopus}. Authors
 * are numbered in the order given. The affiliation name matches no Cristin organization.
 */
record ScopusRecord(String eid, List<ScopusAuthor> authors, String afid) {

  private static final String RECORD_TEMPLATE =
      stringFromResources(Path.of("scopus", "record.xml"));
  private static final String AUTHOR_TEMPLATE =
      stringFromResources(Path.of("scopus", "author.xml"));

  static ScopusRecord withAuthors(String afid, ScopusAuthor... authors) {
    return new ScopusRecord(FakeScopusIds.randomEid(), List.of(authors), afid);
  }

  String title() {
    return "NVA API test Scopus import " + eid;
  }

  String fileName() {
    return eid + ".xml";
  }

  String toXml() {
    return fill(
        RECORD_TEMPLATE,
        Map.of(
            "EID", eid,
            "TITLE", title(),
            "YEAR", currentYear(),
            "AUTHORS", authorsXml(),
            "AFID", afid,
            "AFFILIATION_NAME", affiliationName()));
  }

  private String affiliationName() {
    return "NVA API test affiliation " + eid;
  }

  private static String currentYear() {
    return Integer.toString(Year.now(ZoneOffset.UTC).getValue());
  }

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

  private String authorsXml() {
    return IntStream.range(0, authors.size())
        .mapToObj(index -> authorXml(authors.get(index), index + 1))
        .collect(joining());
  }

  private static String authorXml(ScopusAuthor author, int sequence) {
    return fill(
        AUTHOR_TEMPLATE,
        Map.of(
            "AUID", author.auid(),
            "SEQUENCE", Integer.toString(sequence),
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
}
