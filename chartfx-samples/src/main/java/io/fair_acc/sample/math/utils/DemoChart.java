package io.fair_acc.sample.math.utils;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.DataPointTooltip;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.plugins.TableViewer;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.ui.geometry.Side;

/**
 * Short hand extension/configuration of the standard XYChart functionalities to make the samples more readable
 * 
 * @author rstein
 */
public class DemoChart extends XYChart {
    private final List<DefaultNumericAxis> yAxes = new ArrayList<>();
    private final List<ErrorDataSetRenderer> renderer = new ArrayList<>();

    public DemoChart() {
        this(1);
    }

    public DemoChart(final int nAxes) {
        super(new DefaultNumericAxis("x-axis"), new DefaultNumericAxis("y-axis"));

        if (nAxes <= 0) {
            throw new IllegalArgumentException("nAxes= " + nAxes + " must be >=1");
        }

        ErrorDataSetRenderer defaultRenderer = (ErrorDataSetRenderer) getRenderers().get(0);
        defaultRenderer.setPolyLineStyle(LineStyle.NORMAL);
        defaultRenderer.setErrorStyle(ErrorStyle.ERRORCOMBO);
        renderer.add(defaultRenderer);

        getYAxis().setAutoRangePadding(0.05);
        yAxes.add(getYAxis()); // NOPMD by rstein on 13/06/19 11:25

        for (int i = 1; i < nAxes; i++) {
            DefaultNumericAxis yAxis = new DefaultNumericAxis("y-axis" + i);
            yAxis.setAutoRangePadding(0.05);
            yAxis.setSide(Side.RIGHT);
            yAxes.add(yAxis);

            ErrorDataSetRenderer newRenderer = new ErrorDataSetRenderer();
            newRenderer.getAxes().addAll(getXAxis(), yAxis); // NOPMD by rstein on 13/06/19 11:24
            getRenderers().add(newRenderer);
            renderer.add(newRenderer);
        }

        getPlugins().add(new ParameterMeasurements());
        getPlugins().add(new Zoomer());
        getPlugins().add(new TableViewer());
        getPlugins().add(new EditAxis());
        getPlugins().add(new DataPointTooltip());

        VBox.setVgrow(this, Priority.ALWAYS);
        HBox.setHgrow(this, Priority.ALWAYS);
    }

    public ErrorDataSetRenderer getRenderer() {
        return renderer.get(0);
    }

    public ErrorDataSetRenderer getRenderer(final int index) {
        return renderer.get(index);
    }

    @Override
    public DefaultNumericAxis getXAxis() {
        return (DefaultNumericAxis) super.getXAxis();
    }

    @Override
    public DefaultNumericAxis getYAxis() {
        return (DefaultNumericAxis) super.getYAxis();
    }

    public DefaultNumericAxis getYAxis(final int index) {
        return yAxes.get(index);
    }
}
