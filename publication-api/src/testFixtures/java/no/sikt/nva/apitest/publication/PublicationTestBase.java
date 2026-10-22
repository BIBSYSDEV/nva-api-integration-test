package no.sikt.nva.apitest.publication;

import static java.util.UUID.randomUUID;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;

import java.util.List;
import java.util.stream.Stream;
import no.sikt.Contributor;
import no.sikt.nva.PublicationFactory;
import no.sikt.nva.PublicationTicketFactory;
import no.sikt.nva.apitest.base.IntegrationTestBase;

public class PublicationTestBase extends IntegrationTestBase {

  public static final PublicationFactory PUBLICATION_FACTORY = new PublicationFactory();
  public static final PublicationTicketFactory PUBLICATION_TICKET_FACTORY =
      new PublicationTicketFactory();

  protected static String setupDraftPublication() {
    return PUBLICATION_FACTORY
        .createDraftPublication(UIB_CREATOR)
        .jsonPath()
        .getString(IDENTIFIER_FIELD);
  }

  protected static String setupPublishedPublication() {
    return PUBLICATION_FACTORY.createPublishedPublication(
        UIB_CREATOR,
        randomTitle(),
        ACADEMIC_ARTICLE,
        List.of(Contributor.asCreator(UIB_CREATOR)),
        UIB_PUBLISHING_CURATOR);
  }

  protected static String setupUnpublishedPublication() {
    var identifier = setupPublishedPublication();
    PUBLICATION_FACTORY.unpublish(UIB_CREATOR, identifier);
    return identifier;
  }

  public static String randomTitle() {
    return "Test publication - %s".formatted(randomUUID());
  }

  protected static Stream<String> ticketTypes() {
    return Stream.of("DoiRequest", "GeneralSupportCase", "PublishingRequest");
  }
}
