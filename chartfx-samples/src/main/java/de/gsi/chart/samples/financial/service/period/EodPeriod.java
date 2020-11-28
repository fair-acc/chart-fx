package de.gsi.chart.samples.financial.service.period;

public class EodPeriod extends Period {

  public static final EodPeriod DAILY = new EodPeriod();

  public enum PeriodEnum {
    DAILY, WEEKLY, MONTHLY
  }

  private final PeriodEnum period;

  public EodPeriod() {
    this(PeriodEnum.DAILY);
  }

  public EodPeriod(PeriodEnum period) {
    this.period = period;
  }

  public PeriodEnum getPeriod() {
    return period;
  }

  @Override
  public long getMillis() {
      switch (period) {
      case DAILY:
          return 24 * 60 * 60 * 1000;
      case WEEKLY:
          return 7 * 24 * 60 * 60 * 1000;
      case MONTHLY:
          return 2592000000L;
      default:
          throw new IllegalArgumentException("The method getMillis() is not supported for this type of period: " + this);
      }
  }

@Override
  public String toString() {
    return period.toString();
  }

}
