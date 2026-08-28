package no.sikt.nva.apitest.publication.batch;

import static no.sikt.Category.ACADEMIC_ARTICLE;
import static no.sikt.Category.ACADEMIC_MONOGRAPH;
import static no.sikt.Role.CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_CREATOR;
import static no.sikt.nva.apitest.base.UserFixtures.UIB_PUBLISHING_CURATOR;
import static no.sikt.nva.apitest.publication.PublicationFields.PUBLICATION_CONTEXT;
import static no.sikt.nva.apitest.publication.PublicationFields.PUBLICATION_INSTANCE;
import static no.sikt.nva.apitest.publication.PublicationFields.TYPE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.sikt.Category;
import no.sikt.Contributor;
import no.sikt.nva.PublicationFactory;
import no.sikt.nva.apitest.base.Project;

/**
 * The publications the batch update tests run against, created once for the whole JVM and shared by
 * every test class that only reads them.
 *
 * <p>Sharing matters because the set is large: the handler pages through search results, so the
 * tests need enough publications to span several real pages, and creating one costs three
 * sequential requests plus indexing. Building a set per test class would multiply that.
 *
 * <p>The mix exists because the update types disagree about what a publication must look like.
 * Contributor and project updates work on anything, publisher updates need a book rather than a
 * journal, and the unconfirmed variants need a channel that is a name instead of an id. Each group
 * is sized well above one page so that every type is exercised across a page boundary.
 *
 * <p>Sharing is only safe because the tests reading this set all run as dry runs. A test that
 * persists must create its own publications.
 *
 * @param titleToken the token every publication in the set shares, used to scope a run to it
 * @param unconfirmedPublisherName the publisher name on the unconfirmed group; it embeds the title
 *     token, so a CONTAINS comparison on the token alone matches exactly this run's publications
 * @param identifiers every publication in the set, in no particular order
 */
public record SharedPublicationSet(
    String titleToken, String unconfirmedPublisherName, List<String> identifiers) {

  /** Comfortably more than one page at the page size the tests use, so each type spans pages. */
  public static final int PUBLICATIONS_PER_GROUP = 35;

  private static final int GROUPS = 3;
  public static final int TOTAL_PUBLICATIONS = GROUPS * PUBLICATIONS_PER_GROUP;

  private static final String UNCONFIRMED_PUBLISHER_PREFIX = "Testforlaget ";
  private static final String UNCONFIRMED_PUBLISHER_TYPE = "UnconfirmedPublisher";
  private static final String PUBLISHER_FIELD = "publisher";
  private static final String NAME_FIELD = "name";
  private static final PublicationFactory PUBLICATION_FACTORY = new PublicationFactory();
  private static final List<Contributor> CREATORS = List.of(new Contributor(UIB_CREATOR, CREATOR));

  public SharedPublicationSet {
    identifiers = List.copyOf(identifiers);
  }

  public static SharedPublicationSet get() {
    return Holder.INSTANCE;
  }

  public Map<String, String> searchParams() {
    return IndexedPublications.searchParamsFor(titleToken);
  }

  /** The search parameters of a documented example, narrowed to this set by the title token. */
  public Map<String, String> searchParamsWith(String parameterName, String value) {
    var searchParams = new HashMap<>(searchParams());
    searchParams.put(parameterName, value);
    return searchParams;
  }

  private static SharedPublicationSet create() {
    var titleToken = IndexedPublications.randomTitleToken();
    var unconfirmedPublisherName = UNCONFIRMED_PUBLISHER_PREFIX + titleToken;

    var creators =
        Stream.of(
                repeat(() -> createArticle(titleToken)),
                repeat(() -> createMonograph(titleToken)),
                repeat(() -> createUnconfirmedMonograph(titleToken, unconfirmedPublisherName)))
            .flatMap(group -> group)
            .toList();

    var identifiers = creators.parallelStream().map(Supplier::get).toList();
    IndexedPublications.awaitSearchable(TOTAL_PUBLICATIONS, titleToken);

    return new SharedPublicationSet(titleToken, unconfirmedPublisherName, identifiers);
  }

  private static Stream<Supplier<String>> repeat(Supplier<String> creator) {
    return IntStream.range(0, PUBLICATIONS_PER_GROUP).mapToObj(index -> creator);
  }

  /** A journal article, which carries the confirmed serial publication the tests match on. */
  private static String createArticle(String titleToken) {
    return createPublication(titleToken, ACADEMIC_ARTICLE, emptyReference());
  }

  /** A monograph, whose template carries a confirmed publisher with an id. */
  private static String createMonograph(String titleToken) {
    return createPublication(titleToken, ACADEMIC_MONOGRAPH, emptyReference());
  }

  /** A monograph whose confirmed publisher is replaced by a name without an id. */
  private static String createUnconfirmedMonograph(String titleToken, String publisherName) {
    return createPublication(
        titleToken, ACADEMIC_MONOGRAPH, unconfirmedPublisherReference(publisherName));
  }

  private static String createPublication(
      String titleToken, Category category, Map<String, Object> reference) {
    return PUBLICATION_FACTORY.createPublishedPublicationWithProjects(
        UIB_CREATOR,
        titleToken,
        category,
        CREATORS,
        UIB_PUBLISHING_CURATOR,
        reference,
        List.of(Project.CURRENT.getValue()));
  }

  private static Map<String, Object> emptyReference() {
    return new HashMap<>();
  }

  private static Map<String, Object> unconfirmedPublisherReference(String publisherName) {
    var publisher = new HashMap<String, Object>();
    publisher.put(TYPE, UNCONFIRMED_PUBLISHER_TYPE);
    publisher.put(NAME_FIELD, publisherName);

    var publicationContext = new HashMap<String, Object>();
    publicationContext.put(PUBLISHER_FIELD, publisher);

    return Map.of(PUBLICATION_CONTEXT, publicationContext, PUBLICATION_INSTANCE, new HashMap<>());
  }

  // Class initialization is thread safe, so concurrent test classes all wait for the same set.
  private static final class Holder {
    private static final SharedPublicationSet INSTANCE = create();
  }
}
