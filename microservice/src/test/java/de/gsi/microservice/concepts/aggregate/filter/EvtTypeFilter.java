package de.gsi.microservice.concepts.aggregate.filter;

import java.util.Objects;
import java.util.function.Predicate;

import de.gsi.microservice.concepts.aggregate.Filter;

public class EvtTypeFilter implements Filter {
    public EvtType evtType = EvtType.UNKNOWN;
    public String typeName = null;
    protected int hashCode = 0;

    @Override
    public void clear() {
        hashCode = 0;
        evtType = EvtType.UNKNOWN;
        typeName = null;
    }

    @Override
    public void copyTo(final Filter other) {
        if (!(other instanceof EvtTypeFilter)) {
            return;
        }
        ((EvtTypeFilter) other).hashCode = this.hashCode;
        ((EvtTypeFilter) other).evtType = this.evtType;
        ((EvtTypeFilter) other).typeName = this.typeName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final EvtTypeFilter that = (EvtTypeFilter) o;
        return evtType == that.evtType && Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return hashCode == 0 ? hashCode = Objects.hash(evtType, typeName) : hashCode;
    }

    @Override
    public String toString() {
        return '[' + EvtTypeFilter.class.getSimpleName() + ": evtType=" + evtType + " typeName='" + typeName + "'";
    }

    public enum EvtType {
        TIMING_EVENT,
        DEVICE_DATA,
        SETTING_SUPPLY_DATA,
        PROCESSED_DATA,
        UNKNOWN
    }

    public static Predicate<EvtTypeFilter> isDeviceData() {
        return t -> t.evtType == EvtType.DEVICE_DATA;
    }

    public static Predicate<EvtTypeFilter> isDeviceData(final String typeName) {
        return t -> t.evtType == EvtType.DEVICE_DATA && Objects.equals(t.typeName, typeName);
    }
}
