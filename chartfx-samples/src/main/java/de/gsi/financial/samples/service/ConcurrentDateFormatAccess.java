package de.gsi.financial.samples.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @see SimpleDateFormat is not thread safe. For parallel processing of data streams is necessary to use threadlocal processing.
 */
public class ConcurrentDateFormatAccess {
    private final String simpleDateFormatString;

    public ConcurrentDateFormatAccess(String dateFormatMask) {
        simpleDateFormatString = dateFormatMask;
    }

    public ConcurrentDateFormatAccess() {
        simpleDateFormatString = "MM/dd/yyyy";
    }

    private final ThreadLocal<DateFormat> df = new ThreadLocal<>() {
        @Override
        public DateFormat get() {
            return super.get();
        }

        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat(simpleDateFormatString);
        }

        @Override
        public void remove() {
            super.remove();
        }

        @Override
        public void set(DateFormat value) {
            super.set(value);
        }
    };

    public Date parse(String dateString) throws ParseException {
        return df.get().parse(dateString);
    }

    public String format(Date date) {
        return df.get().format(date);
    }
}
