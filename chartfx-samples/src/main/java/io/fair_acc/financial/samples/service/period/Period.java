package io.fair_acc.financial.samples.service.period;

public abstract class Period {
    public enum PeriodType {
        EOD,
        INTRA
    }

    private final PeriodType type;

    public Period(PeriodType type) {
        this.type = type;
    }

    public Period() {
        this(PeriodType.EOD);
    }

    /**
     * @return common type of the time period
     */
    public PeriodType getType() {
        return type;
    }

    /**
     * @return get period in millis
     */
    public abstract long getMillis();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        Period other = (Period) obj;
        return type == other.type;
    }
}
