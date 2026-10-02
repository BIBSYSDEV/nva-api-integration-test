package no.sikt.nva.apitest.publication;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;

import java.util.stream.Stream;
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

  public static String randomTitle() {
    return "Test publication - %s".formatted(randomUUID());
  }

  protected static Stream<String> ticketTypes() {
    return Stream.of("DoiRequest", "GeneralSupportCase", "PublishingRequest");
  }
}
