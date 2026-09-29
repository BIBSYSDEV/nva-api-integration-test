package no.sikt.nva;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;

import io.restassured.response.Response;
import java.util.Map;
import no.sikt.nva.apitest.base.User;

public class PublicationTicketFactory {

  private static final String BASE_TICKET_PATH = "/publications/{publicationIdentifier}/ticket";
  private static final String TICKET_PATH = "/publications/{publicationIdentifier}/ticket/{ticketIdentifier}";
  private static final String TICKETS_PATH = "/publications/{publicationIdentifier}/tickets";

  public Response createTicket(User user, String publicationIdentifier, TicketType ticketType) {

    Map<String, Object> requestBody = Map.of("type", ticketType.getType());

    return givenAuthenticatedJsonRequestAsUser(user)
        .body(requestBody)
        .when()
        .post(BASE_TICKET_PATH, publicationIdentifier)
        .then()
        .statusCode(HTTP_CREATED)
        .extract()
        .response();
  }

  public Response fetchTickets(User user, String publicationIdentifier) {

    return givenAuthenticatedJsonRequestAsUser(user)
        .when()
        .get(TICKETS_PATH, publicationIdentifier)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .response();
  }

  public Response fetchTicket(User user, String publicationIdentifier, String ticketIdentifier) {

    return givenAuthenticatedJsonRequestAsUser(user)
        .when()
        .get(TICKET_PATH, publicationIdentifier, ticketIdentifier)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .response();
  }

  public Response deleteTicket(User user, String publicationIdentifier, String ticketIdentifier) {

    return givenAuthenticatedJsonRequestAsUser(user)
        .when()
        .delete(TICKET_PATH, publicationIdentifier, ticketIdentifier)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .response();
  }

  public Response updateTicket(
      User user, String publicationIdentifier, String ticketIdentifier, String status) {

    Map<String, Object> requestBody = Map.of("type", status, "viewStatus", "Read", "assignee", user.cristinId());

    return givenAuthenticatedJsonRequestAsUser(user)
        .body(requestBody)
        .when()
        .put(TICKET_PATH, publicationIdentifier, ticketIdentifier)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .response();
  }

  public Response addMessage(
      User user, String publicationIdentifier, String ticketIdentifier, String message) {

    Map<String, Object> requestBody = Map.of("message", message);

    return givenAuthenticatedJsonRequestAsUser(user)
        .body(requestBody)
        .when()
        .post(TICKET_PATH + "/message", publicationIdentifier, ticketIdentifier)
        .then()
        .statusCode(HTTP_CREATED)
        .extract()
        .response();
  }

  public Response deleteMessage(
      User user, String publicationIdentifier, String ticketIdentifier, String messageIdentifier) {

    return givenAuthenticatedJsonRequestAsUser(user)
        .when()
        .delete(
            TICKET_PATH + "/message/{messageIdentifier}",
            publicationIdentifier,
            ticketIdentifier,
            messageIdentifier)
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .response();
  }
}
