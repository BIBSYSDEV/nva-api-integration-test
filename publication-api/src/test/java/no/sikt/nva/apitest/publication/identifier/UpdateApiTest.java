package no.sikt.nva.apitest.publication.identifier;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;
import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.nva.apitest.base.Affiliation.KRISTIANIA;
import static no.sikt.nva.apitest.base.Affiliation.UIB;
import static no.sikt.nva.apitest.base.Requests.givenAuthenticatedJsonRequestAsUser;
import static no.sikt.nva.apitest.base.UserFixtures.KRISTIANIA_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_EDITOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.ENTITY_DESCRIPTION_FIELD;
import static no.sikt.nva.apitest.publication.PublicationFields.IDENTIFIER_FIELD;
import static no.sikt.nva.apitest.publication.PublicationFields.TYPE;
import static no.sikt.nva.apitest.publication.PublicationPaths.filePath;
import static no.sikt.nva.apitest.publication.PublicationPaths.publicationPath;
import static no.sikt.nva.apitest.publication.PublicationPaths.ticketsPath;
import static no.sikt.nva.apitest.publication.file.FileUploadFlow.uploadExampleFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import no.sikt.Contributor;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.publication.PublicationTestBase;
import no.sikt.nva.apitest.publication.file.PendingOpenFile;
import no.sikt.nva.apitest.publication.ticket.Ticket;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The update endpoint dispatches on the type of the request body rather than on the path, so each
 * request type it accepts is covered by its own nested class.
 */
@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("PUT /publication/{identifier}")
class UpdateApiTest extends PublicationTestBase {

  private static final String TICKETS_FIELD = "tickets";
  private static final String STATUS_FIELD = "status";
  private static final String MAIN_TITLE_FIELD = ENTITY_DESCRIPTION_FIELD + ".mainTitle";
  private static final String PUBLISHED = "PUBLISHED";
  private static final String UNPUBLISHED = "UNPUBLISHED";
  private static final String CREATIVE_COMMONS_LICENSE =
      "https://creativecommons.org/licenses/by/4.0/";

  /**
   * Updating replaces the metadata of a publication. The owner, contributors and curators at a
   * related institution may update it, anyone else may not.
   */
  @Nested
  @DisplayName("UpdatePublicationRequest")
  class UpdateRequest {

    /** The owner of a draft should be able to change its title. */
    @Test
    @DisplayName("Owner updates the title of their draft")
    @Description(useJavaDoc = true)
    void shouldUpdateTitleWhenOwnerUpdatesDraft() {
      var draft = PUBLICATION_FACTORY.createDraftPublication(UIB_CREATOR).jsonPath();
      var newTitle = randomTitle();

      var updatedTitle =
          requestUpdate(UIB_CREATOR, draft.getString(IDENTIFIER_FIELD), withTitle(draft, newTitle))
              .then()
              .statusCode(HTTP_OK)
              .extract()
              .jsonPath()
              .getString(MAIN_TITLE_FIELD);

      assertThat(updatedTitle).isEqualTo(newTitle);
    }

    /**
     * A user with no relation to the publication, at another institution, should get status {@code
     * 403 Forbidden} when updating it.
     */
    @Test
    @DisplayName("Unrelated user cannot update a publication")
    @Description(useJavaDoc = true)
    @Disabled("FIXME: Unrelated user should get 403, but gets 401. See NP-51870.")
    void shouldReturnForbiddenWhenUnrelatedUserUpdates() {
      var draft = PUBLICATION_FACTORY.createDraftPublication(UIB_CREATOR).jsonPath();

      requestUpdate(
              KRISTIANIA_CREATOR,
              draft.getString(IDENTIFIER_FIELD),
              withTitle(draft, randomTitle()))
          .then()
          .statusCode(HTTP_FORBIDDEN);
    }
  }

  /**
   * Unpublishing takes a published publication out of circulation and requires a comment saying
   * why. The owner may unpublish their own publication, anyone else unrelated to it may not.
   */
  @Nested
  @DisplayName("UnpublishPublicationRequest")
  class UnpublishRequest {

    /** The owner of a published publication should be able to unpublish it. */
    @Test
    @DisplayName("Owner unpublishes their published publication")
    @Description(useJavaDoc = true)
    void shouldUnpublishWhenOwnerUnpublishesPublishedPublication() {
      var publicationIdentifier = setupPublishedPublication(List.of(UIB_CREATOR));

      var status =
          requestUpdate(UIB_CREATOR, publicationIdentifier, unpublishRequest())
              .then()
              .statusCode(HTTP_ACCEPTED)
              .extract()
              .jsonPath()
              .getString(STATUS_FIELD);

      assertThat(status).isEqualTo(UNPUBLISHED);
    }

