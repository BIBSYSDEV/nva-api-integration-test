package no.sikt.nva.apitest.scientificindex;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class ScientificIndexPaths {

  private static final String BASE_PATH = "/scientific-index";
  private static final String PERIOD_PATH = BASE_PATH + "/period";
  private static final String CANDIDATE_PATH = BASE_PATH + "/candidate";

  private ScientificIndexPaths() {}

  public static String listPeriodsPath() {
    return PERIOD_PATH;
  }

  public static String periodPath(String publishingYear) {
    return PERIOD_PATH + "/" + publishingYear;
  }

  public static String candidatePath(String candidateIdentifier) {
    return CANDIDATE_PATH + "/" + candidateIdentifier;
  }

  public static String candidateByPublicationIdPath(String publicationId) {
    return CANDIDATE_PATH + "/publication/" + encode(publicationId);
  }

  public static String candidateStatusPath(String candidateIdentifier) {
    return candidatePath(candidateIdentifier) + "/status";
  }

  public static String reportStatusPath(String publicationId) {
    return BASE_PATH + "/publication/" + encode(publicationId) + "/report-status";
  }

  private static String encode(String publicationId) {
    return URLEncoder.encode(publicationId, StandardCharsets.UTF_8);
  }
}
