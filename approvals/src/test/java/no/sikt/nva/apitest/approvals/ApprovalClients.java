package no.sikt.nva.apitest.approvals;

/**
 * The external clients the approval tests write as, seeded by NVA-end-to-end-testing. They belong
 * to two different customers, because the approval API allows a client to write only the identifier
 * names its own customer is registered for. Writing the other customer's name must be rejected.
 */
public final class ApprovalClients {

  public static final String UIB_CLIENT_SECRET = "ApiTestApprovalClientUib";
  public static final String UIS_CLIENT_SECRET = "ApiTestApprovalClientUis";

  /** The identifier names each customer is registered for, seeded alongside the clients. */
  public static final String UIB_IDENTIFIER_NAME = "apitest-uib";

  public static final String UIS_IDENTIFIER_NAME = "apitest-uis";

  private ApprovalClients() {}
}
