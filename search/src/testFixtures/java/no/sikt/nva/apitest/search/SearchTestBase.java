package no.sikt.nva.apitest.search;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

import io.restassured.response.Response;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import no.sikt.nva.PublicationFactory;
import no.sikt.nva.apitest.base.IntegrationTestBase;

public class SearchTestBase extends IntegrationTestBase {

  public static final PublicationFactory PUBLICATION_FACTORY = new PublicationFactory();

  private static final long DEFAULT_INDEXING_TIMEOUT_SECONDS = 120;

  protected static Response awaitSearchResult(
      Supplier<Response> search, Predicate<Response> isReady) {
    return awaitSearchResult(search, isReady, DEFAULT_INDEXING_TIMEOUT_SECONDS);
  }

  protected static Response awaitSearchResult(
      Supplier<Response> search, Predicate<Response> isReady, long timeoutSeconds) {
    var latestResponse = new AtomicReference<Response>();
    with()
        .pollInterval(fibonacci().with().unit(SECONDS))
        .ignoreExceptions()
        .await()
        .atMost(timeoutSeconds, SECONDS)
        .until(
            () -> {
              var response = search.get();
              latestResponse.set(response);
              return isReady.test(response);
            });
    return latestResponse.get();
  }
}
