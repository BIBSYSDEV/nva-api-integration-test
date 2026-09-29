package no.sikt.nva.apitest.publication.identifier.ticket;

import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.sikt.Category;
import no.sikt.nva.apitest.publication.PublicationTestBase;

@ExtendWith(SoftAssertionsExtension.class)
public class DoiRequestApiTest extends PublicationTestBase{


  @Test
  void shouldCreateDoiRequest(SoftAssertions softly) {
    var title = "Integration test puvlication " + UUID.randomUUID();
    var publicationIdentifiewr = PUBLICATION_FACTORY.createPublishedPublication(Category.ACADEMIC_ARTICLE, title);


  }

}