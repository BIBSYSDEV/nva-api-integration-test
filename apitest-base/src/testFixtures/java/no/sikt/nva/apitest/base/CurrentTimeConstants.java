package no.sikt.nva.apitest.base;

import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;

public final class CurrentTimeConstants {

  private static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("Europe/Oslo");
  public static final String CURRENT_YEAR = getCurrentYear().toString();
  public static final String CURRENT_MONTH = String.valueOf(getCurrentDate().getMonthValue());
  public static final String CURRENT_MONTH_SHORT_NAME =
      getCurrentDate()
          .getMonth()
          .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
          .toLowerCase(Locale.ENGLISH);
  public static final String CURRENT_DAY = String.valueOf(getCurrentDate().getDayOfMonth());
  public static final String CURRENT_DATE = getCurrentDate().toString();

  private CurrentTimeConstants() {}

  public static LocalDate getCurrentDate() {
    return LocalDate.now(DEFAULT_TIME_ZONE);
  }

  public static Year getCurrentYear() {
    return Year.now(DEFAULT_TIME_ZONE);
  }
}
