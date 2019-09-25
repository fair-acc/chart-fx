package de.gsi.chart.renderer.spi;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.renderer.Renderer;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.utils.NoDuplicatesList;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;

/**
 * @author rstein
 * @param <R>
 *            renderer generics
 */
public abstract class AbstractDataSetManagement<R extends Renderer> implements Renderer {
    private final ObservableList<DataSet> datasets = FXCollections.observableArrayList();
    protected BooleanProperty showInLegend = new SimpleBooleanProperty(this, "showInLegend", true);

    private final ObservableList<Axis> axesList = FXCollections.observableList(new NoDuplicatesList<Axis>());

    @Override
    public ObservableList<Axis> getAxes() {
        return axesList;
    }

    @Override
    public ObservableList<DataSet> getDatasets() {
        return datasets;
    }

    @Override
    public ObservableList<DataSet> getDatasetsCopy() {
        return getDatasetsCopy(getDatasets());
    }

    public Axis getFirstAxis(final Orientation orientation) {
        for (final Axis axis : getAxes()) {
            if (axis.getSide() == null) {
                continue;
            }
            switch (orientation) {
            case VERTICAL:
                if (axis.getSide().isVertical()) {
                    return axis;
                }
                break;
            case HORIZONTAL:
            default:
                if (axis.getSide().isHorizontal()) {
                    return axis;
                }
                break;
            }
        }
        return null;
    }

    /**
     * Sets whether DataSets attached to this renderer shall be shown in the
     * legend
     *
     * @param state
     *            true (default) if data sets are supposed to be drawn
     * @return the renderer class
     */
    @Override
    public R setShowInLegend(final boolean state) {
        showInLegend.set(state);
        return getThis();
    }

    /**
     * Sets whether DataSets attached to this renderer shall be shown in the
     * legend
     *
     * @return true (default) if data sets are supposed to be drawn
     */
    @Override
    public boolean showInLegend() {
        return showInLegend.get();
    }

    /**
     * Sets whether DataSets attached to this renderer shall be shown in the
     * legend
     *
     * @return true (default) if data sets are supposed to be drawn
     */
    @Override
    public final BooleanProperty showInLegendProperty() {
        return showInLegend;
    }

    protected ObservableList<DataSet> getDatasetsCopy(final ObservableList<DataSet> localDataSets) {
        final long start = ProcessingProfiler.getTimeStamp();
        final ObservableList<DataSet> dataSets = FXCollections.observableArrayList();
        for (final DataSet dataSet : localDataSets) {
            if (dataSet instanceof DataSetError) {
                final DataSetError dataSetError = (DataSetError) dataSet;
                dataSets.add(AbstractDataSetManagement.getErrorDataSetCopy(dataSetError));
            } else {
                dataSets.add(AbstractDataSetManagement.getDataSetCopy(dataSet));
            }
        }
        ProcessingProfiler.getTimeDiff(start);
        return dataSets;
    }

    /**
     * @return the instance of this AbstractDataSetManagement.
     */
    protected abstract R getThis();

    protected static void copyMetaData(final DataSet from, final DataSet to) {
        to.setStyle(from.getStyle());
    }

    protected static final DoubleDataSet getDataSetCopy(final DataSet dataSet) {
        final int nLength = dataSet.getDataCount(DIM_X); //TODO: expand to n-dimensional DataSet
        final DoubleDataSet ret = new DoubleDataSet(dataSet.getName(), nLength);

        dataSet.lock().writeLockGuard(() -> {

            if (dataSet instanceof DoubleDataSet) {
                final DoubleDataSet doubleDataSet = (DoubleDataSet) dataSet;
                // known data set implementation, may use faster array copy

                final double[] xValues = doubleDataSet.getXValues();
                final double[] yValues = doubleDataSet.getYValues();
                ret.set(xValues, yValues);

                ret.getDataLabelMap().putAll(doubleDataSet.getDataLabelMap());
                ret.getDataStyleMap().putAll(doubleDataSet.getDataStyleMap());
            } else {
                // generic implementation that works with all DataSetError
                // implementation
                for (int i = 0; i < nLength; i++) {
                    ret.set(i, dataSet.get(DIM_X, i), dataSet.get(DIM_Y, i));

                    final String label = dataSet.getDataLabel(i);
                    if (label != null) {
                        ret.getDataLabelMap().put(i, label);
                    }
                    final String style = ret.getDataLabel(i);
                    if (style != null) {
                        ret.getDataStyleMap().put(i, style);
                    }
                }
            }
            AbstractDataSetManagement.copyMetaData(dataSet, ret);
        });
        ret.fireInvalidated(new UpdatedDataEvent(dataSet, "copy"));
        return ret;
    }

    protected static final DoubleErrorDataSet getErrorDataSetCopy(final DataSetError dataSet) {
        final int nLength = dataSet.getDataCount(DIM_X);
        final DoubleErrorDataSet ret = new DoubleErrorDataSet(dataSet.getName(), nLength);

        dataSet.lock().writeLockGuard(() -> {
            if (dataSet instanceof DoubleErrorDataSet) {
                final DoubleErrorDataSet doubleErrorDataSet = (DoubleErrorDataSet) dataSet;
                // known data set implementation, may use faster array copy

                final double[] xValues = doubleErrorDataSet.getXValues();
                final double[] yValues = doubleErrorDataSet.getYValues();
                final double[] yErrorsNeg = doubleErrorDataSet.getErrorsNegative(DIM_Y);
                final double[] yErrorsPos = doubleErrorDataSet.getErrorsPositive(DIM_Y);
                ret.set(xValues, yValues, yErrorsNeg, yErrorsPos);

                ret.getDataLabelMap().putAll(doubleErrorDataSet.getDataLabelMap());
                ret.getDataStyleMap().putAll(doubleErrorDataSet.getDataStyleMap());
            } else {
                // generic implementation that works with all DataSetError
                // implementation
                for (int i = 0; i < nLength; i++) {
                    ret.set(i, dataSet.get(DIM_X, i), dataSet.get(DIM_Y, i), dataSet.getErrorNegative(DIM_Y, i),
                            dataSet.getErrorPositive(DIM_Y, i));
                    final String label = ret.getDataLabel(i);
                    if (label != null) {
                        ret.getDataLabelMap().put(i, label);
                    }
                    final String style = ret.getDataLabel(i);
                    if (style != null) {
                        ret.getDataStyleMap().put(i, style);
                    }
                }
            }
            AbstractDataSetManagement.copyMetaData(dataSet, ret);
        });
        ret.fireInvalidated(new UpdatedDataEvent(dataSet, "copy"));

        return ret;
    }

}
