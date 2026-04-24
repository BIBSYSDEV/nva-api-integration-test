package no.sikt;

public enum TestUser {
  UIB_CREATOR(
      "Creator UiB ApiTestUser",
      "api-test-user-creator-uib@test.sikt.no",
      "1862458@184.0.0.0",
      Affiliation.UIB.value),
  UIB_PUBLISHING_CURATOR(
      "Publishing Curator UiB ApiTestUser",
      "api-test-user-publishing-curator-uib@test.sikt.no",
      "1862459@184.0.0.0",
      Affiliation.UIB.value),
  UIB_NVI_CURATOR(
      "NVI Curator UiB ApiTestUser",
      "api-test-user-nvi-curator-uib@test.sikt.no",
      "1862460@184.0.0.0",
      Affiliation.UIB.value),
  UIB_SUPPORT_CURATOR(
      "Support Curator UiB ApiTestUser",
      "api-test-user-support-curator-uib@test.sikt.no",
      "1862461@184.0.0.0",
      Affiliation.UIB.value),
  UIB_DOI_CURATOR(
      "Doi Curator UiB ApiTestUser",
      "api-test-user-doi-curator-uib@test.sikt.no",
      "1862462@184.0.0.0",
      Affiliation.UIB.value),
  UIB_EDITOR(
      "Editor UiB ApiTestUser",
      "api-test-user-editor-uib@test.sikt.no",
      "1862463@184.0.0.0",
      Affiliation.UIB.value);

  public final String userId;
  public final String cristinId;
  public final String[] affiliations;
  public final String cristinIdentifier;
  public final String name;

  TestUser(String name, String userId, String cristinId, String... affiliations) {
    this.name = name;
    this.userId = userId;
    this.cristinId = cristinId;
    this.affiliations = affiliations;
    cristinIdentifier = cristinId.split("@", -1)[0];
  }
}
