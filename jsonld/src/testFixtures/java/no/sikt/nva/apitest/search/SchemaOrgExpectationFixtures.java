package no.sikt.nva.apitest.search;

public final class SchemaOrgExpectationFixtures {

  public static final SchemaOrgExpectation EXPECTED_SCHEMA_ORG_ACADEMIC_ARTICLE =
      new SchemaOrgExpectation("ScholarlyArticle");
  public static final SchemaOrgExpectation EXPECTED_SCHEMA_ORG_ACADEMIC_MONOGRAPH =
      new SchemaOrgExpectation("Book");
  public static final SchemaOrgExpectation EXPECTED_SCHEMA_ORG_ACADEMIC_CHAPTER =
      new SchemaOrgExpectation("Chapter");
  public static final SchemaOrgExpectation EXPECTED_SCHEMA_ORG_DEGREE_MASTER =
      new SchemaOrgExpectation("Thesis");
  public static final SchemaOrgExpectation EXPECTED_SCHEMA_ORG_DEGREE_PHD =
      new SchemaOrgExpectation("Thesis");
  public static final SchemaOrgExpectation EXPECTED_SCHEMA_ORG_REPORT_RESEARCH =
      new SchemaOrgExpectation("Report");
  public static final SchemaOrgExpectation EXPECTED_SCHEMA_ORG_CONFERENCE_LECTURE =
      new SchemaOrgExpectation("PresentationDigitalDocument");

  private SchemaOrgExpectationFixtures() {}
}
