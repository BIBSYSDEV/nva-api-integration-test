package no.sikt.nva.apitest.search;

import java.util.List;

public record BibTexExpectation(String bibtexType, List<String> expectations) {}
