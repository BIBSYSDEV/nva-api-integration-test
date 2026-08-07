package no.sikt.nva.apitest.base;

public final class UserFixtures {
  public static final User UIB_CREATOR =
      User.builder()
          .withName("Creator UiB ApiTestUser")
          .withUserId("api-test-user-creator-uib@test.sikt.no")
          .withCristinId("1862458@184.0.0.0")
          .withAffiliations(Affiliation.UIB)
          .build();

  public static final User UIB_PUBLISHING_CURATOR =
      User.builder()
          .withName("Publishing Curator UiB ApiTestUser")
          .withUserId("api-test-user-publishing-curator-uib@test.sikt.no")
          .withCristinId("1862459@184.0.0.0")
          .withAffiliations(Affiliation.UIB)
          .build();

  public static final User UIB_NVI_CURATOR =
      User.builder()
          .withName("NVI Curator UiB ApiTestUser")
          .withUserId("api-test-user-nvi-curator-uib@test.sikt.no")
          .withCristinId("1862460@184.0.0.0")
          .withAffiliations(Affiliation.UIB)
          .build();

  public static final User UIB_SUPPORT_CURATOR =
      User.builder()
          .withName("Support Curator UiB ApiTestUser")
          .withUserId("api-test-user-support-curator-uib@test.sikt.no")
          .withCristinId("1862461@184.0.0.0")
          .withAffiliations(Affiliation.UIB)
          .build();

  public static final User UIB_DOI_CURATOR =
      User.builder()
          .withName("Doi Curator UiB ApiTestUser")
          .withUserId("api-test-user-doi-curator-uib@test.sikt.no")
          .withCristinId("1862462@184.0.0.0")
          .withAffiliations(Affiliation.UIB)
          .build();

  public static final User UIB_EDITOR =
      User.builder()
          .withName("Editor UiB ApiTestUser")
          .withUserId("api-test-user-editor-uib@test.sikt.no")
          .withCristinId("1862463@184.0.0.0")
          .withAffiliations(Affiliation.UIB)
          .build();

  public static final User UIB_CONTRIBUTOR =
      User.builder()
          .withName("Contributor UiB ApiTestUser")
          .withUserId("api-test-user-contributor-uib@test.sikt.no")
          .withCristinId("1862464@184.0.0.0")
          .withAffiliations(Affiliation.UIB)
          .build();

  public static final User UIB_THESIS_CURATOR =
      User.builder()
          .withName("Curator-thesis UiB ApiTestUser")
          .withUserId("api-test-user-curator-thesis-uib@test.sikt.no")
          .withCristinId("1862466@184.0.0.0")
          .withAffiliations(Affiliation.UIB)
          .build();

  public static final User KRISTIANIA_CREATOR =
      User.builder()
          .withName("Registrator Kristiania ApiTestUser")
          .withUserId("api-test-user-registrator-kristiania@test.sikt.no")
          .withCristinId("1862476@1615.0.0.0")
          .withAffiliations(Affiliation.KRISTIANIA)
          .build();

  public static final User KRISTIANIA_PUBLISHING_CURATOR =
      User.builder()
          .withName("Publishing Curator Kristiania ApiTestUser")
          .withUserId("api-test-user-publishing-curator-kristiania@test.sikt.no")
          .withCristinId("1862477@1615.0.0.0")
          .withAffiliations(Affiliation.KRISTIANIA)
          .build();

  public static final User KRISTIANIA_NVI_CURATOR =
      User.builder()
          .withName("Nvi-curator Kristiania ApiTestUser")
          .withUserId("api-test-user-nvi-curator-kristiania@test.sikt.no")
          .withCristinId("1862478@1615.0.0.0")
          .withAffiliations(Affiliation.KRISTIANIA)
          .build();

  public static final User KRISTIANIA_EDITOR =
      User.builder()
          .withName("Editor Kristiania ApiTestUser")
          .withUserId("api-test-user-editor-kristiania@test.sikt.no")
          .withCristinId("1862479@1615.0.0.0")
          .withAffiliations(Affiliation.KRISTIANIA)
          .build();

  public static final User OSLO_MET_CREATOR =
      User.builder()
          .withName("Registrator OsloMet ApiTestUser")
          .withUserId("api-test-user-registrator-oslomet@test.sikt.no")
          .withCristinId("1862480@215.0.0.0")
          .withAffiliations(Affiliation.OSLO_MET)
          .build();

  public static final User OSLO_MET_PUBLISHING_CURATOR =
      User.builder()
          .withName("Publishing curator OsloMet ApiTestUser")
          .withUserId("api-test-user-publishing-curator-oslomet@test.sikt.no")
          .withCristinId("1862481@215.0.0.0")
          .withAffiliations(Affiliation.OSLO_MET)
          .build();

  public static final User OSLO_MET_NVI_CURATOR =
      User.builder()
          .withName("Nvi-curator OsloMet ApiTestUser")
          .withUserId("api-test-user-nvi-curator-oslomet@test.sikt.no")
          .withCristinId("1862482@215.0.0.0")
          .withAffiliations(Affiliation.OSLO_MET)
          .build();

  public static final User OSLO_MET_EDITOR =
      User.builder()
          .withName("Editor OsloMet ApiTestUser")
          .withUserId("api-test-user-editor-oslomet@test.sikt.no")
          .withCristinId("1862483@215.0.0.0")
          .withAffiliations(Affiliation.OSLO_MET)
          .build();

  public static final User UIS_CREATOR =
      User.builder()
          .withName("Registrator UiS ApiTestUser")
          .withUserId("api-test-user-registrator-uis@test.sikt.no")
          .withCristinId("1862472@217.0.0.0")
          .withAffiliations(Affiliation.UIS)
          .build();

  public static final User UIS_NVI_CURATOR =
      User.builder()
          .withName("Nvi-curator UiS ApiTestUser")
          .withUserId("api-test-user-nvi-curator-uis@test.sikt.no")
          .withCristinId("1862474@217.0.0.0")
          .withAffiliations(Affiliation.UIS)
          .build();

  public static final User UIS_PUBLISHING_CURATOR =
      User.builder()
          .withName("Publishing Curator UiS ApiTestUser")
          .withUserId("api-test-user-publishing-curator-uis@test.sikt.no")
          .withCristinId("1862473@217.0.0.0")
          .withAffiliations(Affiliation.UIS)
          .build();

  public static final User UIS_EDITOR =
      User.builder()
          .withName("Editor UiS ApiTestUser")
          .withUserId("api-test-user-editor-uis@test.sikt.no")
          .withCristinId("1862475@217.0.0.0")
          .withAffiliations(Affiliation.UIS)
          .build();

  private UserFixtures() {}
}
