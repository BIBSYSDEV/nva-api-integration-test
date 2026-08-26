package no.sikt.nva.apitest.publication.batch;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Map;

/**
 * The payload of ManuallyUpdatePublicationsHandler in nva-publication-api. Fields left null are
 * omitted from the JSON rather than sent as null, so that a request can leave out a field entirely
 * and exercise the handler's own defaulting and validation.
 */
@JsonInclude(NON_NULL)
public record ManualUpdateRequest(
    String type,
    String oldValue,
    String newValue,
    Map<String, String> searchParams,
    String comparator,
    Boolean dryRun,
    Integer limit,
    Integer pageSize) {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final boolean DRY_RUN = true;

  /**
   * A dry run, since that is the safe default for a test: the handler reports what it would change
   * without writing anything. Tests that need the changes persisted opt in with {@link
   * #withDryRun}.
   */
  public static ManualUpdateRequest dryRunOf(
      String type, String oldValue, String newValue, Map<String, String> searchParams) {
    return new ManualUpdateRequest(
        type, oldValue, newValue, searchParams, null, DRY_RUN, null, null);
  }

  public ManualUpdateRequest withDryRun(Boolean newDryRun) {
    return new ManualUpdateRequest(
        type, oldValue, newValue, searchParams, comparator, newDryRun, limit, pageSize);
  }

  public ManualUpdateRequest withLimit(Integer newLimit) {
    return new ManualUpdateRequest(
        type, oldValue, newValue, searchParams, comparator, dryRun, newLimit, pageSize);
  }

  public ManualUpdateRequest withPageSize(Integer newPageSize) {
    return new ManualUpdateRequest(
        type, oldValue, newValue, searchParams, comparator, dryRun, limit, newPageSize);
  }

  public ManualUpdateRequest withOldValue(String newOldValue) {
    return new ManualUpdateRequest(
        type, newOldValue, newValue, searchParams, comparator, dryRun, limit, pageSize);
  }

  public ManualUpdateRequest withNewValue(String replacementValue) {
    return new ManualUpdateRequest(
        type, oldValue, replacementValue, searchParams, comparator, dryRun, limit, pageSize);
  }

  public ManualUpdateRequest withSearchParams(Map<String, String> newSearchParams) {
    return new ManualUpdateRequest(
        type, oldValue, newValue, newSearchParams, comparator, dryRun, limit, pageSize);
  }

  public String toJson() {
    try {
      return MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
