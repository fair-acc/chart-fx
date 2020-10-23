package de.gsi.microservice.concepts.aggregate.filter;

import java.util.Objects;
import java.util.function.Predicate;

import de.gsi.microservice.concepts.aggregate.Filter;
import de.gsi.microservice.concepts.aggregate.RingBufferEvent;

public class CustomFilter implements Filter {
    public String filterName;
    public Predicate<RingBufferEvent> userFilter;
    public Boolean result;

    @Override
    public void clear() {
        result = null;
    }

    @Override
    public void copyTo(final Filter o) {
        if (!(o instanceof CustomFilter)) {
            return;
        }
        final CustomFilter other = (CustomFilter) o;
        other.result = result;
        other.filterName = filterName;
        other.userFilter = userFilter;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomFilter)) {
            return false;
        }
        final CustomFilter other = (CustomFilter) o;
        if (userFilter != other.userFilter) {
            return false;
        }
        return Objects.equals(filterName, other.filterName) && Objects.equals(result, other.result);
    }

    @Override
    public int hashCode() {
        int result1 = filterName != null ? filterName.hashCode() : 0;
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        return result1;
    }

    @Override
    public String toString() {
        return '[' + CustomFilter.class.getSimpleName() + ": filterName=" + filterName + " result='" + result + "'";
    }

    public static Predicate<CustomFilter> test(RingBufferEvent rbEvt) {
        return evt -> evt.result == null ? (evt.result = evt.userFilter.test(rbEvt)) : evt.result;
    }
}
