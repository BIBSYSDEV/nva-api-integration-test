package no.sikt.nva.apitest.publication;

public final class PublicationPaths {

  private static final String BASE_PATH = "/publication/";
  private static final String FILE_UPLOAD = "/file-upload";

  private PublicationPaths() {}

  public static String createPublicationPath() {
    return BASE_PATH;
  }

  public static String publicationPath(String identifier) {
    return BASE_PATH + identifier;
  }

  public static String publishPublicationPath(String identifier) {
    return publicationPath(identifier) + "/publish";
  }

  public static String fileUploadCreatePath(String identifier) {
    return publicationPath(identifier) + FILE_UPLOAD + "/create";
  }

  public static String fileUploadPreparePath(String identifier) {
    return publicationPath(identifier) + FILE_UPLOAD + "/prepare";
  }

  public static String fileUploadCompletePath(String identifier) {
    return publicationPath(identifier) + FILE_UPLOAD + "/complete";
  }
}
