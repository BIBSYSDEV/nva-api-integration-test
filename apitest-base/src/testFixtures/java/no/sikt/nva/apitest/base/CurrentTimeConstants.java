package no.sikt.nva.apitest.base;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

public class CurrentTimeConstants {

  private static final LocalDate TODAY = LocalDate.now();
  public static final String CURRENT_YEAR = Integer.toString(TODAY.getYear());
  public static final String CURRENT_MONTH = Integer.toString(TODAY.getMonthValue());
  public static final String CURRENT_MONTH_SHORT_NAME = Month.of(TODAY.getMonthValue())
      .getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toLowerCase(Locale.ENGLISH);
  public static final String CURRENT_DAY = Integer.toString(TODAY.getDayOfMonth());
}
