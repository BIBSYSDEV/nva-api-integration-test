package no.sikt.nva.apitest.publication.identifier.ticket;

import static io.restassured.http.Method.DELETE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.PublicationTicketFactory.GENERAL_SUPPORT_CASE;
import static no.sikt.nva.PublicationTicketFactory.STATUS_DELETED;
import static no.sikt.nva.PublicationTicketFactory.TICKET_MESSAGE_PATH;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;

import io.qameta.allure.Description;
import java.util.UUID;
import java.util.stream.Collectors;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName(
    "DELETE"
        + " /publication/{publicationIdentifier}/ticket/{ticketIdentifier}/message/{messageIdentifier}")
class DeleteMessageApiTest extends PublicationTestBase {

  private static final String INITIAL_MESSAGE = "Initial message";

  /** Delete message from ticket return {@code 200 Ok} */
  @Test
  @DisplayName("Delete message from ticket returns Ok")
  @Description
  void shouldDeleteMessageFromTicket(SoftAssertions softly) {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE, INITIAL_MESSAGE);

    String messageText = "Followup message";
    PUBLICATION_TICKET_FACTORY.addMessageToTicket(
        UIB_CREATOR, publicationIdentifier, ticketIdentifier, messageText);

    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            UIB_CREATOR, publicationIdentifier, ticketIdentifier);
    var messageList =
        ticket.messages().stream()
            .filter(message -> message.text().equals(messageText))
            .collect(Collectors.toList());

    softly.assertThat(messageList.size()).isEqualTo(1);
    var messageIdentifier = messageList.getFirst().identifier();

    PUBLICATION_TICKET_FACTORY.deleteMessage(
        UIB_CREATOR, publicationIdentifier, ticketIdentifier, messageIdentifier, HTTP_OK);
    var updatedTicket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            UIB_CREATOR, publicationIdentifier, ticketIdentifier);

    softly
        .assertThat(
            updatedTicket.messages().stream()
                .filter(message -> message.identifier().equals(messageIdentifier))
                .collect(Collectors.toList())
                .getFirst()
                .status())
        .isEqualTo(STATUS_DELETED);

    var updatedMessageList =
        updatedTicket.messages().stream()
            .filter(message -> message.identifier().equals(messageIdentifier))
            .collect(Collectors.toList());
    softly.assertThat(updatedMessageList.getFirst().text()).isNull();
  }

  /** Delete non-existing message return {@code 404 Not Found} */
  @Test
  @DisplayName("Trying to delete non-existing message returns Not Found")
  @Description
  void shouldReturnNotFoundWhenTicketNotExisting() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE, INITIAL_MESSAGE);

    var messageIdentifier = UUID.randomUUID().toString();

    PUBLICATION_TICKET_FACTORY.deleteMessage(
        UIB_CREATOR, publicationIdentifier, ticketIdentifier, messageIdentifier, HTTP_NOT_FOUND);
  }

  /**
   * Trying to delete a message from a ticket when unauthenticated returns {@code 401 Unauthorized}
   */
  @Test
  @DisplayName("Trying to delete a message from a ticket when unauthenticated return Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhenNotAuthenticated() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE, INITIAL_MESSAGE);

    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            UIB_CREATOR, publicationIdentifier, ticketIdentifier);
    var messageIdentifier = ticket.messages().getFirst().identifier();

    requestShouldReturnUnauthorized(
        DELETE, TICKET_MESSAGE_PATH, publicationIdentifier, ticketIdentifier, messageIdentifier);
  }

  /** Trying to delete a message from a ticket when not owner returns {@code 403 Forbidden} */
  @Test
  @Disabled("FIXME: Returns 401. See NP-52023")
  @DisplayName("Trying to delete a message from a ticket when not owner return Forbidden")
  @Description
  void shouldReturnForbiddenWhenNotowner() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE, INITIAL_MESSAGE);

    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            UIB_CREATOR, publicationIdentifier, ticketIdentifier);
    var messageIdentifier = ticket.messages().getFirst().identifier();

    requestShouldReturnForbidden(
        DELETE,
        UIB_CONTRIBUTOR,
        TICKET_MESSAGE_PATH,
        publicationIdentifier,
        ticketIdentifier,
        messageIdentifier);
  }
}
