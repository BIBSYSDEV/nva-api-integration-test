package no.sikt.nva.apitest.base;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public final class Polling {

  private Polling() {}

  /**
   * Repeats the request until the response satisfies the settled condition, ignoring transient
   * exceptions along the way.
   *
   * @return the first response that satisfied the condition
   */
  public static <T> T pollUntil(Duration timeout, Callable<T> request, Predicate<T> settled) {
    return with()
        .pollInterval(fibonacci().with().unit(SECONDS))
        .ignoreExceptions()
        .await()
        .atMost(timeout)
        .until(request, settled);
  }
}
