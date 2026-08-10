package no.sikt.nva.apitest.scientificindex;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class ScientificIndexPaths {

  public static final String BASE_PATH = "/scientific-index";

  public static final String CONTEXT_PATH = BASE_PATH + "/context";

  public static final String PERIODS_PATH = BASE_PATH + "/period";
  public static final String PERIOD_PATH = PERIODS_PATH + "/{period}";

  public static final String CANDIDATES_PATH = BASE_PATH + "/candidate";
  public static final String CANDIDATE_PATH = CANDIDATES_PATH + "/{candidate}";
  public static final String CANDIDATE_STATUS_PATH = CANDIDATE_PATH + "/status";
  public static final String CANDIDATE_ASSIGNEE_PATH = CANDIDATE_PATH + "/assignee";
  public static final String CANDIDATE_NOTES_PATH = CANDIDATE_PATH + "/note";
  public static final String DELETE_NOTE_PATH = CANDIDATE_NOTES_PATH + "/{note}";
  public static final String CANDIDATE_BY_PUBLICATION_PATH =
      CANDIDATES_PATH + "/publication/{publication}";

  public static final String REPORTS_PATH = BASE_PATH + "/reports";
  public static final String PERIOD_REPORT_PATH = REPORTS_PATH + "/{period}";
  public static final String INSTITUTION_REPORTS_PATH = PERIOD_REPORT_PATH + "/institutions";
  public static final String INSTITUTION_REPORT_PATH = INSTITUTION_REPORTS_PATH + "/{institution}";

  public static final String PUBLICATION_REPORT_STATUS_PATH =
      BASE_PATH + "/publication/{publication}/report-status";
  public static final String DEPRECATED_INSTITUTION_REPORT_PATH =
      BASE_PATH + "/institution-report/{period}";

  private ScientificIndexPaths() {}

  public static String encode(String publicationId) {
    return URLEncoder.encode(publicationId, StandardCharsets.UTF_8);
  }
}
