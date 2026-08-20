package no.sikt.nva.apitest.scientificindex;

import static no.sikt.nva.apitest.base.UserFixtures.APP_ADMIN;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_DOI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_EDITOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_NVI_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_SUPPORT_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_THESIS_CURATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIS_NVI_CURATOR;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.stream.Stream;
import no.sikt.nva.apitest.base.IntegrationTestBase;
import org.junit.jupiter.params.provider.Arguments;

public class ScientificIndexTestBase extends IntegrationTestBase {

  public static final NviCandidateFactory CANDIDATE_FACTORY = new NviCandidateFactory();

  /** Test users that should have read access to all NVI endpoints. */
  protected static Stream<Arguments> usersWithNviReadAccess() {
    return Stream.of(
        argumentSet("Application administrator", APP_ADMIN),
        argumentSet("UiB NVI curator", UIB_NVI_CURATOR),
        argumentSet("UiS NVI curator", UIS_NVI_CURATOR));
  }

  /** Test users that do not have access to endpoints that require an NVI role. */
  protected static Stream<Arguments> usersWithoutNviAccess() {
    return Stream.of(
        argumentSet("Editor", UIB_EDITOR),
        argumentSet("Registrar", UIB_CREATOR),
        argumentSet("DOI curator", UIB_DOI_CURATOR),
        argumentSet("Publishing curator", UIB_PUBLISHING_CURATOR),
        argumentSet("Support curator", UIB_SUPPORT_CURATOR),
        argumentSet("Thesis curator", UIB_THESIS_CURATOR));
  }
}
