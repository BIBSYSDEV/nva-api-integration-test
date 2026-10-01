package no.sikt.nva.apitest.publication.identifier.ticket;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import static io.restassured.http.Method.GET;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.PublicationTicketFactory.GENERAL_SUPPORT_CASE;
import static no.sikt.nva.PublicationTicketFactory.TICKET_PATH;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_CREATOR;
import no.sikt.nva.apitest.publication.PublicationTestBase;

@ExtendWith(SoftAssertionsExtension.class)
class FetchTicketApiTest extends PublicationTestBase {

  /**
   * Fetch a existing ticket returns ticket and {@code 200 Ok}
   */
  @Test
  @DisplayName("Fetch existing ticket return ticket")
  @Description 
  void shouldFetchTicket(SoftAssertions softly) {

    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    PUBLICATION_TICKET_FACTORY.createTicket(
        UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE);

    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY
            .fetchTickets(UIB_CREATOR, publicationIdentifier)
            .getFirst()
            .identifier();

    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            UIB_CREATOR, publicationIdentifier, ticketIdentifier, HTTP_OK);

    softly.assertThat(ticket.type()).isEqualTo(GENERAL_SUPPORT_CASE);
    softly.assertThat(ticket.ownerAffiliation()).isEqualTo(UIB_CREATOR.extractAffiliation(UIB));
  }

  /**
   * Trying to fetch a non-existing ticket returns {@code 404 Not Found}
   */
  @Test
  @DisplayName("Fetch non-existing ticket returns Not Found")
  @Description
  void shouldReturnNotFoundWhenTicketNotExisting() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    PUBLICATION_TICKET_FACTORY.createTicket(
        UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE);

    var ticketIdentifier = UUID.randomUUID().toString();

    PUBLICATION_TICKET_FACTORY.fetchTicket(
        UIB_CREATOR, publicationIdentifier, ticketIdentifier, HTTP_NOT_FOUND);
  }

  /**
   * Trying to fetch a ticket when not authenticate returns {@code }
   */
  @Test
  void shouldReturnUnauthorizedWhenNotAuthenticated() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    PUBLICATION_TICKET_FACTORY.createTicket(
        UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE);

    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY
            .fetchTickets(UIB_CREATOR, publicationIdentifier)
            .getFirst()
            .identifier();

    requestShouldReturnUnauthorized(GET, TICKET_PATH, publicationIdentifier, ticketIdentifier);
  }

  @Test
  void shouldReturnForbiddenWhenNotOwner() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    PUBLICATION_TICKET_FACTORY.createTicket(
        UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE);

    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY
            .fetchTickets(UIB_CREATOR, publicationIdentifier)
            .getFirst()
            .identifier();

    requestShouldReturnForbidden(
        GET, UIS_CREATOR, TICKET_PATH, publicationIdentifier, ticketIdentifier);
  }
}
