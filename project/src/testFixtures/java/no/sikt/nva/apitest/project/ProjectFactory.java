package no.sikt.nva.apitest.project;

import no.sikt.nva.apitest.base.Requests;
import no.sikt.nva.apitest.base.User;

public class ProjectFactory {

  private static final String PROJECT_PATH = "/cristin/project";

  public Project createProject(User user) {

    Requests.givenAuthenticatedJsonRequestAsUser(user);

    return new Project("");
  }
}
