package no.sikt.nva.apitest.publication.batch;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;

/**
 * The report ManuallyUpdatePublicationsHandler writes to its output stream. The counters
 * distinguish the stages of a run: {@code totalHits} is what the search matched, {@code
 * hitsReturned} and {@code resourcesFetched} what the pages actually delivered, {@code
 * resourcesMatched} how many of those the update applied to, and {@code resourcesChanged} how many
 * ended up with a change.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManualUpdateReport(
    boolean dryRun,
    int limit,
    boolean limitReached,
    int pageSize,
    String type,
    String oldValue,
    String newValue,
    int totalHits,
    int pagesFetched,
    int hitsReturned,
    int resourcesFetched,
    int resourcesMatched,
    int resourcesChanged,
    List<ResourceChange> changes) {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  /**
   * A report always carries a changes array, but a handler that stopped short of writing one would
   * otherwise turn every reader of it into a null check, and fail as a NullPointerException that
   * says nothing about the report being incomplete.
   */
  public ManualUpdateReport {
    changes = isNull(changes) ? List.of() : List.copyOf(changes);
  }

  public static ManualUpdateReport fromJson(String json) {
    try {
      return MAPPER.readValue(json, ManualUpdateReport.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(exception);
    }
  }

  public List<String> changedIdentifiers() {
    return changes.stream().map(ResourceChange::identifier).toList();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ResourceChange(String identifier, List<FieldChange> fieldChanges) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FieldChange(String path, String oldValue, String newValue) {}
}
