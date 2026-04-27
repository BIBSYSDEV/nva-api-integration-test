package no.sikt;

import java.util.Collection;
import java.util.List;

public record User(String name, String userId, String cristinId, Collection<String> affiliations) {

  public User {
    affiliations = List.copyOf(affiliations);
  }

  public User(String name, String userId, String cristinId, String... affiliations) {
    this(name, userId, cristinId, List.of(affiliations));
  }

  public Builder builder() {
    return new Builder();
  }

  public Builder copy() {
    return builder()
        .withName(name)
        .withUserId(userId)
        .withCristinId(cristinId)
        .withAffiliations(affiliations);
  }

  public static final class Builder {
    private String name;
    private String userId;
    private String cristinId;
    private Collection<String> affiliations;

    private Builder() {}

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withUserId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder withCristinId(String cristinId) {
      this.cristinId = cristinId;
      return this;
    }

    public Builder withAffiliations(Collection<String> affiliations) {
      this.affiliations = affiliations;
      return this;
    }

    public User build() {
      return new User(name, userId, cristinId, affiliations);
    }
  }
}
