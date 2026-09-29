package no.sikt;

public enum TicketType {

  DOI_REQUEST("DoiRequest");

  private String value;

  private TicketType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
