package no.sikt.nva.apitest.base;

/**
 * Cristin projects that exist in the test environment. Two of them, because the batch update tests
 * move publications from one project to another and need both ends of that move to be real. These
 * are the same projects NVA-end-to-end-testing puts on its test registrations.
 */
public enum Project {
  CURRENT("https://api.e2e.nva.aws.unit.no/cristin/project/2745236"),
  REPLACEMENT("https://api.e2e.nva.aws.unit.no/cristin/project/2674874");

  private static final String IDENTIFIER_SEPARATOR = "/";

  private final String value;

  Project(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  /** The bare cristin identifier, which is what the handler and the search api take as a value. */
  public String getIdentifier() {
    return value.substring(value.lastIndexOf(IDENTIFIER_SEPARATOR) + 1);
  }
}
