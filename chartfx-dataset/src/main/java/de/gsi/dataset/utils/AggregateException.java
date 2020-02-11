package de.gsi.dataset.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple aggregation of exceptions (used for catching exceptions of parallel executions)
 *
 * @author rstein
 */
public class AggregateException extends RuntimeException {
    private static final long serialVersionUID = 1580760362088821678L;
    private final List<Throwable> throwableList = Collections.synchronizedList(new ArrayList<>());

    public AggregateException(final String message) {
        this(message, Collections.emptyList());
    }

    public AggregateException(final String message, final List<Throwable> throwables) {
        super(message, throwables.isEmpty() ? null : throwables.get(0));
        throwableList.addAll(throwables);
    }

    /**
     * @return the throwableList
     */
    public List<Throwable> getThrowableList() {
        return throwableList;
    }

}
