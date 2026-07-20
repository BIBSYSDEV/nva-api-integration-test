package no.sikt.nva.apitest.kanalregister;

import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.LEVEL_DISPLAY_PATH;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.LEVEL_PATH;
import static no.sikt.nva.apitest.kanalregister.ChannelRegistryRequests.LEVEL_YEAR_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.path.json.JsonPath;
import java.time.LocalDate;
import java.time.ZoneId;
import org.assertj.core.api.SoftAssertions;

/**
 * Shared assertions on the channel data model. All endpoint types return the same model, so every
 * endpoint test asserts through these helpers to keep the endpoints from drifting apart.
 */
public final class ChannelAssertions {

  private static final String NULL_STRING = "null";

  private ChannelAssertions() {}

  /** The levelElementDto carries the channel's level for the requested year. */
  public static void assertLevelForYear(SoftAssertions softly, JsonPath json, Channel channel) {
    softly.assertThat(json.getString(LEVEL_PATH)).as("level").isEqualTo(channel.level());
    softly
        .assertThat(json.getString(LEVEL_YEAR_PATH))
        .as("levelElementDto.year")
        .isEqualTo(String.valueOf(channel.year()));
  }

  /** The levelElementDto carries levelDisplay, matching level for non-X channels. */
  public static void assertLevelDisplayMatchesLevel(
      SoftAssertions softly, JsonPath json, Channel channel) {
    softly
        .assertThat(json.getString(LEVEL_DISPLAY_PATH))
        .as("levelDisplay for a channel with level %s", channel.level())
        .isEqualTo(channel.level());
  }

  /** An X-channel carries the counting level in level and the X mark in levelDisplay. */
  public static void assertCountingLevelAndXMark(
      SoftAssertions softly, JsonPath json, Channel channel) {
    softly.assertThat(json.getString(LEVEL_PATH)).as("counting level").isEqualTo(channel.level());
    softly.assertThat(json.getString(LEVEL_DISPLAY_PATH)).as("X mark").isEqualTo("X");
  }

  /** The levelHistories list includes the requested year. */
  public static void assertLevelHistoryIncludesYear(
      SoftAssertions softly, JsonPath json, Channel channel) {
    var levelForRequestedYear =
        json.getString("levelHistories.find { it.year == %d }.level".formatted(channel.year()));
    softly
        .assertThat(levelForRequestedYear)
        .as("levelHistories entry for %d", channel.year())
        .isEqualTo(channel.level());
  }

  /** Without a year segment, the level is for the current year or absent, never a future year. */
  public static void assertLevelIsForCurrentYearOrAbsent(SoftAssertions softly, JsonPath json) {
    var currentYear = String.valueOf(LocalDate.now(ZoneId.systemDefault()).getYear());
    softly
        .assertThat(json.getString(LEVEL_YEAR_PATH))
        .as("levelElementDto.year without a year segment")
        .satisfiesAnyOf(
            year -> assertThat(year).isNull(), year -> assertThat(year).isEqualTo(currentYear));
  }

  /** Absent decision texts are JSON null, never the literal string "null". */
  public static void assertDecisionTextsAreNotNullStrings(SoftAssertions softly, JsonPath json) {
    softly
        .assertThat(json.getString("levelElementDto.vedtak"))
        .as("vedtak")
        .isNotEqualTo(NULL_STRING);
    softly
        .assertThat(json.getString("levelElementDto.decision"))
        .as("decision")
        .isNotEqualTo(NULL_STRING);
  }
}
