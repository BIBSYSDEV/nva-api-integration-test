package no.sikt.nva.apitest.publication.identifier.ticket;

import static io.restassured.http.Method.PUT;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.PublicationTicketFactory.GENERAL_SUPPORT_CASE;
import static no.sikt.nva.PublicationTicketFactory.TICKET_PATH;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_SUPPORT_CURATOR;

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
@DisplayName("PUT /publication/{publicationIdentifier}/ticket/{ticketIdentifier}")
class UpdateTicketApiTest extends PublicationTestBase {

  /** Update an existing ticket return {@code 202 Accepted} */
  @Test
  @DisplayName("Update existing ticket return Accepted")
  @Description
  void shouldUpdateTicket(SoftAssertions softly) {

    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE);

    var requestBody = Map.of("assignee", UIB_SUPPORT_CURATOR.cristinId());
    PUBLICATION_TICKET_FACTORY.updateTicket(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, requestBody, HTTP_ACCEPTED);

    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier);

    softly.assertThat(ticket.status()).isEqualTo("Pending");
    softly.assertThat(ticket.assignee()).isEqualTo(UIB_SUPPORT_CURATOR.cristinId());

    requestBody = Map.of("viewStatus", "Read");
    PUBLICATION_TICKET_FACTORY.updateTicket(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, requestBody, HTTP_ACCEPTED);

    ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier);
    softly.assertThat(ticket.viewedBy()).contains(UIB_SUPPORT_CURATOR.cristinId());

    requestBody = Map.of("status", "Completed");
    PUBLICATION_TICKET_FACTORY.updateTicket(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, requestBody, HTTP_ACCEPTED);

    ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier);
    softly.assertThat(ticket.status()).isEqualTo("Completed");
  }

  /** Trying to update a non-existing ticket returns {@code 404 Not Found} */
  @Test
  @Disabled("FIXME: Returns 403, enable when bug is fixed. See NP-52023")
  @DisplayName("Update non-existing ticket returns Not Found")
  @Description
  void shouldReturnNotFoundWhenNonExistingTicket() {

    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    var ticketIdentifier = UUID.randomUUID().toString();

    var requestBody = Map.of("assignee", UIB_SUPPORT_CURATOR.cristinId());
    PUBLICATION_TICKET_FACTORY.updateTicket(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, requestBody, HTTP_NOT_FOUND);
  }

  /** Trying to update a ticket when not authenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Update ticket when not authenticated returns Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhenNotAuthenticated() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE);

    requestShouldReturnUnauthorized(PUT, TICKET_PATH, publicationIdentifier, ticketIdentifier);
  }

  /** Trying to update a ticket when not owner returns {@code 403 Forbidden} */
  @Test
  @DisplayName("Update ticket when not owner returns Forbidden")
  @Description
  void shouldReturnForbiddenWhenNotOwner() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE);

    Map<String, Object> payload = Map.of("assignee", UIB_SUPPORT_CURATOR.cristinId());
    requestWithPayloadShouldReturnForbidden(
        PUT, UIB_CONTRIBUTOR, TICKET_PATH, payload, publicationIdentifier, ticketIdentifier);
  }
}
