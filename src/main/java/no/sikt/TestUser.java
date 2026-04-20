package no.sikt;

import java.util.List;

public enum TestUser {
    UIB_CREATOR(
        "Creator UiB ApiTestUser",
        "api-test-user-creator-uib@test.sikt.no",
        "1862458@184.0.0.0",
        List.of("https://api.e2e.nva.aws.unit.no/cristin/organization/184.0.0.0")
    ),
    UIB_PUBLISHING_CURATOR(
        "Publishing Curator UiB ApiTestUser",
        "api-test-user-publishing-curator-uib@test.sikt.no",
        "1862459@184.0.0.0",
        List.of("https://api.e2e.nva.aws.unit.no/cristin/organization/184.0.0.0")
    ),
    UIB_NVI_CURATOR(
        "NVI Curator UiB ApiTestUser",
        "api-test-user-nvi-curator-uib@test.sikt.no",
        "1862460@184.0.0.0",
        List.of("https://api.e2e.nva.aws.unit.no/cristin/organization/184.0.0.0")
    ),
    UIB_SUPPORT_CURATOR(
        "Support Curator UiB ApiTestUser",
        "api-test-user-support-curator-uib@test.sikt.no",
        "1862461@184.0.0.0",
        List.of("https://api.e2e.nva.aws.unit.no/cristin/organization/184.0.0.0")
    ),
    UIB_DOI_CURATOR(
        "Doi Curator UiB ApiTestUser",
        "api-test-user-doi-curator-uib@test.sikt.no",
        "1862462@184.0.0.0",
        List.of("https://api.e2e.nva.aws.unit.no/cristin/organization/184.0.0.0")
    ),
    UIB_EDITOR(
        "Editor UiB ApiTestUser",
        "api-test-user-editor-uib@test.sikt.no",
        "1862463@184.0.0.0",
        List.of("https://api.e2e.nva.aws.unit.no/cristin/organization/184.0.0.0")
    );

    public final String userId;
    public final String cristinId;
    public final List<String> affiliations;
    public final String cristinIdentifier;
    public final String name;

    private TestUser(String name, String userId, String cristinId, List<String> affiliations) {
        this.name = name;
        this.userId = userId;
        this.cristinId = cristinId;
        this.affiliations = affiliations;
        cristinIdentifier = cristinId.split("@")[0];
    }
}