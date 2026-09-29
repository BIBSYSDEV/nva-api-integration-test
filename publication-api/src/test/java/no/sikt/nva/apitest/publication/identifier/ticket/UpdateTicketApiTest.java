package no.sikt.nva.apitest.publication.identifier.ticket;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_SUPPORT_CURATOR;
import no.sikt.nva.apitest.publication.PublicationTestBase;

@ExtendWith (SoftAssertionsExtension.class)
class UpdateTicketApiTest extends PublicationTestBase{

  @Test 
  void shouldUpdateTicket(SoftAssertions softly) {

    var publicationIdentifier = PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    PUBLICATION_TICKET_FACTORY.createTicket(UIB_CREATOR, publicationIdentifier, "GeneralSupportCase");
    var ticketList = PUBLICATION_TICKET_FACTORY.fetchTickets(UIB_SUPPORT_CURATOR, publicationIdentifier);
    var ticket = ticketList.getFirst();

    PUBLICATION_TICKET_FACTORY.updateTicket(UIB_SUPPORT_CURATOR, publicationIdentifier, ticket.identifier(), "Active", "Updated ticket");

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    ticketList = PUBLICATION_TICKET_FACTORY.fetchTickets(UIB_SUPPORT_CURATOR, publicationIdentifier, HTTP_ACCEPTED);
    ticket = ticketList.getFirst();
    softly.assertThat(ticket.status()).isEqualTo("Pending");
    softly.assertThat(ticket.viewStatus()).isEqualTo("Read");
    softly.assertThat(ticket.assignee()).isEqualTo(UIB_SUPPORT_CURATOR.cristinUri());

  }
}
