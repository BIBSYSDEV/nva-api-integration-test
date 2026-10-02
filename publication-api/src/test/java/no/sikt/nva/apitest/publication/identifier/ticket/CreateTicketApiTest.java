package no.sikt.nva.apitest.publication.identifier.ticket;

import static io.restassured.http.Method.POST;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.PublicationTicketFactory.BASE_TICKET_PATH;
import static no.sikt.nva.PublicationTicketFactory.DOI_REQUEST;
import static no.sikt.nva.PublicationTicketFactory.GENERAL_SUPPORT_CASE;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CONTRIBUTOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;

import io.qameta.allure.Description;
import java.util.UUID;
import java.util.stream.Collectors;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("POST /publication/{publicationIdentifier}/ticket")
class CreateTicketApiTest extends PublicationTestBase {

  /** Creating a ticket on a Publication returns {@code 201 Created} */
  @ParameterizedTest
  @ValueSource(strings = {DOI_REQUEST, GENERAL_SUPPORT_CASE})
  @DisplayName("Create ticket on Publication")
  @Description
  void shouldCreateTicket(String ticketType, SoftAssertions softly) {
    var ticketMessage = "API test ticket message " + UUID.randomUUID();
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var ticketIdentifier =
        PUBLICATION_TICKET_FACTORY.createTicket(
            UIB_CREATOR, publicationIdentifier, ticketType, HTTP_CREATED, ticketMessage);
    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            UIB_CREATOR, publicationIdentifier, ticketIdentifier, HTTP_OK);

    softly.assertThat(ticket.type()).isEqualTo(ticketType);
    softly.assertThat(ticket.owner()).isEqualTo(UIB_CREATOR.cristinId());
    softly.assertThat(ticket.isOwnedBy(UIB)).isTrue();
    softly.assertThat(ticket.publicationIdentifier()).isEqualTo(publicationIdentifier);
    softly
        .assertThat(
            ticket.messages().stream()
                .filter(message -> message.text().equals(ticketMessage))
                .collect(Collectors.toList()))
        .hasSize(1);
  }

  /**
   * Trying to create a ticket on a Publication when not authenticated returns {@code 401
   * Unauthorized}
   */
  @Test
  @DisplayName("Trying to create a ticket when not authenticated return Unauthorized")
  @Description
  void shouldReturnUnauthorizedWhenUnauthenticated() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    requestShouldReturnUnauthorized(POST, BASE_TICKET_PATH, publicationIdentifier);
  }

  /** Trying to create a ticket on a Publication when not owner returns {@code 403 Forbidden} */
  @Test
  //   @Disabled("FIXME: Returns 500, enable when bug is fixed")
  @DisplayName("Trying to create a ticket when not owner returns Forbidden")
  @Description
  void shouldReturnForbiddenWhenNotOwnerOfPublication() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());

    PUBLICATION_TICKET_FACTORY.createTicket(
        UIB_CONTRIBUTOR, publicationIdentifier, GENERAL_SUPPORT_CASE, HTTP_FORBIDDEN);
  }

  /** Trying to create a ticket on a non-existing Publication returns {@code 404 Not Found} */
  @Test
  @DisplayName("Trying to create a ticket on non-existing Publication returns Not Found")
  @Description
  void shouldReturnNotFoundWhenCreatingOnNonExistingPublication() {
    var publicationIdentifier = UUID.randomUUID().toString();

    PUBLICATION_TICKET_FACTORY.createTicket(
        UIB_CREATOR, publicationIdentifier, GENERAL_SUPPORT_CASE, HTTP_NOT_FOUND);
  }

  /** Trying to create a ticket with wrong ticket type returns {@code 400 Bad Request} */
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
