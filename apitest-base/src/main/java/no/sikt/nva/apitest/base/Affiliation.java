package no.sikt.nva.apitest.base;

public enum Affiliation {
  UIB("https://api.e2e.nva.aws.unit.no/cristin/organization/184.0.0.0"),
  KRISTIANIA("https://api.e2e.nva.aws.unit.no/cristin/organization/1615.0.0.0"),
  OSLO_MET("https://api.e2e.nva.aws.unit.no/cristin/organization/215.0.0.0"),
  UIS("https://api.e2e.nva.aws.unit.no/cristin/organization/217.0.0.0");

  private final String value;

  Affiliation(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
