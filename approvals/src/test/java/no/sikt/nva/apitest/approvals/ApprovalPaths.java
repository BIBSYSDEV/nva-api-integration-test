package no.sikt.nva.apitest.approvals;

public final class ApprovalPaths {

  public static final String BASE_PATH = "/approval";

  public static final String CONTEXT_PATH = BASE_PATH + "/context";
  public static final String ONTOLOGY_PATH = BASE_PATH + "/ontology";
  public static final String APPROVAL_PATH = BASE_PATH + "/{approvalId}";

  private ApprovalPaths() {}
}
