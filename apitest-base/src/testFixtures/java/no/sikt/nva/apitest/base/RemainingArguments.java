package no.sikt.nva.apitest.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.SimpleArgumentsAggregator;

/**
 * Collects every argument from the annotated parameter's position onwards into a single list, so a
 * {@code @ParameterizedTest} can accept a variable number of trailing arguments. JUnit has no
 * varargs support for parameterized tests, an aggregator is the supported alternative.
 */
public class RemainingArguments extends SimpleArgumentsAggregator {

  @Override
  protected Object aggregateArguments(
      ArgumentsAccessor accessor,
      Class<?> targetType,
      AnnotatedElementContext context,
      int parameterIndex) {
    // The accessor holds all arguments of the invocation, not just the unconsumed ones
    var arguments = accessor.toList();
    return List.copyOf(arguments.subList(parameterIndex, arguments.size()));
  }

  /** Marks the last parameter of a parameterized test as the collector for trailing arguments. */
  @Target(ElementType.PARAMETER)
  @Retention(RetentionPolicy.RUNTIME)
  @AggregateWith(RemainingArguments.class)
  public @interface PathParams {}
}
