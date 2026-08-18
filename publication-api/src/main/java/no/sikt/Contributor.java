package no.sikt;

import no.sikt.nva.apitest.base.User;

public record Contributor(User user, Role role) {

  public static Contributor asCreator(User user) {
    return new Contributor(user, Role.CREATOR);
  }
}
