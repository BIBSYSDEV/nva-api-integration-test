package no.sikt.nva.apitest.kanalregister;

import io.restassured.path.json.JsonPath;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Caches one response per environment, so a test class asserting several facets of the same
 * response sends only one request. A non-static {@code @BeforeAll} cannot do this instead, because
 * {@code @Parameter} fields are not yet injected when it runs.
 */
public final class SharedResponse {

  private final Map<ChannelRegistryEnvironment, JsonPath> responses = new ConcurrentHashMap<>();
  private final Function<ChannelRegistryEnvironment, JsonPath> request;

  public SharedResponse(Function<ChannelRegistryEnvironment, JsonPath> request) {
    this.request = request;
  }

  public JsonPath forEnvironment(ChannelRegistryEnvironment environment) {
    return responses.computeIfAbsent(environment, request);
  }
}
