package de.gsi.chart.samples.financial.service;

import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarUtils {

    /**
     * Create the calendar interval instance by date interval pattern:
     * yyyy/MM/dd-yyyy/MM/dd
     * for example: 2017/12/01-2017/12/22
     * @param dateIntervalPattern String
     * @return calendar interval instance
     * @throws ParseException parsing fails
     */
    public static Calendar[] createByDateInterval(String dateIntervalPattern) throws ParseException {
        if (dateIntervalPattern == null) {
            throw new ParseException("The resource date interval pattern is null", -1);
        }
        String[] parts = dateIntervalPattern.split("-");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        List<Calendar> calendarList = new ArrayList<>();
        for (String time : parts) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(time));
            calendarList.add(DateUtils.truncate(cal, Calendar.DATE));
        }

        return new Calendar[] { calendarList.get(0), calendarList.get(1) };
    }
}
