package no.sikt.nva.apitest.publication.identifier.ticket;

import static io.restassured.http.Method.DELETE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.PublicationTicketFactory.GENERAL_SUPPORT_CASE;
import static no.sikt.nva.PublicationTicketFactory.TICKET_PATH;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;

import io.qameta.allure.Description;
import java.util.UUID;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Delete /publication/{publicationIdentifier}/ticket/{ticketIdentifier}")
class DeleteTicketApiTest extends PublicationTestBase {

  /** Delete ticket returns {@code 200 Ok} */
  @Test
  @DisplayName("Delete ticket returns Ok")
  @Description
  void shouldDeleteTicket(SoftAssertions softly) {

    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE);

    PUBLICATION_TICKET_FACTORY.deleteTicket(
        UIB_CREATOR, publicationIdentifier, ticketIdentifier, HTTP_OK);

    var tickets = PUBLICATION_TICKET_FACTORY.fetchTickets(UIB_CREATOR, publicationIdentifier);

    softly.assertThat(tickets.size()).isEqualTo(0);
  }

  /** Trying to delete non-existing ticket returns {@code 404 Not Found} */
  @Test
  @DisplayName("Trying to delete non-existing ticket returns Not Found")
  @Description
  void shouldReturnNotFoundWhenDeletingNonExistingTicket() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    var ticketIdentifier = UUID.randomUUID().toString();

    PUBLICATION_TICKET_FACTORY.deleteTicket(
        UIB_CREATOR, publicationIdentifier, ticketIdentifier, HTTP_NOT_FOUND);
  }

  /** Trying to delete while unauthenticated returns {@code 401 Unauthorized} */
  @Test
  @DisplayName("Trying to delete while unauthenticated returns Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhileUnauthenticated() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE);

    requestShouldReturnUnauthorized(DELETE, TICKET_PATH, publicationIdentifier, ticketIdentifier);
  }

  /** Trying to delete a ticket when not owner returns {@code 403 Forbidden} */
  @Test
  @DisplayName("Trying to delete a ticket when not owner returns Forbidden")
  @Description
  void shouldReturnForbiddenWhileNotOwner() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE);

    requestShouldReturnForbidden(
        DELETE, UIB_CONTRIBUTOR, TICKET_PATH, publicationIdentifier, ticketIdentifier);
  }
}
