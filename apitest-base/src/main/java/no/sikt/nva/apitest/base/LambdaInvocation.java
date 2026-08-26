package no.sikt.nva.apitest.base;

import io.restassured.path.json.JsonPath;
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

  /** The message of the exception the handler threw, or null when the invocation succeeded. */
  public String errorMessage() {
    return errorField(ERROR_MESSAGE_FIELD);
  }

  /** The class name of the exception the handler threw, or null when the invocation succeeded. */
  public String errorType() {
    return errorField(ERROR_TYPE_FIELD);
  }

  private String errorField(String fieldName) {
    return failed() ? new JsonPath(payload).getString(fieldName) : null;
  }
}
