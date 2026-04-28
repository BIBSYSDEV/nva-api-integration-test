package no.sikt;

public enum Affiliation {
  UIB("https://api.e2e.nva.aws.unit.no/cristin/organization/184.0.0.0");

  private final String value;

  Affiliation(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
