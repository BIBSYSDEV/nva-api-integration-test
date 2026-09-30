package no.sikt.nva.apitest.publication.identifier.ticket;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.http.Method.PUT;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.PublicationTicketFactory.TICKET_PATH;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_SUPPORT_CURATOR;
import no.sikt.nva.apitest.publication.PublicationTestBase;

@ExtendWith (SoftAssertionsExtension.class)
class UpdateTicketApiTest extends PublicationTestBase{

  @Test 
  void shouldUpdateTicket(SoftAssertions softly) {

    var publicationIdentifier = PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    var ticketIdentifier = PUBLICATION_TICKET_FACTORY.createTicket(UIB_CREATOR, publicationIdentifier, "GeneralSupportCase");

    var requestBody = Map.of("assignee", UIB_SUPPORT_CURATOR.cristinId());
    PUBLICATION_TICKET_FACTORY.updateTicket(UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, requestBody);

    var ticket = PUBLICATION_TICKET_FACTORY.fetchTicket(UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier);

    softly.assertThat(ticket.status()).isEqualTo("Pending");

    requestBody = Map.of("viewStatus", "Read");
    PUBLICATION_TICKET_FACTORY.updateTicket(UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, requestBody);

    ticket = PUBLICATION_TICKET_FACTORY.fetchTicket(UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier);
    softly.assertThat(ticket.viewedBy()).contains(UIB_SUPPORT_CURATOR.cristinId());

    requestBody = Map.of("status", "Completed");
    PUBLICATION_TICKET_FACTORY.updateTicket(UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, requestBody);

    ticket = PUBLICATION_TICKET_FACTORY.fetchTicket(UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier);
    softly.assertThat(ticket.status()).isEqualTo("Completed");
  }

  @Test 
  void shouldReturnNotFoundWhenNonExistingTicket() {

    var publicationIdentifier = PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    var ticketIdentifier = UUID.randomUUID().toString();

    var requestBody = Map.of("assignee", UIB_SUPPORT_CURATOR.cristinId());
    PUBLICATION_TICKET_FACTORY.updateTicket(UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, requestBody, HTTP_NOT_FOUND);
  }

  @Test 
  void shouldReturnUnauthorizedWhenNotAuthenticated() {
    var publicationIdentifier = PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var ticketIdentifier = PUBLICATION_TICKET_FACTORY.createTicket(UIB_CREATOR, publicationIdentifier, "GeneralSupportCase");

    requestShouldReturnUnauthorized(PUT, TICKET_PATH, publicationIdentifier, ticketIdentifier);
  }

  @Test 
  void shouldReturnForbiddenWhenNotOwner() {
    var publicationIdentifier = PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var ticketIdentifier = PUBLICATION_TICKET_FACTORY.createTicket(UIB_CREATOR, publicationIdentifier, "GeneralSupportCase");
    requestShouldReturnForbidden(PUT, UIB_CONTRIBUTOR, TICKET_PATH, publicationIdentifier, ticketIdentifier);
  }
}
