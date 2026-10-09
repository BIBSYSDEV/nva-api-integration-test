package no.sikt.nva.apitest.publication.identifier.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.PublicationTicketFactory.GENERAL_SUPPORT_CASE;
import no.sikt.nva.apitest.base.User;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_SUPPORT_CURATOR;
import no.sikt.nva.apitest.publication.PublicationTestBase;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Support request workflow")
class SupportRequestIntegrationTest extends PublicationTestBase {

  /** A support request is sent to support curator */
  @Test
  @DisplayName("A support request is sent to support curator")
  @Description
  void shouldSendSupportRequestToSupportCurator(SoftAssertions softly) {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var requestMessage = "Support request from creator";
    var ticketIdentifier = createSupportRequest(UIB_CREATOR, publicationIdentifier, requestMessage);
    findSupportTicket(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, requestMessage, softly);
  }

  /*
  given a creator sends a support request
  when a support-curator answers the support request
  then the support-curator is assigned to the support ticket
  and the user can read the answer from the support-curator
  (and the support request is closed?)
  */

  /** Support curator answers a support request */
  @Test
  @DisplayName("Support curator answers a support request")
  @Description
  void shouldSendAnswerOnSupportRequest(SoftAssertions softly) {
    var publicationIdentifier =
        PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
    var requestMessage = "Support request from creator expeting answer";
    var ticketIdentifier = createSupportRequest(UIB_CREATOR, publicationIdentifier, requestMessage);
    var curatorAnswer = "Answer to support request";
    supportCuratorAnswersSupportRequest(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, curatorAnswer);
    assertSupportCuratorAssignedToSupportRequest(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier);
    assertUserReadsAnswerFromSupportCurator(UIB_CREATOR, publicationIdentifier, ticketIdentifier, curatorAnswer);
  }

  /*
  given a creator sends a support request
  when a support-curator closes the support request
  then the support request get status Closed

  given a creator sends a support request
  when a support-curator answers the support request
  and the support request get status Closed
  then the creator can send a response to the answer
  and the support request is opened
  */

  @Step("Given a creator sends a support request for a publication")
  private static String createSupportRequest(
      User user, String publicationIdentifier, String supportMessage) {

    return PUBLICATION_TICKET_FACTORY.createTicket(
        user, publicationIdentifier, GENERAL_SUPPORT_CASE, supportMessage);
  }

  @Step("Then a support-curator from the same instituion find a support ticket")
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

  @Step("when a support-curator answers the support request")
  private static void supportCuratorAnswersSupportRequest(
      User supportCurator,
      String publicationIdentifier,
      String ticketIdentifier,
      String curatorAnswer) {
    PUBLICATION_TICKET_FACTORY.addMessageToTicket(
        UIB_SUPPORT_CURATOR, publicationIdentifier, ticketIdentifier, curatorAnswer);
  }

  @Step("then the support-curator is assigned to the support ticket")
  private static void assertSupportCuratorAssignedToSupportRequest(
      User supportCurator, String publicationIdentifier, String ticketIdentifier) {

    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            supportCurator, publicationIdentifier, ticketIdentifier);
    assertThat(ticket.assignee()).isEqualTo(supportCurator.cristinId());
  }

  @Step ("and the user can read the answer from the support-curator")
  private static void assertUserReadsAnswerFromSupportCurator(User user, String publicationIdentifier, String ticketIdentifier, String curatorAnswer){
    var ticket =
        PUBLICATION_TICKET_FACTORY.fetchTicket(
            user, publicationIdentifier, ticketIdentifier);
    assertThat(ticket.messages().stream().filter(message -> message.text().equals(curatorAnswer)).toList()).isNotEmpty();
  }
}
