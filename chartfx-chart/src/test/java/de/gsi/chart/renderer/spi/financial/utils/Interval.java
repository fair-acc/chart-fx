package de.gsi.chart.renderer.spi.financial.utils;

public class Interval<T> {
    public T from;
    public T to;

    public Interval(T from, T to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((from == null) ? 0 : from.hashCode());
        result = prime * result + ((to == null) ? 0 : to.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Interval other = (Interval) obj;
        if (from == null) {
            if (other.from != null)
                return false;
        } else if (!from.equals(other.from))
            return false;
        if (to == null) {
            return other.to == null;
        } else
            return to.equals(other.to);
    }

    @Override
    public String toString() {
        return "Interval [from=" + from + ", to=" + to + "]";
    }
}
