package no.sikt.nva.apitest.publication.identifier.ticket;

import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Step;
import static no.sikt.nva.PublicationTicketFactory.DOI_REQUEST;
import no.sikt.nva.apitest.base.User;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import no.sikt.nva.apitest.publication.PublicationTestBase;

@ExtendWith(SoftAssertionsExtension.class)
class DoiRequestIntegrationTest extends PublicationTestBase{


  // publishedPublicationIdentifier = PUBLICATION_FACTORY.createPublishedPublication(ACADEMIC_ARTICLE, randomTitle());
  
  @Step("Given a draft publication")
  private static String createDraftPublications() {
    return PUBLICATION_FACTORY.createDraftPublication(UIB_CREATOR)
      .jsonPath().getString("identifier");
  }

  @Step("When the {user} reserves a DOI for the publication")
  private static String reserveDoi(User user, String publicationIdentifier) {

    return PUBLICATION_TICKET_FACTORY.createTicket(user, publicationIdentifier, DOI_REQUEST);
  }

  
}
