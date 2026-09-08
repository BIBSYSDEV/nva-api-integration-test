package no.sikt.nva.apitest.project;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.base.Requests;
import no.sikt.nva.apitest.base.User;

public class ProjectFactory {

  private static final String PROJECT_PATH = "/cristin/project";

  public Project createProject(User user) {

    var projectTitle = "Cristin API test project " + UUID.randomUUID();
    var payload = createProjectPayload(projectTitle, Affiliation.UIB, List.of());

    Requests.givenAuthenticatedJsonRequestAsUser(user).body(payload).when().post(PROJECT_PATH);
    return new Project("");
  }

  private Map<String, Object> createProjectPayload(
      String projectTitle,
      Affiliation coordinatingInstitution,
      List<ProjectContributor> contributors) {

    var contributorsPayload =
        contributors.stream()
            .map(
                contributor ->
                    Map.of(
                        "identity",
                        Map.of("type", "Person", "id", contributor.user().cristinId()),
                        "roles",
                        List.of(
                            Map.of(
                                "type",
                                contributor.role(),
                                "affiliation",
                                Map.of(
                                    "type",
                                    "Organization",
                                    "id",
                                    contributor.user().affiliations().iterator().next())))))
            .collect(Collectors.toList());

    return Map.of(
        "title",
        projectTitle,
        "coordinatingInstitution",
        Map.of("type", "Organization", "id", coordinatingInstitution),
        "contributors",
        contributorsPayload);
  }
}
