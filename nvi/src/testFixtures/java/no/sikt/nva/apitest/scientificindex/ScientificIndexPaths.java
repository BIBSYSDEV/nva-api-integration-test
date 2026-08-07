package no.sikt.nva.apitest.scientificindex;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class ScientificIndexPaths {

  public static final String BASE_PATH = "/scientific-index";

  public static final String PERIOD_PATH = BASE_PATH + "/period/{year}";
  public static final String LIST_PERIODS_PATH = BASE_PATH + "/period";

  public static final String CANDIDATE_SEARCH_PATH = BASE_PATH + "/candidate";
  public static final String CANDIDATE_BY_IDENTIFIER_PATH = BASE_PATH + "/candidate/{identifier}";
  public static final String CANDIDATE_APPROVAL_STATUS_PATH =
      CANDIDATE_BY_IDENTIFIER_PATH + "/status";
  public static final String CANDIDATE_FOR_PUBLICATION_PATH =
      BASE_PATH + "/candidate/publication/{identifier}";
  public static final String REPORT_STATUS_FOR_PUBLICATION_PATH =
      BASE_PATH + "/publication/{identifier}/report-status";

  private ScientificIndexPaths() {}

  public static String encode(String publicationId) {
    return URLEncoder.encode(publicationId, StandardCharsets.UTF_8);
  }
}
