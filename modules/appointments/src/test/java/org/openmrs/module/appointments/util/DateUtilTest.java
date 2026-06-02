package org.openmrs.module.appointments.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.openmrs.module.appointments.util.DateUtil.convertToLocalDateFromUTC;
import static org.openmrs.module.appointments.util.DateUtil.getEpochTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DateUtilTest {

  private TimeZone defaultTimeZone;

  @BeforeEach
  void setUp() {
    defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  @AfterEach
  void tearDown() {
    TimeZone.setDefault(defaultTimeZone);
  }

  @Test
  void convertToDateReturnsNullWhenDateStringIsEmpty() throws ParseException {
    Date date = DateUtil.convertToDate("", DateUtil.DateFormatType.UTC);

    assertNull(date);
  }

  @Test
  void convertToDateReturnsNullWhenDateFormatTypeIsNull() throws ParseException {
    Date date = DateUtil.convertToDate("2017-03-15T16:57:09.0Z", null);

    assertNull(date);
  }

  @Test
  void convertToDateParsesUtcDateString() throws ParseException {
    Date date = DateUtil.convertToDate("2017-03-15T16:57:09.0Z", DateUtil.DateFormatType.UTC);

    assertNotNull(date);
    assertEquals(Instant.parse("2017-03-15T16:57:09Z").toEpochMilli(), date.getTime());
  }

  @Test
  void convertToLocalDateFromUtcReturnsNullWhenDateStringIsNull() throws ParseException {
    Date date = convertToLocalDateFromUTC(null);

    assertNull(date);
  }

  @Test
  void convertToLocalDateFromUtcParsesDateWithUtcTimeZone() throws ParseException {
    Date date = convertToLocalDateFromUTC("2017-03-15T16:57:09.0Z");

    assertNotNull(date);
    assertEquals(Instant.parse("2017-03-15T16:57:09Z").toEpochMilli(), date.getTime());
  }

  @Test
  void getCalendarReturnsCalendarForGivenDate() {
    Date date = new Date();

    Calendar calendar = DateUtil.getCalendar(date);

    assertEquals(date, calendar.getTime());
  }

  @Test
  void getStartOfDayReturnsStartOfCurrentDay() {
    Date date = DateUtil.getStartOfDay();
    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    assertEquals("00:00:00", dateFormat.format(date));
  }

  @Test
  void getEndOfDayReturnsEndOfCurrentDay() {
    Date date = DateUtil.getEndOfDay();
    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    assertEquals("23:59:59", dateFormat.format(date));
  }

  @Test
  void getEpochTimeReturnsTimeWithinCurrentDay() throws ParseException {
    Date date = convertToLocalDateFromUTC("2017-03-15T16:57:09.0Z");

    long milliseconds = getEpochTime(date.getTime());

    assertEquals(61_029_000L, milliseconds);
  }

  @Test
  void getEpochTimeUsesTimeOfDayForNegativeEpochValues() {
    long milliseconds = getEpochTime(-10_000L);

    assertEquals(86_390_000L, milliseconds);
  }

  @Test
  void convertUtcToGivenFormatReturnsNullIfAnyParameterIsNull() {
    String result = DateUtil.convertUTCToGivenFormat(new Date(), "yyyy-MM-dd HH:mm:ss", null);

    assertNull(result);
  }

  @Test
  void convertUtcToGivenFormatReturnsFormattedDateForValidParameters() {
    Date date = Date.from(Instant.parse("2017-03-15T16:57:09Z"));

    String result = DateUtil.convertUTCToGivenFormat(date, "yyyy-MM-dd HH:mm:ss", "GMT+00:00");

    assertEquals("2017-03-15 16:57:09", result);
  }

  @Test
  void convertUtcToGivenFormatReturnsNullWhenDateTimeIsNull() {
    String result = DateUtil.convertUTCToGivenFormat(null, "yyyy-MM-dd HH:mm:ss", "GMT+00:00");

    assertNull(result);
  }

  @Test
  void convertUtcToGivenFormatReturnsNullWhenFormatIsEmpty() {
    String result = DateUtil.convertUTCToGivenFormat(new Date(), "", "GMT+00:00");

    assertNull(result);
  }

  @Test
  void convertUtcToGivenFormatReturnsNullWhenTimeZoneIsEmpty() {
    String result = DateUtil.convertUTCToGivenFormat(new Date(), "yyyy-MM-dd HH:mm:ss", "");

    assertNull(result);
  }

  @Test
  void convertUtcToGivenFormatReturnsNullWhenDateTimeIsNullAndFormatIsEmpty() {
    String result = DateUtil.convertUTCToGivenFormat(null, "", "GMT+00:00");

    assertNull(result);
  }
}
