package io.fair_acc.chartfx.profiler;

import io.fair_acc.chartfx.axes.AxisLabelOverlapPolicy;
import io.fair_acc.chartfx.axes.spi.AxisRange;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

/**
 * @author ennerf
 */
class PercentileAxis extends DefaultNumericAxis {

    PercentileAxis() {
        super("Percentile");
        setUnit(null);
        setAxisLabelGap(10);
        setLogAxis(true);
        setLogarithmBase(10);
        setMinorTickCount(10);
        setForceZeroInRange(true);
        setOverlapPolicy(AxisLabelOverlapPolicy.SKIP_ALT);

        setTickLabelFormatter(new StringConverter<Number>() {

            @Override
            public String toString(Number object) {
                return labelCache.computeIfAbsent(object, number -> {
                    String str = String.valueOf(HdrHistogramDataSet.convertPercentileFromX(number.doubleValue()));
                    if (str.endsWith(".0")) {
                        str = str.substring(0, str.length() - 2);
                    }
                    return str + "%";
                });
            }

            @Override
            public Number fromString(String string) {
                throw new IllegalStateException("not implemented");
            }

        });

    }

    protected AxisRange computeRange(final double min, final double max, final double axisLength, final double labelSize) {
        return super.computeRange(min, max + 5, axisLength, labelSize);
    }

    @Override
    protected void calculateMajorTickValues(AxisRange axisRange,  DoubleArrayList tickValues) {
        precomputeTicks(axisRange.getUpperBound());
        final double min = axisRange.getLowerBound();
        final double max = axisRange.getUpperBound();
        majorTicks.forEach(tick -> {
            if (tick >= min && tick <= max) {
                tickValues.add(tick);
            }
        });
    }

    @Override
    protected void calculateMinorTickValues(DoubleArrayList tickValues) {
        if (getMinorTickCount() <= 0 || getTickUnit() <= 0) {
            return;
        }
        final double min = getMin();
        final double max = getMax();
        minorTicks.forEach(tick -> {
            if (tick >= min && tick <= max) {
                tickValues.add(tick);
            }
        });
    }

    private void precomputeTicks(double maxValue) {
        // fast path: skip
        if (majorTicks.size() > 0 && maxValue <= majorTicks.getDouble(majorTicks.size() - 1)) {
            return;
        }

        // re-compute ticks
        majorTicks.clear();
        minorTicks.clear();
        majorTicks.add(HdrHistogramDataSet.convertPercentileToX(0));
        majorTicks.add(HdrHistogramDataSet.convertPercentileToX(25));
        double value = 1;
        while (value < maxValue) {

            majorTicks.add(value * 2); // --------> 50%
            minorTicks.add(value * 3); // 66.666%
            majorTicks.add(value * 4); // --------> 75%
            minorTicks.add(value * 5); // 80%
            minorTicks.add(value * 6); // 83.333%
            minorTicks.add(value * 7); // 85.714%
            minorTicks.add(value * 8); // 87.5%
            minorTicks.add(value * 9); // 88.888%
            majorTicks.add(value * 10); // -------> 90%

            value *= 10;
        }

    }

    DoubleArrayList majorTicks = new DoubleArrayList();
    DoubleArrayList minorTicks = new DoubleArrayList();

    private static WeakHashMap<Number, String> labelCache = new WeakHashMap<>();

}
