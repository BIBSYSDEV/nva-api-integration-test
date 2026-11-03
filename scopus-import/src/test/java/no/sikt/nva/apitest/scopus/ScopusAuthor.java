package no.sikt.nva.apitest.scopus;

/** An author in a generated Scopus record, identified in Scopus by the AUID. */
record ScopusAuthor(String auid, String givenName, String surname) {

  /** The name the import gives an author it cannot identify. */
  String fullName() {
    return givenName + " " + surname;
  }

  String initials() {
    return givenName.charAt(0) + ".";
  }
}
