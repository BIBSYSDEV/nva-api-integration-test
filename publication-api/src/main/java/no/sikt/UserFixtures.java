package no.sikt;

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

  private UserFixtures() {}
}
