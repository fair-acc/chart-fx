package de.gsi.chart.renderer.spi.financial.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarUtils {
    /**
     * Create the calendar interval instance by date interval pattern:
     * yyyy/MM/dd-yyyy/MM/dd
     * for example: 2017/12/01-2017/12/22
     *
     * @param dateIntervalPattern String
     * @return calendar interval instance
     * @throws ParseException parsing fails
     */
    public static Interval<Calendar> createByDateInterval(String dateIntervalPattern) throws ParseException {
        if (dateIntervalPattern == null) {
            throw new ParseException("The resource date interval pattern is null", -1);
        }
        String[] parts = dateIntervalPattern.split("-");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        List<Calendar> calendarList = new ArrayList<>();
        for (String time : parts) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(time));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            calendarList.add(cal);
        }

        return new Interval<>(calendarList.get(0), calendarList.get(1));
    }

    /**
     * Create the calendar interval instance by datetime interval pattern:
     * yyyy/MM/dd HH:mm-yyyy/MM/dd HH:mm
     * for example: 2017/12/01 15:30-2017/12/22 22:15
     *
     * @param datetimeIntervalPattern String
     * @return calendar interval instance
     * @throws ParseException parsing fails
     */
    public static Interval<Calendar> createByDateTimeInterval(String datetimeIntervalPattern) throws ParseException {
        if (datetimeIntervalPattern == null) {
            throw new ParseException("The resource datetime interval pattern is null", -1);
        }
        String[] parts = datetimeIntervalPattern.split("-");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        List<Calendar> calendarList = new ArrayList<>();
        for (String time : parts) {
            Date fromTotime = sdf.parse(time);
            Calendar cal = Calendar.getInstance();
            cal.setTime(fromTotime);
            cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE),
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0);
            calendarList.add(cal);
        }

        return new Interval<>(calendarList.get(0), calendarList.get(1));
    }

    /**
     * Create the calendar interval instance by time interval pattern:
     * HH:mm-HH:mm
     * for example: 15:30-22:15
     *
     * @param timeIntervalPattern String
     * @return calendar interval instance
     * @throws ParseException parsing fails
     */
    public static Interval<Calendar> createByTimeInterval(String timeIntervalPattern) throws ParseException {
        if (timeIntervalPattern == null) {
            throw new ParseException("The resource time interval pattern is null", -1);
        }
        String[] parts = timeIntervalPattern.split("-");
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        List<Calendar> calendarList = new ArrayList<>();
        for (String time : parts) {
            Date fromTotime = sdf.parse(time);
            Calendar cal = Calendar.getInstance();
            cal.setTime(fromTotime);
            cal.set(1900, Calendar.JANUARY, 1, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0);
            calendarList.add(cal);
        }

        return new Interval<>(calendarList.get(0), calendarList.get(1));
    }

    /**
     * Create the calendar instance by datetime pattern:
     * yyyy/MM/dd HH:mm
     * for example: 2017/12/01 15:30
     *
     * @param datetimePattern String
     * @return calendar interval instance
     * @throws ParseException parsing fails
     */
    public static Calendar createByDateTime(String datetimePattern) throws ParseException {
        if (datetimePattern == null) {
            throw new ParseException("The resource datetime pattern is null", -1);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        Date fromTotime = sdf.parse(datetimePattern);
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromTotime);
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0);

        return cal;
    }
}
