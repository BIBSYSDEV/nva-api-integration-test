package no.sikt.nva.apitest.publication.identifier.ticket;

import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;

import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class AddMessageTest extends PublicationTestBase {

  @Test
  void shouldAddMessageToTicket(SoftAssertions softly) {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, "GeneralSupportCase", "Initial message");

    var messageText = "Follow-up message";
    PUBLICATION_TICKET_FACTORY.addMessageToTicket(
        UIB_CREATOR, publicationIdentifier, ticketIdentifier, messageText);

    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            UIB_CREATOR, publicationIdentifier, ticketIdentifier);

    softly
        .assertThat(
            ticket.messages().stream().anyMatch(message -> message.text().equals(messageText)))
        .isTrue();
  }
}
