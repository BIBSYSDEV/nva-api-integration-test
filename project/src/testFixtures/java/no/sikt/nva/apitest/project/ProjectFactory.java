package no.sikt.nva.apitest.project;

import static java.net.HttpURLConnection.HTTP_CREATED;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.restassured.path.json.JsonPath;
import no.sikt.nva.apitest.base.Affiliation;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import no.sikt.nva.apitest.base.CurrentTimeConstants;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import no.sikt.nva.apitest.base.User;

public class ProjectFactory {

  public static final String BASE_PROJECT_PATH = "/cristin/project";
  public static final String PROJECT_PATH = BASE_PROJECT_PATH + "/{identifier}";
  public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("Europe/Oslo");

  private static final String TYPE = "type";

  public Project createProject(User user, String projectTitle) {

    var payload =
        createProjectPayload(projectTitle, UIB, List.of(ProjectContributor.asProjectManager(user)));

    JsonPath jsonPath =
        givenAuthenticatedJsonRequestAsUser(user)
            .body(payload)
            .when()
            .post(BASE_PROJECT_PATH)
            .then()
            .statusCode(HTTP_CREATED)
            .extract()
            .jsonPath();
    return new Project(List.of(jsonPath.getString("id").split("/")).getLast());
  }

  public static Map<String, Object> createProjectPayload(
      String projectTitle,
      Affiliation coordinatingInstitution,
      List<ProjectContributor> contributors) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    var startDateTime = CurrentTimeConstants.currentDateTime().format(formatter);
    var endDateTime = CurrentTimeConstants.currentDateTime().plusYears(1).format(formatter);

    var contributorsPayload =
        contributors.stream()
            .map(ProjectFactory::createContributorPayload)
            .collect(Collectors.toList());

    return Map.of(
        "title",
        projectTitle,
        "coordinatingInstitution",
        Map.of(
            TYPE,
            "Organization",
            "id",
            "https://api.e2e.nva.aws.unit.no/cristin/organization/"
                + coordinatingInstitution.getCristinId()),
        "contributors",
        contributorsPayload,
        "startDate",
        startDateTime,
        "endDate",
        endDateTime);
  }

  private static Map<String, Object> createContributorPayload(ProjectContributor contributor) {
    String cristinId = contributor.user().cristinId();
    String id =
        cristinId.contains("@") ? cristinId.substring(0, cristinId.indexOf('@')) : cristinId;
    return Map.of(
        "identity",
        Map.of(TYPE, "Person", "id", id),
        "roles",
        List.of(createRolePayload(contributor)));
  }

  private static Map<String, String> createAffiliationPayload(User user) {
    return Map.of(TYPE, "Organization", "id", user.affiliations().iterator().next());
  }

  private static Map<String, Object> createRolePayload(ProjectContributor contributor) {
    return Map.of(
        TYPE, contributor.role(), "affiliation", createAffiliationPayload(contributor.user()));
  }
}
