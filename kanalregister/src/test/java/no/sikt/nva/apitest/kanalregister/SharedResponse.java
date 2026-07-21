package no.sikt.nva.apitest.kanalregister;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Caches one response per environment, so a test class asserting several facets of the same
 * response sends only one request. A non-static {@code @BeforeAll} cannot do this instead, because
 * {@code @Parameter} fields are not yet injected when it runs.
 */
public final class SharedResponse {

  private final Map<ChannelRegistryEnvironment, Response> responses = new ConcurrentHashMap<>();
  private final Function<ChannelRegistryEnvironment, Response> request;

  public SharedResponse(Function<ChannelRegistryEnvironment, Response> request) {
    this.request = request;
  }

  /** A fresh JsonPath over the cached response, so root paths set by one test do not leak. */
  public JsonPath jsonPathForEnvironment(ChannelRegistryEnvironment environment) {
    return response(environment).jsonPath();
  }

  /** The raw response body, which schema validation needs instead of a parsed JsonPath. */
  public String bodyForEnvironment(ChannelRegistryEnvironment environment) {
    return response(environment).asString();
  }

  private Response response(ChannelRegistryEnvironment environment) {
    return responses.computeIfAbsent(environment, request);
  }
}
