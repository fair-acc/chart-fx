/*****************************************************************************
 *                                                                           *
 * BI Common - convert Number <-> String                                     *
 *                                                                           *
 * modified: 2017-03-07 Harald Braeuning                                     *
 *                                                                           *
 ****************************************************************************/

package de.gsi.chart.utils;

import java.util.GregorianCalendar;

import javafx.util.StringConverter;

/**
 *
 * @author braeun
 */
public class TimeStringConverter extends StringConverter<Number> {

    private final GregorianCalendar calendar = new GregorianCalendar();

    public TimeStringConverter() {
    }

    @Override
    public Number fromString(String string) {
        throw new UnsupportedOperationException("Converting from string not supported yet");
    }

    @Override
    public String toString(Number object) {
        calendar.setTimeInMillis(object.longValue());
        return String.format("%tT", calendar);
    }

}