    /**
     * A user with no relation to the publication, at another institution, should get status {@code
     * 403 Forbidden} when unpublishing it.
     */
    @Test
    @DisplayName("Unrelated user cannot unpublish a publication")
    @Description(useJavaDoc = true)
    @Disabled("FIXME: Unrelated user should get 403, but gets 401. See NP-51870.")
    void shouldReturnForbiddenWhenUnrelatedUserUnpublishes() {
      var publicationIdentifier = setupPublishedPublication(List.of(UIB_CREATOR));

      requestUpdate(KRISTIANIA_CREATOR, publicationIdentifier, unpublishRequest())
          .then()
          .statusCode(HTTP_FORBIDDEN);
    }
  }

  /**
   * Republishing makes an unpublished publication published again.
   *
   * <p>A file uploaded while a publication is unpublished must also be covered by a file approval
   * ticket once the publication is republished, so that it does not stay in pending approval with
   * nothing to approve it through. Files uploaded by different institutions are approved
   * separately, one ticket per institution. A pending file approval ticket is readable only by the
   * institution it was created for, so the number of tickets on the publication is counted as the
   * distinct tickets seen by the institutions taking part in the scenario.
   */
  @Nested
  @DisplayName("RepublishPublicationRequest")
  class RepublishRequest {

    /** An editor republishing an unpublished publication should make it published again. */
    @Test
    @DisplayName("Editor republishes an unpublished publication")
    @Description(useJavaDoc = true)
    void shouldPublishAgainWhenEditorRepublishesUnpublishedPublication() {
      var publicationIdentifier = setupUnpublishedPublication(List.of(UIB_CREATOR));

      republish(publicationIdentifier);

      assertThat(publicationStatus(publicationIdentifier)).isEqualTo(PUBLISHED);
    }

    /**
     * Only an editor may republish, so the publication owner republishing without the editor role
     * should return status {@code 403 Forbidden}.
     */
    @Test
    @DisplayName("Owner who is not an editor cannot republish")
    @Description(useJavaDoc = true)
    @Disabled("FIXME: Non-editor should get 403, but gets 401. See NP-51870.")
    void shouldReturnForbiddenWhenNonEditorRepublishes() {
      var publicationIdentifier = setupUnpublishedPublication(List.of(UIB_CREATOR));

      requestRepublish(UIB_CREATOR, publicationIdentifier).then().statusCode(HTTP_FORBIDDEN);
    }

    /**
     * Only an unpublished publication can be republished, so republishing one that is already
     * published should return status {@code 403 Forbidden}.
     */
    @Test
    @DisplayName("Already published publication cannot be republished")
    @Description(useJavaDoc = true)
    void shouldReturnForbiddenWhenRepublishingPublishedPublication() {
      var publicationIdentifier = setupPublishedPublication(List.of(UIB_CREATOR));

      requestRepublish(UIB_EDITOR, publicationIdentifier).then().statusCode(HTTP_FORBIDDEN);
    }

    /**
     * A file uploaded while the publication is unpublished should be covered by a file approval
     * ticket at the uploading institution once the publication is republished.
     */
    @Test
    @DisplayName("File uploaded while unpublished is covered by an approval ticket")
    @Disabled
    @Description(useJavaDoc = true)
    void shouldCoverFileUploadedWhileUnpublishedByApprovalTicket(SoftAssertions softly) {
      var publicationIdentifier = setupUnpublishedPublication(List.of(UIB_CREATOR));
      uploadFilesForApproval(publicationIdentifier, UIB_CREATOR, 1);

      republish(publicationIdentifier);

      var ticketsAtUib = fileApprovalTicketsVisibleTo(UIB_CREATOR, publicationIdentifier);
      assertInstitutionHasFileApprovalTicketCovering(softly, UIB, ticketsAtUib, 1);
      assertPublicationHasFileApprovalTickets(softly, 1, ticketsAtUib);
    }

    /**
     * Several files uploaded by the same institution while the publication is unpublished should be
     * covered by a single file approval ticket once the publication is republished.
     */
    @Test
    @DisplayName("Files from the same institution share a single approval ticket")
    @Disabled
    @Description(useJavaDoc = true)
    void shouldCoverFilesFromSameInstitutionByOneApprovalTicket(SoftAssertions softly) {
      var publicationIdentifier = setupUnpublishedPublication(List.of(UIB_CREATOR));
      uploadFilesForApproval(publicationIdentifier, UIB_CREATOR, 2);

      republish(publicationIdentifier);

      var ticketsAtUib = fileApprovalTicketsVisibleTo(UIB_CREATOR, publicationIdentifier);
      assertInstitutionHasFileApprovalTicketCovering(softly, UIB, ticketsAtUib, 2);
      assertPublicationHasFileApprovalTickets(softly, 1, ticketsAtUib);
    }

