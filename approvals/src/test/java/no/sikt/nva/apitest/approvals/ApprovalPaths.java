package no.sikt.nva.apitest.approvals;

import io.restassured.RestAssured;

public final class ApprovalPaths {

  private static final String ABSOLUTE_PATH_FORMAT = "%s%s";

  public static final String BASE_PATH = "/approval";

  public static final String CONTEXT_PATH = BASE_PATH + "/context";
  public static final String CURRENT_CONTEXT_PATH = CONTEXT_PATH + "/v1";
  public static final String ONTOLOGY_PATH = BASE_PATH + "/ontology";
  public static final String CURRENT_ONTOLOGY_PATH = ONTOLOGY_PATH + "/1.0.0";
  public static final String APPROVAL_PATH = BASE_PATH + "/{approvalId}";

  private ApprovalPaths() {}

  public static String absolute(String path) {
    return ABSOLUTE_PATH_FORMAT.formatted(RestAssured.baseURI, path);
  }
}
