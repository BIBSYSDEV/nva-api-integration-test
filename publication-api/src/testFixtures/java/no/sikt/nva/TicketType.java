package no.sikt.nva;

/** TicketType */
public enum TicketType {
  DOI_REQUEST("DoiRequest");

  private String type;

  private TicketType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
