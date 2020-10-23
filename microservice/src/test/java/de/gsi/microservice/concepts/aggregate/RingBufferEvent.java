package de.gsi.microservice.concepts.aggregate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;

import de.gsi.microservice.utils.SharedPointer;

public class RingBufferEvent implements FilterPredicate, Cloneable {
    private final static Logger LOGGER = LoggerFactory.getLogger(RingBufferEvent.class);

    /**
     * local UTC event arrival time-stamp [ms]
     */
    public long arrivalTimeStamp;

    /**
     * reference to the parent's disruptor sequence ID number
     */
    public long parentSequenceNumber;

    /**
     * list of known filters. N.B. this
     */
    public final Filter[] filters;
    private final Class<? extends Filter>[] filterConfig;

    /**
     * domain object carried by this ring buffer event
     */
    public SharedPointer<Object> payload;

    /**
     * collection of exceptions that have been issued while handling this RingBuffer event
     */
    public final List<Throwable> throwables = new ArrayList<>();

    /**
     *
     * @param filterConfig static filter configuration
     */
    @SafeVarargs
    public RingBufferEvent(final Class<? extends Filter>... filterConfig) {
        assert filterConfig != null;
        this.filterConfig = filterConfig;
        this.filters = new Filter[filterConfig.length];
        for (int i = 0; i < filters.length; i++) {
            try {
                filters[i] = filterConfig[i].getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalArgumentException("filter initialisations error - could not instantiate class:" + filterConfig[i], e);
            }
        }
        clear();
    }

    @Override
    public RingBufferEvent clone() { // NOSONAR NOPMD we do not want to call super (would be kind of stupid)
        final RingBufferEvent retVal = new RingBufferEvent(this.filterConfig);
        retVal.arrivalTimeStamp = arrivalTimeStamp;
        retVal.parentSequenceNumber = parentSequenceNumber;
        for (int i = 0; i < retVal.filters.length; i++) {
            filters[i].copyTo(retVal.filters[i]);
        }
        retVal.payload = payload.getCopy();
        retVal.throwables.addAll(throwables);
        return retVal;
    }

    public void copyTo(RingBufferEvent other) {
        other.arrivalTimeStamp = arrivalTimeStamp;
        other.parentSequenceNumber = parentSequenceNumber;
        for (int i = 0; i < other.filters.length; i++) {
            filters[i].copyTo(other.filters[i]);
        }
        other.payload = payload.getCopy();
        other.throwables.addAll(throwables);
    }

    public <T extends Filter> T getFilter(final Class<T> filterType) {
        for (Filter filter : filters) {
            if (filter.getClass().isAssignableFrom(filterType)) {
                return filterType.cast(filter);
            }
        }
        final StringBuilder builder = new StringBuilder();
        builder.append("requested filter type '").append(filterType.getSimpleName()).append(" not part of ").append(RingBufferEvent.class.getSimpleName()).append(" definition: ");
        printToStringArrayList(builder, "[", "]", (Object[]) filters);
        throw new IllegalArgumentException(builder.toString());
    }

    public boolean matches(final Predicate<RingBufferEvent> predicate) {
        return predicate.test(this);
    }

    public <T extends Filter> boolean matches(Class<T> filterType, final Predicate<T> predicate) {
        return predicate.test(filterType.cast(getFilter(filterType)));
    }

    //    public <T> boolean test(Class<T> filterType, final Predicate<T> predicate) {
    //        return predicate.test(filterType.cast(getFilter(filterType)));
    //    }

    public final void clear() {
        arrivalTimeStamp = 0L;
        parentSequenceNumber = -1L;
        for (Filter filter : filters) {
            filter.clear();
        }
        throwables.clear();
        if (payload != null) {
            payload.release();
        }
        payload = null;
    }

    @Override
    public <R extends Filter> boolean test(final Class<R> filterClass, final Predicate<R> filterPredicate) {
        return filterPredicate.test(filterClass.cast(getFilter(filterClass)));
    }

    public String toString() {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        final StringBuilder builder = new StringBuilder();
        builder.append(RingBufferEvent.class.getSimpleName()).append(": arrivalTimeStamp ").append(arrivalTimeStamp).append(" (").append(sdf.format(arrivalTimeStamp)).append(") parent sequence number: ").append(parentSequenceNumber).append(" - filter: ");
        printToStringArrayList(builder, "[", "]", (Object[]) filters);
        if (!throwables.isEmpty()) {
            builder.append(" - exceptions (n=").append(throwables.size()).append("):\n");
            for (Throwable t : throwables) {
                builder.append(getPrintableStackTrace(t)).append('\n');
            }
        }
        return builder.toString();
    }

    public static void printToStringArrayList(final StringBuilder builder, final String prefix, final String postFix, final Object... items) {
        if (prefix != null && !prefix.isBlank()) {
            builder.append(prefix);
        }
        boolean more = false;
        for (Object o : items) {
            if (more) {
                builder.append(", ");
            }
            builder.append(o.getClass().getSimpleName()).append(':').append(o.toString());
            more = true;
        }
        if (postFix != null && !postFix.isBlank()) {
            builder.append(postFix);
        }
        //TODO: refactor into a common utility class
    }

    public static String getPrintableStackTrace(final Throwable t) {
        if (t == null) {
            return "<null stack trace>";
        }
        final StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
        //TODO: refactor into a common utility class
    }

    /**
     * default buffer element clearing handler
     */
    public static class ClearEventHandler implements EventHandler<RingBufferEvent> {
        public void onEvent(RingBufferEvent event, long sequence, boolean endOfBatch) {
            LOGGER.atTrace().addArgument(sequence).addArgument(endOfBatch).log("clearing RingBufferEvent sequence = {} endOfBatch = {}");
            event.clear();
        }
    }
}
