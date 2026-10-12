package no.sikt.nva.apitest.publication.identifier.ticket;

import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.PublicationTicketFactory.GENERAL_SUPPORT_CASE;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_SUPPORT_CURATOR;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import java.util.Map;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Support request workflow")
class SupportRequestIntegrationTest extends PublicationTestBase {

  /** A support request is sent to Support-curator */
  @Test
  @DisplayName("A support request is sent to Support-curator")
  @Description
  void shouldSendSupportRequestToSupportCurator(SoftAssertions softly) {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var requestMessage = "Support request from creator";
    var ticketIdentifier = createSupportRequest(UIB_CREATOR, publicationIdentifier, requestMessage);
    findSupportTicket(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, requestMessage, softly);
  }

  /** Support-curator answers a support request */
  @Test
  @DisplayName("Support-curator answers a support request")
  @Description
  void shouldSendAnswerOnSupportRequest() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var requestMessage = "Support request from creator expecting answer";
    var ticketIdentifier = createSupportRequest(UIB_CREATOR, publicationIdentifier, requestMessage);
    var curatorAnswer = "Answer to support request";
    supportCuratorAnswersSupportRequest(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, curatorAnswer);
    assertSupportCuratorAssignedToSupportRequest(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier);
    assertUserReadsAnswerFromSupportCurator(
        UIB_CREATOR, publicationIdentifier, ticketIdentifier, curatorAnswer);
  }

  /** Support-curator completes a support request */
  @DisplayName("Support-curator completes a support request")
  @Description
  @Test
  void shouldCompleteASupportRequest() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var requestMessage = "Support request being completed";
    var ticketIdentifier = createSupportRequest(UIB_CREATOR, publicationIdentifier, requestMessage);
    supportCuratorCompletesSupportRequest(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier);
    assertThatSupportRequestIsCompleted(UIB_CREATOR, publicationIdentifier, ticketIdentifier);
  }

  /** User opens a completed support request */
  @DisplayName("User opens a completed support request")
  @Description
  @Test
  void shouldOpenACompletedSupportRequest() {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var requestMessage = "Support request being completed";
    var ticketIdentifier = createSupportRequest(UIB_CREATOR, publicationIdentifier, requestMessage);
    supportCuratorCompletesSupportRequest(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier);
    assertThatSupportRequestIsCompleted(UIB_CREATOR, publicationIdentifier, ticketIdentifier);
    var userAnswer = "Open a closed support request message";
    userAnswersResponseToSupportRequest(
        UIB_CREATOR, publicationIdentifier, ticketIdentifier, userAnswer);
    assertSupportRequestHasStatusPending(UIB_CREATOR, publicationIdentifier, ticketIdentifier);
  }

  @Step("Given a creator sends a support request for a publication")
  private static String createSupportRequest(
      User user, String publicationIdentifier, String supportMessage) {

    return PUBLICATION_TICKET_FACTORY.createTicket(
        user, publicationIdentifier, GENERAL_SUPPORT_CASE, supportMessage);
  }

  @Step("Then a support-curator from the same institution has a support ticket")
  private static void findSupportTicket(
      User supportCurator,
      String publicationIdentifier,
      String ticketIdentifier,
      String supportMessage,
      SoftAssertions softly) {

    var tickets =
        PUBLICATION_TICKET_FACTORY.fetchTickets(supportCurator, publicationIdentifier).stream()
            .filter(ticket -> ticket.identifier().equals(ticketIdentifier))
            .toList();

    softly.assertThat(tickets.size()).isEqualTo(1);
    softly
        .assertThat(
            tickets.getFirst().messages().stream()
                .filter(message -> message.text().equals(supportMessage))
                .toList()
                .isEmpty())
        .isFalse();
  }

  @Step("When a support-curator answers the support request")
  private static void supportCuratorAnswersSupportRequest(
      User supportCurator,
      String publicationIdentifier,
      String ticketIdentifier,
      String curatorAnswer) {
    PUBLICATION_TICKET_FACTORY.addMessageToTicket(
        supportCurator, publicationIdentifier, ticketIdentifier, curatorAnswer);
  }

  @Step("Then the support-curator is assigned to the support ticket")
  private static void assertSupportCuratorAssignedToSupportRequest(
      User supportCurator, String publicationIdentifier, String ticketIdentifier) {

    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            supportCurator, publicationIdentifier, ticketIdentifier);
    assertThat(ticket.assignee()).isEqualTo(supportCurator.cristinId());
  }

  @Step("And the user can read the answer from the support-curator")
  private static void assertUserReadsAnswerFromSupportCurator(
      User user, String publicationIdentifier, String ticketIdentifier, String curatorAnswer) {
    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(user, publicationIdentifier, ticketIdentifier);
    assertThat(ticket.messages()).extracting(Message::text).contains(curatorAnswer);
  }

  @Step("When a Support-curator completes the support request")
  private static void supportCuratorCompletesSupportRequest(
      User supportCurator, String publicationIdentifier, String ticketIdentifier) {
    var curatorAnswer = "Complete support request";
    PUBLICATION_TICKET_FACTORY.addMessageToTicket(
        supportCurator, publicationIdentifier, ticketIdentifier, curatorAnswer);
    var requestBody = Map.of("status", "Completed");
    PUBLICATION_TICKET_FACTORY.updateTicket(
        supportCurator, publicationIdentifier, ticketIdentifier, requestBody);
  }

  @Step("Then the support request is completed")
  private static void assertThatSupportRequestIsCompleted(
      User user, String publicationIdentifier, String ticketIdentifier) {
    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(user, publicationIdentifier, ticketIdentifier);

    assertThat(ticket.status()).isEqualTo("Completed");
  }

  @Step("When a user sends an answer to a response from Support-curator")
  private static void userAnswersResponseToSupportRequest(
      User user, String publicationIdentifier, String ticketIdentifier, String userAnswer) {
    PUBLICATION_TICKET_FACTORY.addMessageToTicket(
        user, publicationIdentifier, ticketIdentifier, userAnswer);
  }

  @Step("Then the support request gets status Pending")
  private static void assertSupportRequestHasStatusPending(
      User user, String publicationIdentifier, String ticketIdentifier) {
    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(user, publicationIdentifier, ticketIdentifier);
    assertThat(ticket.status()).isEqualTo("Pending");
  }
}
