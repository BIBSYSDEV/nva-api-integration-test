package no.sikt.nva.apitest.publication;

import java.util.List;
import no.sikt.Contributor;
import no.sikt.nva.apitest.base.User;

/** A contributor in the entity description of a publication create or update request. */
public record ContributorPayload(
    String type,
    int sequence,
    RoleType role,
    Identity identity,
    List<Organization> affiliations,
    boolean correspondingAuthor) {

  public static ContributorPayload from(Contributor contributor, int sequence) {
    var user = contributor.user();
    return new ContributorPayload(
        "Contributor",
        sequence,
        new RoleType(contributor.role().getValue()),
        Identity.verified(user),
        user.affiliations().stream().map(Organization::withId).toList(),
        false);
  }

  public record RoleType(String type) {}

  public record Identity(String type, String id, String verificationStatus, String name) {

    public static Identity verified(User user) {
      return new Identity("Identity", user.cristinUri(), "Verified", user.name());
    }
  }

  public record Organization(String type, String id) {

    public static Organization withId(String id) {
      return new Organization("Organization", id);
    }
  }
}
