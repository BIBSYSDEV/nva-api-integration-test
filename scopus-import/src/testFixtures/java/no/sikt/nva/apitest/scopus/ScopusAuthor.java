package no.sikt.nva.apitest.scopus;

/** An author in a generated Scopus record, identified in Scopus by the AUID. */
public record ScopusAuthor(String auid, String givenName, String surname) {

  /** The name the import gives an author it cannot identify. */
  public String fullName() {
    return givenName + " " + surname;
  }

  public String initials() {
    return givenName.charAt(0) + ".";
  }
}
