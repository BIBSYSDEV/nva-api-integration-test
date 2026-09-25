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
import static no.sikt.nva.apitest.publication.PublicationPaths.filePath;
import static no.sikt.nva.apitest.publication.PublicationPaths.publicationPath;
import static no.sikt.nva.apitest.publication.PublicationPaths.ticketsPath;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import no.sikt.Contributor;
import no.sikt.nva.apitest.base.Affiliation;
import no.sikt.nva.apitest.base.User;
import no.sikt.nva.apitest.publication.file.PendingOpenFile;
import no.sikt.nva.apitest.publication.file.UploadedFile;
import no.sikt.nva.apitest.publication.identifier.fileupload.FileUploadTestBase;
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
class UpdateApiTest extends FileUploadTestBase {

  private static final String TICKETS_FIELD = "tickets";
  private static final String STATUS_FIELD = "status";
  private static final String PUBLISHED = "PUBLISHED";
  private static final String CREATIVE_COMMONS_LICENSE =
      "https://creativecommons.org/licenses/by/4.0/";

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

  private String setupUnpublishedPublication(List<User> contributors) {
    var publicationIdentifier = setupPublishedPublication(contributors);

    unpublish(publicationIdentifier);

    return publicationIdentifier;
  }

  private String setupPublishedPublication(List<User> contributors) {
    var title = "Republish API test " + UUID.randomUUID();
    return PUBLICATION_FACTORY.createPublishedPublication(
        UIB_CREATOR,
        title,
        ACADEMIC_ARTICLE,
        contributors.stream().map(Contributor::asCreator).toList(),
        UIB_PUBLISHING_CURATOR);
  }

  private void unpublish(String publicationIdentifier) {
    givenAuthenticatedJsonRequestAsUser(UIB_CREATOR)
        .body(unpublishRequest())
        .when()
        .put(publicationPath(publicationIdentifier))
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
    return givenAuthenticatedJsonRequestAsUser(requester)
        .body(republishRequest())
        .when()
        .put(publicationPath(publicationIdentifier));
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
        .mapToObj(ignored -> uploadFile(publicationIdentifier, uploader))
        .map(uploadedFile -> uploadedFile.awaitingApprovalUnder(CREATIVE_COMMONS_LICENSE))
        .forEach(pendingFile -> updateFile(publicationIdentifier, uploader, pendingFile));
  }

  private UploadedFile uploadFile(String publicationIdentifier, User uploader) {
    var createResponse = createFileUploadAsUser(uploader, publicationIdentifier);
    var uploadId = createResponse.jsonPath().getString(UPLOAD_ID);
    var key = createResponse.jsonPath().getString(KEY);
    var eTag = prepareAndUploadAsUser(uploader, publicationIdentifier, uploadId, key);

    return completeUploadAsUser(uploader, publicationIdentifier, uploadId, key, eTag)
        .as(UploadedFile.class);
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
