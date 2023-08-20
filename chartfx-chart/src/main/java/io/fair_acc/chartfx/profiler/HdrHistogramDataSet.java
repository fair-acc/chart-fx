package io.fair_acc.chartfx.profiler;

import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.spi.AbstractDataSet;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;
import org.HdrHistogram.DoubleHistogram;
import org.HdrHistogram.DoubleHistogramIterationValue;
import org.HdrHistogram.DoublePercentileIterator;

/**
 * Experimental dataSet for storing hdr histogram data
 *
 * @author ennerf
 */
class HdrHistogramDataSet extends AbstractDataSet<HdrHistogramDataSet> {

    public HdrHistogramDataSet(String name) {
        super(name, 2);
        histogram.setAutoResize(true);
    }

    @Override
    public double get(int dimIndex, int index) {
        switch (dimIndex) {
            case DIM_X:
                return x.getDouble(index);
            case DIM_Y:
                return y.getDouble(index);
            default:
                return Double.NaN;
        }
    }

    public void add(double value) {
        FXUtils.assertJavaFxThread();
        histogram.recordValue(value);
        getAxisDescription(DIM_Y).add(value);
        getAxisDescription(DIM_X).fireInvalidated(ChartBits.DataSetRange);
        fireInvalidated(ChartBits.DataSetDataAdded);
    }

    @Override
    public DataSet recomputeLimits(final int dimIndex) {
        if (getBitState().isDirty(ChartBits.DataSetRange)) {
            convertHistogramToXY();
        }
        return getThis();
    }

    public void convertHistogramToXY() {

        // Convert to displayable x/y values
        x.clear();
        y.clear();
        var it = new DoublePercentileIterator(histogram, 50);
        while (it.hasNext()) {
            var value = it.next();
            x.add(convertPercentileToX(value));
            y.add(value.getValueIteratedTo());
        }

        // Update X range
        var xDescription = getAxisDescription(DIM_X);
        if (!x.isEmpty()) {
            double min = x.getDouble(0);
            double max = x.getDouble(x.size() - 1);
            xDescription.set(min, max);
        } else {
            xDescription.clear();
        }
        xDescription.getBitState().clear();

        // Update Y range
        var yDescription = getAxisDescription(DIM_Y);
        yDescription.set(histogram.getMinValue(), histogram.getMaxValue());
        yDescription.getBitState().clear();
        getBitState().clear(ChartBits.DataSetRange);
    }

    /**
     * x = 1 / (1 - percentage)
     */
    public static double convertPercentileToX(DoubleHistogramIterationValue value) {
        double percentileLevel = value.getPercentileLevelIteratedTo();
        if (percentileLevel == 100d) {
            // 100 results in NaN. We cap it to total count minus one so that
            // the chart x range stays within the possible data set resolution
            // (e.g. not 6 9s when we only have 5 values)
            double totalCount = value.getTotalCountToThisValue();
            percentileLevel = 100d * ((totalCount - 1d) / totalCount);
        }

        return convertPercentileToX(percentileLevel);
    }

    public static double convertPercentileToX(double percentileLevel) {
        return 1 / (1.0D - (percentileLevel / 100.0D));
    }

    public static double convertPercentileFromX(double x) {
        return 100d - (100d / x);
    }

    public void clear() {
        x.clear();
        y.clear();
        histogram.reset();
        for (AxisDescription axisDescription : getAxisDescriptions()) {
            axisDescription.clear();
        }
    }

    @Override
    public int getDataCount() {
        return x.size();
    }

    @Override
    public DataSet set(DataSet other, boolean copy) {
        throw new UnsupportedOperationException();
    }

    protected DoubleArrayList x = new DoubleArrayList(1000);
    protected DoubleArrayList y = new DoubleArrayList(1000);

    protected final DoubleHistogram histogram = new DoubleHistogram(2);

}