    /**
     * Files uploaded by two institutions while the publication is unpublished should be covered by
     * one file approval ticket per institution once the publication is republished.
     */
    @Test
    @DisplayName("Each uploading institution gets its own approval ticket")
    @Disabled
    @Description(useJavaDoc = true)
    void shouldCreateOneApprovalTicketPerUploadingInstitution(SoftAssertions softly) {
      var publicationIdentifier =
          setupUnpublishedPublication(List.of(UIB_CREATOR, KRISTIANIA_CREATOR));
      uploadFilesForApproval(publicationIdentifier, UIB_CREATOR, 1);
      uploadFilesForApproval(publicationIdentifier, KRISTIANIA_CREATOR, 2);

      republish(publicationIdentifier);

      var ticketsAtUib = fileApprovalTicketsVisibleTo(UIB_CREATOR, publicationIdentifier);
      var ticketsAtKristiania =
          fileApprovalTicketsVisibleTo(KRISTIANIA_CREATOR, publicationIdentifier);
      assertInstitutionHasFileApprovalTicketCovering(softly, UIB, ticketsAtUib, 1);
      assertInstitutionHasFileApprovalTicketCovering(softly, KRISTIANIA, ticketsAtKristiania, 2);
      assertPublicationHasFileApprovalTickets(softly, 2, ticketsAtUib, ticketsAtKristiania);
    }

    /**
     * Republishing a publication that received no files while it was unpublished should not create
     * a file approval ticket.
     */
    @Test
    @DisplayName("Republishing without newly uploaded files creates no approval ticket")
    @Disabled
    @Description(useJavaDoc = true)
    void shouldNotCreateApprovalTicketWhenRepublishingWithoutNewFiles() {
      var publicationIdentifier = setupUnpublishedPublication(List.of(UIB_CREATOR));

      republish(publicationIdentifier);

      assertThat(fileApprovalTicketsVisibleTo(UIB_CREATOR, publicationIdentifier))
          .as("file approval tickets on the publication")
          .isEmpty();
    }

    private static void assertInstitutionHasFileApprovalTicketCovering(
        SoftAssertions softly,
        Affiliation institution,
        List<Ticket> ticketsVisibleAtInstitution,
        int fileCount) {
      var ticketsOwnedByInstitution =
          ticketsVisibleAtInstitution.stream()
              .filter(ticket -> ticket.isOwnedBy(institution.getValue()))
              .toList();

      softly
          .assertThat(ticketsOwnedByInstitution)
          .as("file approval tickets owned by institution %s", institution)
          .hasSize(1);
      softly
          .assertThat(ticketsOwnedByInstitution)
          .as("files awaiting approval at institution %s", institution)
          .allSatisfy(ticket -> assertThat(ticket.filesForApproval()).hasSize(fileCount));
    }

    /**
     * A pending file approval ticket is readable only at the institution it belongs to, so the
     * tickets on the publication are the distinct tickets seen across the institutions' views.
     */
    @SafeVarargs
    private static void assertPublicationHasFileApprovalTickets(
        SoftAssertions softly, int ticketCount, List<Ticket>... ticketsVisibleAtEachInstitution) {
      var distinctTicketIdentifiers =
          Stream.of(ticketsVisibleAtEachInstitution)
              .flatMap(List::stream)
              .map(Ticket::identifier)
              .collect(toSet());

      softly
          .assertThat(distinctTicketIdentifiers)
          .as("file approval tickets on the publication")
          .hasSize(ticketCount);
    }
  }

  /**
   * Deleting terminates an unpublished publication for good. Only an editor may delete, and this is
   * a different operation from the {@code DELETE} method on the same path.
   */
  @Nested
  @DisplayName("DeletePublicationRequest")
  class DeleteRequest {

    /** An editor should be able to delete an unpublished publication. */
    @Test
    @DisplayName("Editor deletes an unpublished publication")
    @Description(useJavaDoc = true)
    void shouldAcceptWhenEditorDeletesUnpublishedPublication() {
      var publicationIdentifier = setupUnpublishedPublication(List.of(UIB_CREATOR));

      requestUpdate(UIB_EDITOR, publicationIdentifier, deleteRequest())
          .then()
          .statusCode(HTTP_ACCEPTED);
    }

