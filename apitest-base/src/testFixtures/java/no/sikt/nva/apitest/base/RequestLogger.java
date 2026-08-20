package no.sikt.nva.apitest.base;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs one line per request with the method, the resolved path and the status code, so that a test
 * run shows which endpoints were called. Polls log one line per attempt, which makes every retry
 * visible here as well.
 */
public class RequestLogger implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogger.class);

  @Override
  public Response filter(
      FilterableRequestSpecification requestSpec,
      FilterableResponseSpecification responseSpec,
      FilterContext context) {
    var response = context.next(requestSpec, responseSpec);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
          "{} {} {} in {} ms",
          response.getStatusCode(),
          requestSpec.getMethod(),
          requestSpec.getURI(),
          response.time());
    }
    return response;
  }
}
