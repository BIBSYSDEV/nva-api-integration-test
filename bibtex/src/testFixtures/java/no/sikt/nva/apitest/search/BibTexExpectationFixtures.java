package no.sikt.nva.apitest.search;

import java.util.List;

public class BibTexExpectationFixtures {

  public static final BibTexExpectation EXPECTED_BIBTEX_ACADEMIC_ARTICLE = new BibTexExpectation(
      "article",
      List.of(
          "journal = {ACM Journal of Data and Information Quality}",
          "issn = {1936-1963}",
          "note = {nva type: AcademicArticle}",
          "number = {1}",
          "pages = {10--20}",
          "volume = {3}"));
  public static final BibTexExpectation EXPECTED_BIBTEX_ACADEMIC_MONOGRAPH = new BibTexExpectation(
      "book",
      List.of(
          "isbn = {9783161484100}",
          "note = {nva type: AcademicMonograph}",
          "pages = {150}",
          "publisher = {Springer Nature}"));
  public static final BibTexExpectation EXPECTED_BIBTEX_BOOK_ANTHOLOGY = new BibTexExpectation(
      "book",
      List.of(
          "isbn = {9783161484100}",
          "note = {nva type: BookAnthology}",
          "pages = {150}",
          "publisher = {Springer Nature}"));
  public static final BibTexExpectation EXPECTED_BIBTEX_ACADEMIC_CHAPTER = new BibTexExpectation(
      "inbook",
      List.of(
          "isbn = {9783161484100}",
          "note = {nva type: AcademicChapter}",
          "pages = {1--20}",
          "publisher = {Springer Nature}"));
}
