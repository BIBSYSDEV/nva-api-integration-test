package no.sikt.nva.apitest.publication.identifier.ticket;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import no.sikt.nva.apitest.publication.PublicationTestBase;

@ExtendWith (SoftAssertionsExtension.class)
class FetchTicketApiTest extends PublicationTestBase{

  @Test 
  void shouldFetchTicket(SoftAssertions softly) {

    var publicationIdentifier = PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    PUBLICATION_TICKET_FACTORY.createTicket(UIB_CREATOR, publicationIdentifier, "GeneralSupportCase", HTTP_ACCEPTED);

    var ticketIdentifier = PUBLICATION_TICKET_FACTORY.fetchTickets(UIB_CREATOR, publicationIdentifier).getFirst().identifier();

    var ticket = PUBLICATION_TICKET_FACTORY.fetchTicket(UIB_CREATOR, publicationIdentifier, ticketIdentifier, HTTP_ACCEPTED);
    
    softly.assertThat(ticket.type()).isEqualTo("GeneralSupportCase");
    softly.assertThat(ticket.ownerAffiliation()).isEqualTo(List.of(UIB_CREATOR.affiliations()).getFirst());
  }

}
