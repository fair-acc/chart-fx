package io.fair_acc.chartfx.renderer.spi;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.AxisRange;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.DataSet;

import java.util.List;

/**
 * Renderer that uses 3 Axes.
 * <p>
 * Note: inherits from AbstractPointReducingRenderer instead of AbstractRendererXY to
 * not break required parameters for ContourRenderer.
 *
 * @author ennerf
 */
public abstract class AbstractRendererXYZ<R extends AbstractRendererXYZ<R>> extends AbstractPointReducingRenderer<R> {

    @Override
    public void updateAxes() {
        super.updateAxes();

        // Check if there is a user-specified 3rd axis
        if (zAxis == null) {
            zAxis = tryGetZAxis(getAxes(), false);
        }

        // Fallback to one from the chart
        if (zAxis == null) {
            zAxis = tryGetZAxis(getChart().getAxes(), true);
        }

        // Fallback to adding one to the chart (to match behavior of X and Y)
        if (zAxis == null) {
            zAxis = createZAxis();
            getChart().getAxes().add(zAxis);
        }
    }

    @Override
    public void updateAxisRange(Axis axis, AxisRange range) {
        super.updateAxisRange(axis, range);
        if (axis == zAxis) {
            updateAxisRange(range, DataSet.DIM_Z);
        }
    }

    private Axis tryGetZAxis(List<Axis> axes, boolean requireDimZ) {
        Axis firstNonXY = null;
        for (Axis axis : axes) {
            if (axis != xAxis && axis != yAxis) {
                // Prefer DIM_Z if possible
                if (axis.getDimIndex() == DataSet.DIM_Z) {
                    return axis;
                }

                // Potentially allow the first unused one
                if (firstNonXY == null) {
                    firstNonXY = axis;
                }
            }
        }
        return requireDimZ ? null : firstNonXY;
    }

    public static DefaultNumericAxis createZAxis() {
        var zAxis = new DefaultNumericAxis("z-Axis");
        zAxis.setAnimated(false);
        zAxis.setSide(Side.RIGHT);
        zAxis.setDimIndex(DataSet.DIM_Z);
        return zAxis;
    }

    public void shiftZAxisToLeft() {
        zAxis.setSide(Side.LEFT);
    }

    public void shiftZAxisToRight() {
        zAxis.setSide(Side.RIGHT);
    }

    protected Axis zAxis;

}
