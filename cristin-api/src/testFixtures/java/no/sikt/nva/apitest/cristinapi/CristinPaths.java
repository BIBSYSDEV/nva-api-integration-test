package no.sikt.nva.apitest.cristinapi;

public final class CristinPaths {

  private static final String ID_PARAM = "/{id}";

  public static final String PROJECTS_BASE_PATH = "/projects";
  public static final String PROJECT_PATH = PROJECTS_BASE_PATH + ID_PARAM;
  public static final String PROJECT_CATEGORIES_PATH = PROJECTS_BASE_PATH + "/categories";

  public static final String PERSONS_BASE_PATH = "/persons";
  public static final String PERSON_PATH = PERSONS_BASE_PATH + ID_PARAM;
  public static final String PERSON_AFFILIATIONS_PATH =
      PERSONS_BASE_PATH + ID_PARAM + "/affiliations";
  public static final String AFFILIATION_PATH = PERSON_AFFILIATIONS_PATH + "/{eid}";
  public static final String PERSON_INSTITUTION_PATH = PERSON_PATH + "/institutions" + ID_PARAM;
  public static final String PERSON_PICTURE_PATH = PERSONS_BASE_PATH + ID_PARAM + "/picture";
  public static final String PERSON_POSITIONS_PATH = PERSONS_BASE_PATH + "/affiliations/positions";
  public static final String PERSON_ORCID_PATH = PERSONS_BASE_PATH + "/orcid";

  public static final String UNITS_PATH = "/units";
  public static final String UNIT_PATH = UNITS_PATH + ID_PARAM;

  public static final String KEYWORDS_PATH = "/keywords";
  public static final String KEYWORD_PATH = KEYWORDS_PATH + ID_PARAM;

  private CristinPaths() {}
}
