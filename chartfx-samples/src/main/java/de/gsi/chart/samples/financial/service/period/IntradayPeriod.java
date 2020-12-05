package de.gsi.chart.samples.financial.service.period;

public class IntradayPeriod extends Period {
    public enum IntradayPeriodEnum {
        T, // ticks
        S, // seconds
        M, // minutes
        H, // hours
        RB, // range bars
        V // volume
    }

    private final IntradayPeriodEnum period;
    private final double periodValue;
    private final Double minimalMoveSymbol;
    private final boolean extendedCalculation;
    private final String calculationAddonServicesType;

    public IntradayPeriod(IntradayPeriodEnum period, double periodValue) {
        super(PeriodType.INTRA);
        this.period = period;
        this.periodValue = periodValue;
        this.minimalMoveSymbol = null;
        this.extendedCalculation = false;
        this.calculationAddonServicesType = null; // not used
    }

    public IntradayPeriod(IntradayPeriodEnum period, double periodValue, Double minimalMoveSymbol,
            boolean extendedCalculation, String calculationAddonServicesType) {
        super(PeriodType.INTRA);
        this.period = period;
        this.periodValue = periodValue;
        this.minimalMoveSymbol = minimalMoveSymbol;
        this.extendedCalculation = extendedCalculation;
        this.calculationAddonServicesType = calculationAddonServicesType;
    }

    public IntradayPeriodEnum getPeriod() {
        return period;
    }

    public double getPeriodValue() {
        return periodValue;
    }

    /**
     * @return provides type of ADDONs for OHLC calculation services
     */
    public String getCalculationAddonServicesType() {
        return calculationAddonServicesType;
    }

    /**
     * @return minimal move of market is necessary for range bars
     */
    public Double getMinimalMoveSymbol() {
        return minimalMoveSymbol;
    }

    /**
     * @return defines calculation of extended bid ask volumes for order flow
     */
    public boolean isExtendedCalculation() {
        return extendedCalculation;
    }

    @Override
    public long getMillis() {
        switch (period) {
        case S:
            return 1000 * Math.round(periodValue);
        case M:
            return 60 * 1000 * Math.round(periodValue);
        case H:
            return 60 * 60 * 1000 * Math.round(periodValue);

        default:
            return 60 * 1000;
            //throw new IllegalArgumentException("The method getMillis() is not supported for this type of period: " + this);
        }
    }

    @Override
    public String toString() {
        return periodValue + period.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((period == null) ? 0 : period.hashCode());
        long temp;
        temp = Double.doubleToLongBits(periodValue);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        IntradayPeriod other = (IntradayPeriod) obj;
        if (period != other.period)
            return false;
        if (Double.doubleToLongBits(periodValue) != Double.doubleToLongBits(other.periodValue))
            return false;
        return true;
    }

    public static IntradayPeriod convert(String periodString) {
        if (periodString == null || "".equals(periodString)) {
            return new IntradayPeriod(IntradayPeriodEnum.T, 1);
        }
        String periodSymbol = periodString.substring(periodString.length() - 1);
        double periodValue = Double.parseDouble(periodString.substring(0, periodString.length() - 1));

        return new IntradayPeriod(IntradayPeriodEnum.valueOf(periodSymbol), periodValue);
    }
}
