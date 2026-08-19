package no.sikt.nva.apitest.base;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * A poll condition that can describe what it saw. Polling reports this description for every
 * attempt, which keeps the interesting value in the failure instead of the whole response body.
 */
public interface SettledCondition<T> extends Predicate<T> {

  /**
   * Builds a condition that settles once the extracted value equals the expected one. A timeout
   * then reports that value and what was expected for every attempt, which is more readable than
   * the whole response for endpoints that return large payloads.
   */
  static <T> SettledCondition<T> settledWhen(
      String valueName, Object expectedValue, Function<T, Object> actualValue) {
    return settledWhen(
        value -> Objects.equals(expectedValue, actualValue.apply(value)),
        value ->
            "%s was %s, expected %s".formatted(valueName, actualValue.apply(value), expectedValue));
  }

  /**
   * Builds a condition that settles once the extracted count reaches the minimum. Suited to values
   * that only grow while a test waits, such as the number of search hits.
   */
  static <T> SettledCondition<T> settledWhenAtLeast(
      String valueName, int minimum, ToIntFunction<T> actualCount) {
    return settledWhen(
        value -> actualCount.applyAsInt(value) >= minimum,
        value ->
            "%s was %d, expected at least %d"
                .formatted(valueName, actualCount.applyAsInt(value), minimum));
  }

  /**
   * Builds a condition from an arbitrary test, reported by the given description. Use this when the
   * condition is not a comparison of a single extracted value.
   */
  static <T> SettledCondition<T> settledWhen(
      Predicate<T> settled, Function<T, String> description) {
    return new SettledCondition<>() {
      @Override
      public boolean test(T value) {
        return settled.test(value);
      }

      @Override
      public String describe(T value) {
        return description.apply(value);
      }
    };
  }

  String describe(T value);
}
