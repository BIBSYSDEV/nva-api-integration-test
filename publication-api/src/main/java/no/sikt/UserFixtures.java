package no.sikt;

public final class UserFixtures {
  public static final User UIB_CREATOR =
      new User(
          "Creator UiB ApiTestUser",
          "api-test-user-creator-uib@test.sikt.no",
          "1862458@184.0.0.0",
          Affiliation.UIB.value);

  public static final User UIB_PUBLISHING_CURATOR =
      UIB_CREATOR
          .copy()
          .withName("Publishing Curator UiB ApiTestUser")
          .withCristinId("api-test-user-publishing-curator-uib@test.sikt.no")
          .build();

  public static final User UIB_NVI_CURATOR =
      UIB_CREATOR
          .copy()
          .withName("NVI Curator UiB ApiTestUser")
          .withCristinId("api-test-user-nvi-curator-uib@test.sikt.no")
          .build();

  public static final User UIB_SUPPORT_CURATOR =
      UIB_CREATOR
          .copy()
          .withName("Support Curator UiB ApiTestUser")
          .withCristinId("api-test-user-support-curator-uib@test.sikt.no")
          .build();

  public static final User UIB_DOI_CURATOR =
      UIB_CREATOR
          .copy()
          .withName("Doi Curator UiB ApiTestUser")
          .withCristinId("api-test-user-doi-curator-uib@test.sikt.no")
          .build();

  public static final User UIB_EDITOR =
      UIB_CREATOR
          .copy()
          .withName("Editor UiB ApiTestUser")
          .withCristinId("api-test-user-editor-uib@test.sikt.no")
          .build();

  private UserFixtures() {}
}
