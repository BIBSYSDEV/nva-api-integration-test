package no.sikt.nva.apitest.base;

import io.restassured.path.json.JsonPath;
import io.restassured.path.json.exception.JsonPathException;
import java.util.Optional;
import nva.commons.core.StringUtils;

/**
 * The outcome of a Lambda invocation. A handler that throws still returns HTTP 200 from the Invoke
 * API, with the exception reported in the function error header and the payload holding the error
 * message instead of the handler output, so a failure has to be read off the response rather than
 * caught.
 */
public record LambdaInvocation(String payload, String functionError) {

  private static final String ERROR_MESSAGE_FIELD = "errorMessage";
  private static final String ERROR_TYPE_FIELD = "errorType";

  public boolean failed() {
    return StringUtils.isNotBlank(functionError);
  }

  /**
   * The message of the exception the handler threw, or null when the invocation succeeded. Falls
   * back to the raw payload, because a failure that never reached the handler's own error
   * serialization leaves something other than the usual error json behind, and that text is then
   * the only account of what went wrong.
   */
  public String errorMessage() {
    return failed() ? errorField(ERROR_MESSAGE_FIELD).orElse(payload) : null;
  }

  /** The class name of the exception the handler threw, or null when there is no error json. */
  public String errorType() {
    return failed() ? errorField(ERROR_TYPE_FIELD).orElse(null) : null;
  }

  private Optional<String> errorField(String fieldName) {
    Optional<String> value;
    try {
      value = Optional.ofNullable(new JsonPath(payload).getString(fieldName));
    } catch (JsonPathException payloadIsNotErrorJson) {
      value = Optional.empty();
    }
    return value;
  }
}
