package io.fair_acc.dataset.spi;

import io.fair_acc.dataset.event.AddedDataEvent;
import io.fair_acc.dataset.event.EventListener;
import io.fair_acc.dataset.event.UpdateEvent;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetMetaData;
import io.fair_acc.dataset.GridDataSet;
import io.fair_acc.dataset.events.ChartBits;

/**
 * Reduces 3D data to 2D DataSet either via slicing, min, mean, max or integration
 *
 * @author rstein
 */
public class DimReductionDataSet extends DoubleDataSet implements EventListener {
    /**
     * The possible reduction options if integrated over a value range
     *
     * @author rstein
     */
    public enum Option {
        MIN,
        MEAN,
        MAX,
        INTEGRAL,
        SLICE
    }

    private static final long serialVersionUID = 1L;
    private final Option reductionOption;
    private final GridDataSet source;
    private final int dimIndex;
    private int minIndex;
    private int maxIndex;
    private double minValue;
    private double maxValue;

    /**
     * Reduces 3D grid data to 2D DataSet either via slicing, min, mean, max or integration
     *
     * @param source 3D DataSet to take projections from
     * @param dimIndex the axis index onto which the projection should be performed (ie. DIM_X &lt;-&gt; integrate over
     *            the Y axis within given value ranges and vice versa)
     * @param reductionOption one of the reduction options given in {@link Option}
     */
    public DimReductionDataSet(final GridDataSet source, final int dimIndex, final Option reductionOption) {
        super(source.getName() + "-" + reductionOption + "-dim" + dimIndex);

        this.source = source;
        this.dimIndex = dimIndex;
        this.reductionOption = reductionOption;

        this.source.addListener(this);
    }

    public int getMaxIndex() {
        return maxIndex;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public int getMinIndex() {
        return minIndex;
    }

    public double getMinValue() {
        return minValue;
    }

    public Option getReductionOption() {
        return reductionOption;
    }

    /**
     * @return source data set
     */
    public DataSet getSourceDataSet() {
        return source;
    }

    @Override
    public void handle(UpdateEvent event) {
        lock().writeLockGuard(() -> source.lock().readLockGuard(() -> {
            this.getWarningList().clear();
            if (source instanceof DataSetMetaData) {
                this.getWarningList().addAll(((DataSetMetaData) source).getWarningList());
            }
            if (source.getDimension() != 3 || source.getNGrid() != 2) {
                this.getWarningList().add("input data set not 3 dim grid data set");
                return;
            }
            // recompute min/max indices based on actual new value range
            minIndex = source.getGridIndex(dimIndex == DIM_X ? DIM_Y : DIM_X, minValue);
            maxIndex = source.getGridIndex(dimIndex == DIM_X ? DIM_Y : DIM_X, maxValue);

            switch (reductionOption) {
            case MIN:
                updateMinMax(true);
                break;
            case MAX:
                updateMinMax(false);
                break;
            case MEAN:
                updateMeanIntegral(true);
                break;
            case INTEGRAL:
                updateMeanIntegral(false);
                break;
            case SLICE:
            default:
                updateSlice();
                break;
            }
        }));

        this.fireInvalidated(ChartBits.DataSetDataAdded);
    }

    public void setMaxValue(final double val) {
        lock().writeLockGuard(() -> maxValue = val);
        this.handle(new UpdateEvent(this, "changed indexMax"));
    }

    public void setMinValue(double val) {
        lock().writeLockGuard(() -> minValue = val);
        this.handle(new UpdateEvent(this, "changed indexMin"));
    }

    public void setRange(final double min, final double max) {
        lock().writeLockGuard(() -> {
            minValue = min;
            maxValue = max;
        });
        this.handle(new UpdateEvent(this, "changed indexMin indexMax"));
    }

    protected void updateMeanIntegral(final boolean isMean) {
        final int min = Math.min(minIndex, maxIndex);
        final int max = Math.max(Math.max(minIndex, maxIndex), min + 1);
        this.clearData();
        final int nDataCount = source.getShape(dimIndex);
        for (int index = 0; index < nDataCount; index++) {
            final double x = source.getGrid(dimIndex, index);
            double integral = 0.0;
            double nSlices = 0.0;
            for (int i = min; i <= Math.min(max, nDataCount - 1); i++) {
                if (dimIndex == DataSet.DIM_Y) {
                    integral += source.get(DIM_Z, i, index);
                } else {
                    integral += source.get(DIM_Z, index, i);
                }
                nSlices += 1.0;
            }
            if (isMean) {
                this.add(x, nSlices == 0.0 ? Double.NaN : (integral / nSlices));
            } else {
                this.add(x, integral);
            }
        }
    }

    protected void updateMinMax(final boolean isMin) {
        final int min = Math.min(minIndex, maxIndex);
        final int max = Math.max(Math.max(minIndex, maxIndex), min + 1);
        this.clearData();
        final int nDataCount = source.getShape(dimIndex);
        for (int index = 0; index < nDataCount; index++) {
            final double x = source.get(dimIndex, index);
            double ret;
            if (dimIndex == DataSet.DIM_Y) {
                ret = source.get(DIM_Z, min, index);
            } else {
                ret = source.get(DIM_Z, index, min);
            }
            for (int i = min + 1; i <= Math.min(max, nDataCount - 1); i++) {
                final double val;
                if (dimIndex == DataSet.DIM_Y) {
                    val = source.get(DIM_Z, i, index);
                } else {
                    val = source.get(DIM_Z, index, i);
                }
                ret = isMin ? Math.min(val, ret) : Math.max(val, ret);
            }
            this.add(x, ret);
        }
    }

    protected void updateSlice() {
        this.clearData();
        final int nDataCount = source.getShape(dimIndex);
        for (int index = 0; index < nDataCount; index++) {
            final double x = source.getGrid(dimIndex, index);
            final double y;
            if (dimIndex == DataSet.DIM_Y) {
                y = source.get(DIM_Z, minIndex, index);
            } else {
                y = source.get(DIM_Z, index, minIndex);
            }
            this.add(x, y);
        }
    }
}
