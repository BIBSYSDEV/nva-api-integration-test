package no.sikt.nva.apitest.publication.identifier.ticket;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.qameta.allure.Description;
import static io.restassured.http.Method.POST;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.PublicationTicketFactory.BASE_TICKET_PATH;
import static no.sikt.nva.PublicationTicketFactory.GENERAL_SUPPORT_CASE;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import no.sikt.nva.apitest.publication.PublicationTestBase;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST /publication/{publicationIdentifier}/ticket")
class CreateTicketApiTest extends PublicationTestBase {

  /**
   * Creating a ticket on a Publication returns {@code 201 Created}
   */
  @ParameterizedTest
  @MethodSource("ticketTypes")
  @DisplayName("Create ticket on Publication")
  @Description 
  void shouldCreateTicket(String ticketType, SoftAssertions softly) {
    if (!"PublishingRequest".equals(ticketType)) {
      var publicationIdentifier =
          PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
      PUBLICATION_TICKET_FACTORY.createTicket(UIB_CREATOR, publicationIdentifier, ticketType, HTTP_CREATED);
      var tickets = PUBLICATION_TICKET_FACTORY.fetchTickets(UIB_CREATOR, publicationIdentifier);
      softly.assertThat(tickets.size()).isEqualTo(1);
      softly.assertThat(tickets.getFirst().type()).isEqualTo(ticketType);
    }
  }

  /**
   * Trying to create a ticket on a Publication when not authenticated returns {@code 401 Unauthorized}
   */
  @Test
  @DisplayName("Trying to create a ticket when not authenticated return Unauthorized")
  @Description 
  void shouldReturnUnauthorizedWhenUnauthenticated() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    requestShouldReturnUnauthorized(POST, BASE_TICKET_PATH, publicationIdentifier);
  }

  /**
   * Trying to create a ticket on a Publication when not owner returns {@code 403 Forbidden}
   */
  @Test
  @DisplayName("Trying to create a ticket when not owner returns Forbidden")
  @Description 
  void shouldReturnForbiddenWhenNotOwnerOfPublication() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    requestShouldReturnForbidden(POST, UIB_CONTRIBUTOR, BASE_TICKET_PATH, publicationIdentifier);
  }

  /**
   * Trying to create a ticket on a non-existing Publication returns {@code 404 Not Found}
   */
  @Test
  @DisplayName("Trying to create a ticket on non-existing Publication returns Not Found")
  @Description 
  void shouldReturnNotFoundWhenCreatingOnNonExistingPublication() {
    var publicationIdentifier = UUID.randomUUID().toString();

    PUBLICATION_TICKET_FACTORY.createTicket(
        UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE, HTTP_NOT_FOUND);
  }

  /**
   * Trying to create a ticket with wrong ticket type returns {@code 400 Bad Request}
   */
  @Test
  @DisplayName("Trying to create ticket with wrong ticket type returns Bad Request")
  @Description
  void shouldReturnBadRequestWhenCreatingWithWrongTicketType() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    PUBLICATION_TICKET_FACTORY.createTicket(
        UIB_CREATOR, publicationIdentifier, "WrongRequest", HTTP_BAD_REQUEST);
  }
}
