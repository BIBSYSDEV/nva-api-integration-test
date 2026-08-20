package no.sikt.nva.apitest.base;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.ConditionTimeoutException;
import org.awaitility.core.EvaluatedCondition;
import org.awaitility.core.IgnoredException;
import org.awaitility.pollinterval.FibonacciPollInterval;

public final class Polling {

  private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofMinutes(3);
  private static final Duration MAX_POLL_INTERVAL = Duration.ofSeconds(10);
  private static final FibonacciPollInterval FIBONACCI_SECONDS = fibonacci().with().unit(SECONDS);
  private static final int ATTEMPTS_IN_FAILURE_MESSAGE = 5;
  private static final int ATTEMPT_SUMMARY_LENGTH = 300;
  private static final int LAST_RESPONSE_LENGTH = 4000;

  private Polling() {}

  /**
   * Repeats the request until the response satisfies the settled condition, ignoring transient
   * exceptions along the way. The poll interval backs off along the Fibonacci sequence but is
   * capped, so checks keep running through the whole timeout budget. Every attempt is recorded, so
   * a timeout reports the responses that were actually observed.
   *
   * @return the first response that satisfied the condition
   */
  public static <T> T pollUntil(Duration timeout, Callable<T> request, Predicate<T> settled) {
    var attempts = new PollAttempts<>(settled);
    try {
      return with()
          .pollInterval(Polling::cappedFibonacciInterval)
          .conditionEvaluationListener(attempts)
          .ignoreExceptions()
          .await()
          .atMost(timeout)
          .until(request, settled);
    } catch (ConditionTimeoutException timedOut) {
      throw attempts.withObservedResponses(timedOut);
    }
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

  /**
   * Records what every poll attempt saw, including responses that were rejected by the settled
   * condition and exceptions that were ignored, so that a timeout can report them instead of only
   * reporting that the condition was never met.
   */
  private static final class PollAttempts<T> implements ConditionEvaluationListener<T> {

    private final List<String> attemptSummaries = new CopyOnWriteArrayList<>();
    private final AtomicReference<T> lastValue = new AtomicReference<>();
    private final Predicate<T> settled;

    private PollAttempts(Predicate<T> settled) {
      this.settled = settled;
    }

    @Override
    public void conditionEvaluated(EvaluatedCondition<T> condition) {
      var value = condition.getValue();
      lastValue.set(value);
      record(condition.getElapsedTimeInMS(), summarize(value));
    }

    private String summarize(T value) {
      return settled instanceof SettledCondition<T> describedCondition
          ? describedCondition.describe(value)
          : oneLineSummary(value);
    }

    @Override
    public void exceptionIgnored(IgnoredException ignoredException) {
      var throwable = ignoredException.getThrowable();
      record(
          ignoredException.getElapsedTimeInMS(),
          "%s: %s"
              .formatted(
                  throwable.getClass().getSimpleName(), oneLineSummary(throwable.getMessage())));
    }

    private void record(long elapsedMillis, String summary) {
      var attemptSummary =
          "attempt %d after %ds: %s"
              .formatted(attemptSummaries.size() + 1, elapsedMillis / 1000, summary);
      attemptSummaries.add(attemptSummary);
    }

    private ConditionTimeoutException withObservedResponses(ConditionTimeoutException timedOut) {
      var shownSummaries = lastAttemptSummaries();
      var message =
          """
          %s
          Observed %d attempt(s), last %d shown:
          %s
          Last response:
          %s\
          """
              .formatted(
                  timedOut.getMessage(),
                  attemptSummaries.size(),
                  shownSummaries.size(),
                  shownSummaries.stream().map(summary -> "  " + summary).collect(joining("\n")),
                  fullSummary(lastValue.get()));
      return new ConditionTimeoutException(message);
    }

    private List<String> lastAttemptSummaries() {
      var firstShown = Math.max(0, attemptSummaries.size() - ATTEMPTS_IN_FAILURE_MESSAGE);
      return attemptSummaries.subList(firstShown, attemptSummaries.size());
    }
  }

  private static String oneLineSummary(Object value) {
    return truncate(asText(value).replaceAll("\\s+", " ").trim(), ATTEMPT_SUMMARY_LENGTH);
  }

  private static String fullSummary(Object value) {
    return truncate(asText(value), LAST_RESPONSE_LENGTH);
  }

  private static String asText(Object value) {
    String text;
    if (value instanceof Response response) {
      text = "%d %s".formatted(response.getStatusCode(), response.asString());
    } else if (value instanceof JsonPath jsonPath) {
      text = jsonPath.prettify();
    } else {
      text = String.valueOf(value);
    }
    return text;
  }

  private static String truncate(String text, int maxLength) {
    return text.length() <= maxLength ? text : text.substring(0, maxLength) + "... (truncated)";
  }
}
