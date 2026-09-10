package no.sikt.nva.apitest.project;

import static no.sikt.nva.apitest.project.ProjectContributor.ProjectRole.PARTICIPANT;
import static no.sikt.nva.apitest.project.ProjectContributor.ProjectRole.PROJECT_MANAGER;

import no.sikt.nva.apitest.base.User;

public record ProjectContributor(User user, String role) {
  public enum ProjectRole {
    PARTICIPANT("ProjectParticipant"),
    PROJECT_MANAGER("ProjectManager");

    private final String value;

    ProjectRole(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public static ProjectContributor asProjectManager(User user) {
    return new ProjectContributor(user, PROJECT_MANAGER.getValue());
  }

  public static ProjectContributor asContributor(User user) {
    return new ProjectContributor(user, PARTICIPANT.getValue());
  }
}
