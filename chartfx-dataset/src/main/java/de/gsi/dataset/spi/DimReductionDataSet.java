package de.gsi.dataset.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.UpdateEvent;

/**
 * Reduces 3D data to 2D DataSet either via slicing, min, mean, max or integration
 * 
 * @author rstein
 */
public class DimReductionDataSet extends DoubleDataSet implements EventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DimReductionDataSet.class);
    private static final long serialVersionUID = 1L;
    private final Option reductionOption;
    private final DataSet3D source;
    private final int dimIndex;
    private int indexMin;
    private int indexMax;

    public DimReductionDataSet(final DataSet3D source, final int dimIndex, final Option reductionOption) {
        super(source.getName() + "-" + reductionOption + "-dim" + dimIndex);

        this.source = source;
        this.dimIndex = dimIndex;
        this.reductionOption = reductionOption;

        this.source.addListener(this);
    }

    public int getMaxIndex(final int dimIndex) {
        return indexMax;
    }

    public int getMinIndex(final int dimIndex) {
        return indexMin;
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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(event).log("handle({})");
        }
        source.lock().readLockGuard(() -> lock().writeLockGuard(() -> {
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
        this.fireInvalidated(new AddedDataEvent(this,
                "updated " + DimReductionDataSet.class.getSimpleName() + " name = " + this.getName()));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(event).log("handle({}) - done");
        }
    }

    public void setMaxIndex(final int dimIndex, double val) {
        lock().writeLockGuard(() -> {
            indexMax = source.getIndex(dimIndex, val);
        });
        this.handle(new UpdateEvent(this, "changed indexMax"));
    }

    public void setMinIndex(final int dimIndex, double val) {
        lock().writeLockGuard(() -> {
            indexMin = source.getIndex(dimIndex, val);
        });
        this.handle(new UpdateEvent(this, "changed indexMax"));
    }

    protected void updateMeanIntegral(final boolean isMean) {
        final int min = Math.min(indexMin, indexMax);
        final int max = Math.max(Math.max(indexMin, indexMax), min + 1);
        this.clearData();
        final int nDataCount = source.getDataCount(dimIndex);
        if (dimIndex == DataSet.DIM_Y) {
            for (int index = 0; index < nDataCount; index++) {
                final double x = source.get(dimIndex, index);
                double integral = 0.0;
                double nSlices = 0.0;
                for (int i = min; i <= Math.min(max, nDataCount - 1); i++) {
                    integral += source.getZ(i, index);
                    nSlices += 1.0;
                }
                this.add(x, isMean ? (nSlices == 0.0 ? Double.NaN : (integral / nSlices)) : integral);
            }
        } else {
            for (int index = 0; index < nDataCount; index++) {
                final double x = source.get(dimIndex, index);
                double integral = 0.0;
                double nSlices = 0.0;
                for (int i = min; i <= Math.min(max, nDataCount - 1); i++) {
                    integral += source.getZ(index, i);
                    nSlices += 1.0;
                }
                this.add(x, isMean ? (nSlices == 0.0 ? Double.NaN : (integral / nSlices)) : integral);
            }
        }
    }

    protected void updateMinMax(final boolean isMin) {
        final int min = Math.min(indexMin, indexMax);
        final int max = Math.max(Math.max(indexMin, indexMax), min + 1);
        this.clearData();
        final int nDataCount = source.getDataCount(dimIndex);
        if (dimIndex == DataSet.DIM_Y) {
            for (int index = 0; index < nDataCount; index++) {
                final double x = source.get(dimIndex, index);
                double ret = source.getZ(min, index);
                for (int i = min + 1; i <= Math.min(max, nDataCount - 1); i++) {
                    final double val = source.getZ(i, index);
                    ret = isMin ? Math.min(val, ret) : Math.max(val, ret);
                }
                this.add(x, ret);
            }
        } else {
            for (int index = 0; index < nDataCount; index++) {
                final double x = source.get(dimIndex, index);
                double ret = source.getZ(index, min);
                for (int i = min + 1; i <= Math.min(max, nDataCount - 1); i++) {
                    final double val = source.getZ(index, i);
                    ret = isMin ? Math.min(val, ret) : Math.max(val, ret);
                }
                this.add(x, ret);
            }
        }
    }

    protected void updateSlice() {
        this.clearData();
        final int nDataCount = source.getDataCount(dimIndex);
        if (dimIndex == DataSet.DIM_Y) {
            for (int index = 0; index < nDataCount; index++) {
                final double x = source.get(dimIndex, index);
                final double y = source.getZ(indexMin, index);
                this.add(x, y);
            }
        } else {
            for (int index = 0; index < nDataCount; index++) {
                final double x = source.get(dimIndex, index);
                final double y = source.getZ(index, indexMin);
                this.add(x, y);
            }
        }
    }

    public enum Option {
        MIN,
        MEAN,
        MAX,
        INTEGRAL,
        SLICE;
    }
}
