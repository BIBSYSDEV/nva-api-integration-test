package no.sikt.nva.apitest.publication.ticket;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Set;
import no.sikt.nva.apitest.base.Affiliation;

/**
 * A ticket as the tickets endpoint returns it, covering the fields the API tests assert on. The
 * endpoint returns every kind of ticket in one collection, so the type discriminator is kept to
 * tell file approval tickets from doi requests and support requests.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Ticket(
    String type,
    String status,
    String identifier,
    String ownerAffiliation,
    String publicationIdentifier,
    List<Message> messages,
    List<String> viewedBy,
    String owner,
    String viewStatus,
    String assignee,
    List<TicketFile> filesForApproval) {

  private static final Set<String> FILE_APPROVAL_TYPES =
      Set.of("PublishingRequest", "FilesApprovalThesis");

  /**
   * Only a file approval ticket carries files, and a ticket that carries none serializes without
   * the field rather than with an empty array.
   */
  public Ticket {
    filesForApproval = copyOrEmpty(filesForApproval);
    approvedFiles = copyOrEmpty(approvedFiles);
  }

  private static List<TicketFile> copyOrEmpty(List<TicketFile> files) {
    return isNull(files) ? emptyList() : List.copyOf(files);
  }

  public boolean isFileApproval() {
    return FILE_APPROVAL_TYPES.contains(type);
  }

  public boolean isOwnedBy(Affiliation institution) {
    return institution.getValue().equals(ownerAffiliation);
  }

  public boolean isOwnedBy(String institution) {
    return institution.equals(ownerAffiliation);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TicketFile(String identifier, String name) {}
}
