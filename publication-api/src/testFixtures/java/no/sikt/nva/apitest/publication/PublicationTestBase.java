package no.sikt.nva.apitest.publication;

import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;

import no.sikt.nva.PublicationFactory;
import no.sikt.nva.PublicationTicketFactory;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import no.sikt.nva.apitest.base.UserFixtures;

public class PublicationTestBase extends IntegrationTestBase {

  public static final PublicationFactory PUBLICATION_FACTORY = new PublicationFactory();
  public static final PublicationTicketFactory PUBLICATION_TICKET_FACTORY =
      new PublicationTicketFactory();

  protected static String setupDraftPublication() {
    return PUBLICATION_FACTORY
        .createDraftPublication(UserFixtures.UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER_FIELD);
  }
}