    /**
     * Only an editor may delete, so the publication owner deleting without the editor role should
     * return status {@code 403 Forbidden}.
     */
    @Test
    @DisplayName("Owner who is not an editor cannot delete")
    @Description(useJavaDoc = true)
    @Disabled("FIXME: Non-editor should get 403, but gets 401. See NP-51870.")
    void shouldReturnForbiddenWhenNonEditorDeletes() {
      var publicationIdentifier = setupUnpublishedPublication(List.of(UIB_CREATOR));

      requestUpdate(UIB_CREATOR, publicationIdentifier, deleteRequest())
          .then()
          .statusCode(HTTP_FORBIDDEN);
    }
  }

  private Response requestUpdate(User requester, String publicationIdentifier, Object body) {
    return givenAuthenticatedJsonRequestAsUser(requester)
        .body(body)
        .when()
        .put(publicationPath(publicationIdentifier));
  }

  private static Map<String, Object> withTitle(JsonPath draft, String title) {
    var publication = draft.<String, Object>getMap("");
    publication.put(
        ENTITY_DESCRIPTION_FIELD,
        PUBLICATION_FACTORY.createEntityDescription(
            title, ACADEMIC_ARTICLE, List.of(Contributor.asCreator(UIB_CREATOR))));
    return publication;
  }

  private static Map<String, String> deleteRequest() {
    return Map.of(TYPE, "DeletePublicationRequest");
  }

  private String setupUnpublishedPublication(List<User> contributors) {
    var publicationIdentifier = setupPublishedPublication(contributors);

    unpublish(publicationIdentifier);

    return publicationIdentifier;
  }

  private String setupPublishedPublication(List<User> contributors) {
    return PUBLICATION_FACTORY.createPublishedPublication(
        UIB_CREATOR,
        randomTitle(),
        ACADEMIC_ARTICLE,
        contributors.stream().map(Contributor::asCreator).toList(),
        UIB_PUBLISHING_CURATOR);
  }

  private void unpublish(String publicationIdentifier) {
    requestUpdate(UIB_CREATOR, publicationIdentifier, unpublishRequest())
        .then()
        .statusCode(HTTP_ACCEPTED);
  }

  private static Map<String, String> unpublishRequest() {
    return Map.of(
        TYPE,
        "UnpublishPublicationRequest",
        "comment",
        "Unpublished by the file approval on republish API test");
  }

  /** Only an editor may republish, so the republish request is sent by the editor at UiB. */
  private void republish(String publicationIdentifier) {
    requestRepublish(UIB_EDITOR, publicationIdentifier).then().statusCode(HTTP_OK);
  }

  private Response requestRepublish(User requester, String publicationIdentifier) {
    return requestUpdate(requester, publicationIdentifier, republishRequest());
  }

  private static Map<String, String> republishRequest() {
    return Map.of(TYPE, "RepublishPublicationRequest");
  }

  private String publicationStatus(String publicationIdentifier) {
    return givenAuthenticatedJsonRequestAsUser(UIB_CREATOR)
        .when()
        .get(publicationPath(publicationIdentifier))
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath()
        .getString(STATUS_FIELD);
  }

  /**
   * Uploads files as the given user and then moves them from uploaded to pending approval, which is
   * what the registration form does once the uploader has given a file its metadata. A file that
   * stays uploaded is not awaiting anyone's approval.
   */
  private void uploadFilesForApproval(String publicationIdentifier, User uploader, int fileCount) {
    range(0, fileCount)
        .mapToObj(ignored -> uploadExampleFile(uploader, publicationIdentifier))
        .map(uploadedFile -> uploadedFile.awaitingApprovalUnder(CREATIVE_COMMONS_LICENSE))
        .forEach(pendingFile -> updateFile(publicationIdentifier, uploader, pendingFile));
  }

  private void updateFile(String publicationIdentifier, User uploader, PendingOpenFile file) {
    givenAuthenticatedJsonRequestAsUser(uploader)
        .body(file)
        .when()
        .post(filePath(publicationIdentifier, file.identifier()))
        .then()
        .statusCode(HTTP_ACCEPTED);
  }

  private List<Ticket> fileApprovalTicketsVisibleTo(User user, String publicationIdentifier) {
    return givenAuthenticatedJsonRequestAsUser(user)
        .when()
        .get(ticketsPath(publicationIdentifier))
        .then()
        .statusCode(HTTP_OK)
        .extract()
        .jsonPath()
        .getList(TICKETS_FIELD, Ticket.class)
        .stream()
        .filter(Ticket::isFileApproval)
        .toList();
  }
}
