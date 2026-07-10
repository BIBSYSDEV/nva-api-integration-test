package no.sikt.nva.apitest.base;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import org.awaitility.pollinterval.FibonacciPollInterval;

public final class Polling {

  private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofMinutes(3);
  private static final Duration MAX_POLL_INTERVAL = Duration.ofSeconds(10);
  private static final FibonacciPollInterval FIBONACCI_SECONDS = fibonacci().with().unit(SECONDS);

  private Polling() {}

  /**
   * Repeats the request until the response satisfies the settled condition, ignoring transient
   * exceptions along the way. The poll interval backs off along the Fibonacci sequence but is
   * capped, so checks keep running through the whole timeout budget.
   *
   * @return the first response that satisfied the condition
   */
  public static <T> T pollUntil(Duration timeout, Callable<T> request, Predicate<T> settled) {
    return with()
        .pollInterval(Polling::cappedFibonacciInterval)
        .ignoreExceptions()
        .await()
        .atMost(timeout)
        .until(request, settled);
  }

  public static <T> T pollUntil(Callable<T> request, Predicate<T> settled) {
    return pollUntil(DEFAULT_POLL_TIMEOUT, request, settled);
  }

  private static Duration cappedFibonacciInterval(int pollCount, Duration previousDuration) {
    var fibonacciInterval = FIBONACCI_SECONDS.next(pollCount, previousDuration);
    return fibonacciInterval.compareTo(MAX_POLL_INTERVAL) > 0
        ? MAX_POLL_INTERVAL
        : fibonacciInterval;
  }
}
