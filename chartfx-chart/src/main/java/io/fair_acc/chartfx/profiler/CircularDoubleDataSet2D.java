package io.fair_acc.chartfx.profiler;

import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.spi.AbstractDataSet;
import io.fair_acc.dataset.utils.DoubleCircularBuffer;

/**
 * experimental dataset for ringbuffer data
 *
 * @author ennerf
 */
class CircularDoubleDataSet2D extends AbstractDataSet<CircularDoubleDataSet2D> {

    public CircularDoubleDataSet2D(String name, int capacity) {
        super(name, 2);
        x = new DoubleCircularBuffer(capacity);
        y = new DoubleCircularBuffer(capacity);
    }

    @Override
    public double get(int dimIndex, int index) {
        switch (dimIndex) {
            case DIM_X:
                return x.get(index);
            case DIM_Y:
                return y.get(index);
            default:
                return Double.NaN;
        }
    }

    public void add(double x, double y) {
        FXUtils.assertJavaFxThread();
        this.x.put(x);
        this.y.put(y);
        getAxisDescription(DIM_X).add(x);
        getAxisDescription(DIM_Y).add(y);
        fireInvalidated(ChartBits.DataSetData);
    }

    @Override
    public int getDataCount() {
        return x.available();
    }

    public void clear() {
        x.reset();
        y.reset();
        for (AxisDescription axisDescription : getAxisDescriptions()) {
            axisDescription.clear();
        }
    }

    @Override
    public DataSet set(DataSet other, boolean copy) {
        throw new UnsupportedOperationException();
    }

    protected final DoubleCircularBuffer x;
    protected final DoubleCircularBuffer y;

}
