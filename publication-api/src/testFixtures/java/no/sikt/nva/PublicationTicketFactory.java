package no.sikt.nva;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;

import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.publication.ticket.Ticket;

public class PublicationTicketFactory {

  public static final String BASE_TICKET_PATH = "/publication/{publicationIdentifier}/ticket";
  public static final String TICKET_PATH =
      "/publication/{publicationIdentifier}/ticket/{ticketIdentifier}";
  public static final String TICKET_MESSAGE_PATH =
      "/publication/{publicationIdentifier}/ticket/{ticketIdentifier}/message/{messageIdentifier}";
  public static final String TICKETS_PATH = "/publication/{publicationIdentifier}/tickets";
  public static final String GENERAL_SUPPORT_CASE = "GeneralSupportCase";
  public static final String DOI_REQUEST = "DoiRequest";
  public static final String PUBLISHING_REQUEST = "PublishingRequest";

  public static final String STATUS_ACTIVE = "Active";
  public static final String STATUS_DELETED = "Deleted";

  public String createTicket(
      User user, String publicationIdentifier, String ticketType, String... messages) {
    return createTicket(user, publicationIdentifier, ticketType, HTTP_CREATED, messages);
  }

  public String createTicket(
      User user,
      String publicationIdentifier,
      String ticketType,
      int expectedResponseCode,
      String... messages) {

    var requestBody = Map.of("type", ticketType, "messages", createMessages(messages));

    var location =
        givenAuthenticatedJsonRequestAsUser(user)
            .body(requestBody)
            .when()
            .post(BASE_TICKET_PATH, publicationIdentifier)
            .then()
            .statusCode(expectedResponseCode)
            .extract()
            .header("Location");

    return nonNull(location) ? List.of(location.split("/")).getLast() : "";
  }

  private List<Map<String, String>> createMessages(String... messages) {
    return List.of(messages).stream()
        .map(message -> Map.of("type", "Message", "text", message))
        .collect(Collectors.toList());
  }

  public List<Ticket> fetchTickets(User user, String publicationIdentifier) {
    return fetchTickets(user, publicationIdentifier, HTTP_OK);
  }

  public List<Ticket> fetchTickets(
      User user, String publicationIdentifier, int expectedResponseCode) {

    return givenAuthenticatedJsonRequestAsUser(user)
        .when()
        .get(TICKETS_PATH, publicationIdentifier)
        .then()
        .statusCode(expectedResponseCode)
        .extract()
        .jsonPath()
        .getList("tickets", Ticket.class);
  }

  public Ticket fetchTicket(User user, String publicationIdentifier, String ticketIdentifier) {
    return fetchTicket(user, publicationIdentifier, ticketIdentifier, HTTP_OK);
  }

  public Ticket fetchTicket(
      User user, String publicationIdentifier, String ticketIdentifier, int expectedResponseCode) {

    return givenAuthenticatedJsonRequestAsUser(user)
        .when()
        .get(TICKET_PATH, publicationIdentifier, ticketIdentifier)
        .then()
        .statusCode(expectedResponseCode)
        .extract()
        .as(Ticket.class);
  }

  public Response deleteTicket(User user, String publicationIdentifier, String ticketIdentifier) {
    return deleteTicket(user, publicationIdentifier, ticketIdentifier, HTTP_OK);
  }

  public Response deleteTicket(
      User user, String publicationIdentifier, String ticketIdentifier, int expectedResponseCode) {

    return givenAuthenticatedJsonRequestAsUser(user)
        .when()
        .delete(TICKET_PATH, publicationIdentifier, ticketIdentifier)
        .then()
        .statusCode(expectedResponseCode)
        .extract()
        .response();
  }

  public void updateTicket(
      User user,
      String publicationIdentifier,
      String ticketIdentifier,
      Map<String, String> requestBody) {
    updateTicket(user, publicationIdentifier, ticketIdentifier, requestBody, HTTP_ACCEPTED);
  }

  public void updateTicket(
      User user,
      String publicationIdentifier,
      String ticketIdentifier,
      Map<String, String> requestBody,
      int expectedResponseCode) {

    givenAuthenticatedJsonRequestAsUser(user)
        .body(requestBody)
        .when()
        .put(TICKET_PATH, publicationIdentifier, ticketIdentifier)
        .then()
        .statusCode(expectedResponseCode);
  }

  public Response addMessageToTicket(
      User user, String publicationIdentifier, String ticketIdentifier, String message) {
    return addMessageToTicket(user, publicationIdentifier, ticketIdentifier, message, HTTP_CREATED);
  }

  public Response addMessageToTicket(
      User user,
      String publicationIdentifier,
      String ticketIdentifier,
      String message,
      int expectedResponseCode) {

    var requestBody = Map.of("message", message);

    return givenAuthenticatedJsonRequestAsUser(user)
        .body(requestBody)
        .when()
        .post(TICKET_PATH + "/message", publicationIdentifier, ticketIdentifier)
        .then()
        .statusCode(expectedResponseCode)
        .extract()
        .response();
  }

  public Response deleteMessage(
      User user, String publicationIdentifier, String ticketIdentifier, String messageIdentifier) {
    return deleteMessage(user, publicationIdentifier, ticketIdentifier, messageIdentifier, HTTP_OK);
  }

  public Response deleteMessage(
      User user,
      String publicationIdentifier,
      String ticketIdentifier,
      String messageIdentifier,
      int expectedResponseCode) {

    return givenAuthenticatedJsonRequestAsUser(user)
        .when()
        .delete(TICKET_MESSAGE_PATH, publicationIdentifier, ticketIdentifier, messageIdentifier)
        .then()
        .statusCode(expectedResponseCode)
        .extract()
        .response();
  }
}
