package no.sikt.nva.apitest.search;

import java.util.List;

public class BibTexExpectationFixtures {

  private static final String ISBN = "isbn = {9783161484100}";
  private static final String MONOGRAPH_PAGES = "pages = {150}";

  public static final BibTexExpectation EXPECTED_BIBTEX_ACADEMIC_ARTICLE =
      new BibTexExpectation(
          "article",
          List.of(
              "journal = {ACM Journal of Data and Information Quality}",
              "issn = {1936-1963}",
              "note = {nva type: AcademicArticle}",
              "number = {1}",
              "pages = {10--20}",
              "volume = {3}"));
  public static final BibTexExpectation EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH =
      new BibTexExpectation(
          "book",
          List.of(
              ISBN,
              "note = {nva type: AcademicMonograph}",
              MONOGRAPH_PAGES,
              "publisher = {Springer Nature}"));
  public static final BibTexExpectation EXPECTED_BIBTEX_BOOK_ANTHOLOGY =
      new BibTexExpectation(
          "book",
          List.of(
              ISBN,
              "note = {nva type: BookAnthology}",
              MONOGRAPH_PAGES,
              "publisher = {Springer Nature}"));
  public static final BibTexExpectation EXPECTED_BIBTEX_DEGREE_MASTER =
      new BibTexExpectation(
          "mastersthesis",
          List.of(
              "note = {nva type: DegreeMaster}",
              MONOGRAPH_PAGES,
              "school = {SINTEF akademisk forlag}"));
  public static final BibTexExpectation EXPECTED_BIBTEX_DEGREE_PHD =
      new BibTexExpectation(
          "phdthesis",
          List.of(
              ISBN,
              "note = {nva type: DegreePhd}",
              MONOGRAPH_PAGES,
              "school = {SINTEF akademisk forlag}"));
  public static final BibTexExpectation EXPECTED_BIBTEX_ACADEMIC_CHAPTER =
      new BibTexExpectation(
          "inbook",
          List.of(
              ISBN,
              "booktitle = {Anthology for BibTex integration test",
              "note = {nva type: AcademicChapter}",
              "pages = {1--20}",
              "publisher = {Springer Nature}"));
  public static final BibTexExpectation EXPECTED_BIBTEX_REPORT_RESEARCH =
      new BibTexExpectation(
          "techreport",
          List.of(
              ISBN,
              "note = {nva type: ReportResearch}",
              MONOGRAPH_PAGES,
              "institution = {SINTEF akademisk forlag}"));
  public static final BibTexExpectation EXPECTED_BIBTEX_CONFERENCE_LECTURE =
      new BibTexExpectation(
          "inproceedings",
          List.of("note = {nva type: ConferenceLecture", "booktitle = {Conference lecture}"));
}
