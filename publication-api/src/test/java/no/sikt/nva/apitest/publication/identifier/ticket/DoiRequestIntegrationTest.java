package no.sikt.nva.apitest.publication.identifier.ticket;

import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.PublicationTicketFactory.DOI_REQUEST;
import no.sikt.nva.apitest.base.User;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_DOI_CURATOR;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import no.sikt.nva.apitest.publication.ticket.Ticket;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("DOI request workflow")
class DoiRequestIntegrationTest extends PublicationTestBase {

  /** DOI curator receives a DOI-request ticket */
  @Test
  @DisplayName("A DOI-request is sent to DOI curator")
  @Description
  void shouldSendDoiRequestTicketToDoiCurator(SoftAssertions softly) {
    var publicationIdentifier = createPublishedPublication();
    var ticketIdentifier = reserveDoi(UIB_CREATOR, publicationIdentifier);
    var doiRequestTicket =
        findDoiApprovalTicket(
            UIB_DOI_CURATOR, UIB_CREATOR, publicationIdentifier, ticketIdentifier);

    softly.assertThat(doiRequestTicket.identifier()).isEqualTo(ticketIdentifier);
    softly.assertThat(doiRequestTicket.type()).isEqualTo(DOI_REQUEST);
  }

  /** DOI curator is assigned to a DOI-request */
  @Test
  @DisplayName("A DOI-curator is assigned to a DOI-request")
  @Description
  void shouldBeAssignedToADoiRequest(SoftAssertions softly) {
    var publicationIdentifier = createPublishedPublication();
    var ticketIdentifier = reserveDoi(UIB_CREATOR, publicationIdentifier);

    var doiRequestTicket =
        readAndAssignDoiRequest(UIB_DOI_CURATOR, publicationIdentifier, ticketIdentifier);
    assertDoiRequestIsAssignedAndRead(doiRequestTicket, UIB_DOI_CURATOR, softly);
  }

  /** DOI curator approves DOI-request */
  @Test
  @DisplayName("A DOI-curator approves a DOI-request")
  @Description
  void shouldApproveDoiRequest(SoftAssertions softly) {
    var publicationIdentifier = createPublishedPublication();
    var ticketIdentifier = reserveDoi(UIB_CREATOR, publicationIdentifier);

    var ticket = approveDoiRequest(UIB_DOI_CURATOR, publicationIdentifier, ticketIdentifier);
    assertDoiRequestApproved(ticket, UIB_DOI_CURATOR, softly);
  }

  /** DOI curator rejects DOI-request */
  @Test
  @DisplayName("A DOI-curator rejects a DOI-request")
  @Description
  void shouldRejectDoiRequest(SoftAssertions softly) {
    var publicationIdentifier = createPublishedPublication();
    var ticketIdentifier = reserveDoi(UIB_CREATOR, publicationIdentifier);

    var ticket = rejectDoiRequest(UIB_DOI_CURATOR, publicationIdentifier, ticketIdentifier);
    assertDoiRequestClosed(ticket, UIB_DOI_CURATOR, softly);
  }

  @Step("Given a published publication")
  private static String createPublishedPublication() {
    return PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
  }

  @Step("When the Creator request a DOI for the publication")
  private static String reserveDoi(User user, String publicationIdentifier) {

    return PUBLICATION_TICKET_FACTORY.createTicket(
        user, publicationIdentifier, DOI_REQUEST, "doirequest");
  }

  @Step("Then the DOI-curator find a ticket with for DOI approval from Creator")
  private static Ticket findDoiApprovalTicket(
      User doiCurator, User creator, String publicationIdentifier, String ticketIdentifier) {

    return PUBLICATION_TICKET_FACTORY.fetchTickets(doiCurator, publicationIdentifier).stream()
        .filter(ticket -> DOI_REQUEST.equals(ticket.type()))
        .filter(ticket -> ticket.owner().equals(creator.cristinId()))
        .filter(ticket -> ticket.identifier().equals(ticketIdentifier))
        .toList()
        .getFirst();
  }

  @Step("When the DOI curator reads a DOI-request")
  private static Ticket readAndAssignDoiRequest(
      User doiCurator, String publicationIdentifier, String ticketIdentifier) {

    var requestBody = Map.of("viewStatus", "Read");
    PUBLICATION_TICKET_FACTORY.updateTicket(
        doiCurator, publicationIdentifier, ticketIdentifier, requestBody);

    requestBody = Map.of("assignee", doiCurator.cristinId());
    PUBLICATION_TICKET_FACTORY.updateTicket(
        doiCurator, publicationIdentifier, ticketIdentifier, requestBody);

    return PUBLICATION_TICKET_FACTORY.fetchTicket(
        doiCurator, publicationIdentifier, ticketIdentifier);
  }

  @Step("Then the DOI-curator is assigned to the DOI-request and the ticket is status 'Read'")
  private static void assertDoiRequestIsAssignedAndRead(
      Ticket ticket, User doiCurator, SoftAssertions softly) {
    softly.assertThat(ticket.viewedBy()).contains(doiCurator.cristinId());
    softly.assertThat(ticket.assignee()).isEqualTo(doiCurator.cristinId());
  }

  @Step("When the DOI curator approves a DOI-request")
  private static Ticket approveDoiRequest(
      User doiCurator, String publicationIdentifier, String ticketIdentifier) {

    var requestBody = Map.of("assignee", doiCurator.cristinId());
    PUBLICATION_TICKET_FACTORY.updateTicket(
        doiCurator, publicationIdentifier, ticketIdentifier, requestBody);

    requestBody = Map.of("status", "Completed");
    PUBLICATION_TICKET_FACTORY.updateTicket(
        doiCurator, publicationIdentifier, ticketIdentifier, requestBody);

    return PUBLICATION_TICKET_FACTORY.fetchTicket(
        doiCurator, publicationIdentifier, ticketIdentifier);
  }

  @Step("Then the DOI-request is approved")
  private static void assertDoiRequestApproved(
      Ticket ticket, User doiCurator, SoftAssertions softly) {
    softly.assertThat(ticket.status()).isEqualTo("Completed");
    softly.assertThat(ticket.assignee()).isEqualTo(doiCurator.cristinId());
  }

  @Step("When the DOI-curator rejects a DOI request")
  private static Ticket rejectDoiRequest(
      User doiCurator, String publicationIdentifier, String ticketIdentifier) {
    var requestBody = Map.of("status", "Rejected");
    PUBLICATION_TICKET_FACTORY.updateTicket(
        doiCurator, publicationIdentifier, ticketIdentifier, requestBody);

    return PUBLICATION_TICKET_FACTORY.fetchTicket(
        doiCurator, publicationIdentifier, ticketIdentifier);
  }

  @Step("Then the DOI-request is closed")
  private static void assertDoiRequestClosed(
      Ticket ticket, User doiCurator, SoftAssertions softly) {

    softly.assertThat(ticket.status()).isEqualTo("Closed");
    softly.assertThat(ticket.assignee()).isEqualTo(doiCurator.cristinId());
    softly.assertThat(ticket.finalizedBy()).isEqualTo(doiCurator.cristinId());
  }
}
