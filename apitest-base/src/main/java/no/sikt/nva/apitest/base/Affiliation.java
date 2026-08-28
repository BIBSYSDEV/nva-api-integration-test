package no.sikt.nva.apitest.base;

public enum Affiliation {
  UIB("184.0.0.0"),
  KRISTIANIA("1615.0.0.0"),
  OSLO_MET("215.0.0.0"),
  UIS("217.0.0.0"),
  SIKT("20754.0.0.0");

  private final String cristinId;

  Affiliation(String cristinId) {
    this.cristinId = cristinId;
  }

  public String getCristinId() {
    return cristinId;
  }

  public String getValue() {
    return "https://api.e2e.nva.aws.unit.no/cristin/organization/" + cristinId;
  }
}
