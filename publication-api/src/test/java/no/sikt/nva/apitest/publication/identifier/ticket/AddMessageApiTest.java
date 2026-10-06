package no.sikt.nva.apitest.publication.identifier.ticket;

import static io.restassured.http.Method.POST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.PublicationTicketFactory.TICKET_PATH;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;

import io.qameta.allure.Description;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST /publication/{publicationIdentifier}/ticket/{ticketIdentifier}/message")
class AddMessageApiTest extends PublicationTestBase {

  /** Add message to ticket */
  @Test
  @DisplayName("Add message to ticket on Publication")
  @Description
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

  /** Trying to add a message to a non-existing ticket should return {@code 404 Not Found} */
  @Test
  @DisplayName("Trying to add a message to a non-existing ticket should return Not Found")
  void shouldReturnNotFoundWhenAddingMessageToNonExistingTicket() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var ticketIdentifier = UUID.randomUUID().toString();

    var messageText = "Follow-up message";
    PUBLICATION_TICKET_FACTORY.addMessageToTicket(
        UIB_CREATOR, publicationIdentifier, ticketIdentifier, messageText, HTTP_NOT_FOUND);
  }

  /** Trying to add a message to a ticket when not authenticated return {@code 401 Unauthorized} */
  @Test
  @DisplayName(
      "Trying to add a message to a ticket when not authenticated should return Unauthorized")
  void shouldReturnUnauthorizedWhenNotAuthenticated() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, "GeneralSupportCase", "Initial message");

    Map<String, Object> payload = Map.of("text", "Followup message");

    requestWithPayloadShouldReturnUnauthorized(
        POST, TICKET_PATH + "/message", payload, publicationIdentifier, ticketIdentifier);
  }

  /** Trying to add a message to a ticket when not owner return {@code 403 Forbidden} */
  @Test
  @Disabled("FIXME: Returns 401. See NP-52023")
  @DisplayName("Trying to add a message to a ticket when not owner should return Forbidden")
  void shouldReturnForbiddenWhenNotOwner() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, "GeneralSupportCase", "Initial message");

    Map<String, Object> payload = Map.of("text", "Followup message");

    requestWitPayloadShouldReturnForbidden(
        POST,
        UIB_CONTRIBUTOR,
        TICKET_PATH + "/message",
        payload,
        publicationIdentifier,
        ticketIdentifier);
  }
}
