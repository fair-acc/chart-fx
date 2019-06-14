package de.gsi.chart.renderer.spi;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.XYChartCss;
import de.gsi.chart.axes.Axis;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.utils.ProcessingProfiler;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.utils.StyleParser;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class LabelledMarkerRenderer extends AbstractDataSetManagement<LabelledMarkerRenderer> implements Renderer {
    private static final String STYLE_CLASS_LABELLED_MARKER = "chart-labelled-marker";
    private static final String DEFAULT_FONT = "Helvetia";
    private static final int DEFAULT_FONT_SIZE = 18;
    private static final Color DEFAULT_GRID_LINE_COLOR = Color.GREEN;
    private static final double DEFAULT_GRID_LINE_WIDTH = 1;
    private static final double[] DEFAULT_GRID_DASH_PATTERM = { 3.0, 3.0 };
    protected final StringProperty style = new SimpleStringProperty(this, "style", null);
    protected final BooleanProperty verticalMarker = new SimpleBooleanProperty(this, "verticalMarker", true);
    protected final BooleanProperty horizontalMarker = new SimpleBooleanProperty(this, "horizontalMarker", false);
    protected Paint strokeColorMarker = LabelledMarkerRenderer.DEFAULT_GRID_LINE_COLOR;
    protected double strokeLineWidthMarker = LabelledMarkerRenderer.DEFAULT_GRID_LINE_WIDTH;
    protected double[] strokeDashPattern = LabelledMarkerRenderer.DEFAULT_GRID_DASH_PATTERM;

    public LabelledMarkerRenderer() {
        super();
        updateCSS(); // NOPMD by rstein on 13/06/19 14:25
        setShowInLegend(false);
    }

    @Override
    protected LabelledMarkerRenderer getThis() {
        return this;
    }

    @Override
    public void render(final GraphicsContext gc, final Chart chart, final int dataSetOffset,
            final ObservableList<DataSet> datasets) {
        final long start = ProcessingProfiler.getTimeStamp();
        if (!(chart instanceof XYChart)) {
            throw new InvalidParameterException(
                    "must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
        }
        final XYChart xyChart = (XYChart) chart;

        // make local copy and add renderer specific data sets
        final List<DataSet> localDataSetList = new ArrayList<>(datasets);
        // N.B. only render data sets that are directly attached to this
        // renderer
        localDataSetList.addAll(getDatasets());

        // If there are no data sets
        if (localDataSetList.isEmpty()) {
            return;
        }

        if (!(xyChart.getXAxis() instanceof Axis)) {
            throw new InvalidParameterException("x-Axis must be a derivative of Axis, axis is = " + xyChart.getXAxis());
        }

        final Axis xAxis = xyChart.getXAxis();
        final double xAxisWidth = xAxis.getWidth();
        final double xMin = xAxis.getValueForDisplay(0);
        final double xMax = xAxis.getValueForDisplay(xAxisWidth);

        // N.B. importance of reverse order: start with last index, so that
        // most(-like) important DataSet is drawn on top of the others

        for (int dataSetIndex = localDataSetList.size() - 1; dataSetIndex >= 0; dataSetIndex--) {
            final DataSet dataSet = localDataSetList.get(dataSetIndex);
            dataSet.lock();

            // check for potentially reduced data range we are supposed to plot
            final int indexMin = Math.max(0, dataSet.getXIndex(xMin));
            final int indexMax = Math.min(dataSet.getXIndex(xMax) + 1, dataSet.getDataCount());

            // return if zero length data set
            if (indexMax - indexMin <= 0) {
                dataSet.unlock();
                continue;
            }

            if (horizontalMarker.get()) {
                // draw horizontal marker
                drawHorizontalLabelledMarker(gc, xyChart, dataSet, indexMin, indexMax);
            }

            if (verticalMarker.get()) {
                // draw vertical marker
                drawVerticalLabelledMarker(gc, xyChart, dataSet, indexMin, indexMax);
            }

            dataSet.unlock();

        } // end of 'dataSetIndex' loop

        ProcessingProfiler.getTimeDiff(start);
    }

    /**
     * Draws horizontal markers with horizontal (default) labels attached to the
     * top
     *
     * @param gc
     *            the graphics context from the Canvas parent
     * @param chart
     *            instance of the calling chart
     * @param dataSet
     *            instance of the data set that is supposed to be drawn
     * @param indexMin
     *            minimum index of data set to be drawn
     * @param indexMax
     *            maximum index of data set to be drawn
     */
    protected void drawHorizontalLabelledMarker(final GraphicsContext gc, final XYChart chart, final DataSet dataSet,
            final int indexMin, final int indexMax) {

        if (!(chart.getYAxis() instanceof Axis)) {
            throw new InvalidParameterException("y Axis not a Axis derivative, yAxis = " + chart.getYAxis());
        }
        final Axis yAxis = chart.getYAxis();

        gc.save();
        setGraphicsContextAttributes(gc, dataSet.getStyle());
        gc.setTextAlign(TextAlignment.RIGHT);

        final double width = chart.getCanvas().getWidth();
        double lastLabel = -Double.MAX_VALUE;
        double lastFontSize = 0;
        for (int i = indexMin; i < indexMax; i++) {
            final double screenY = (int) yAxis.getDisplayPosition(dataSet.getY(i));
            final String label = dataSet.getDataLabel(i);

            final String pointStyle = dataSet.getStyle(i);
            if (pointStyle != null) {
                gc.save();
                setGraphicsContextAttributes(gc, pointStyle);
            }

            gc.strokeLine(0, screenY, width, screenY);

            if (Math.abs(screenY - lastLabel) > lastFontSize && !label.isEmpty()) {
                gc.save();
                gc.setLineWidth(0.8);
                gc.setLineDashes(1.0);
                gc.translate(Math.ceil(screenY + 3), Math.ceil(0.99 * width));
                gc.fillText(label, 0.0, 0);
                gc.restore();
                lastLabel = screenY;
                lastFontSize = gc.getFont().getSize();
            }

            if (pointStyle != null) {
                gc.restore();
            }
        }
        gc.restore();
    }

    /**
     * Draws vertical markers with vertical (default) labels attached to the top
     *
     * @param gc
     *            the graphics context from the Canvas parent
     * @param chart
     *            instance of the calling chart
     * @param dataSet
     *            instance of the data set that is supposed to be drawn
     * @param indexMin
     *            minimum index of data set to be drawn
     * @param indexMax
     *            maximum index of data set to be drawn
     */
    protected void drawVerticalLabelledMarker(final GraphicsContext gc, final XYChart chart, final DataSet dataSet,
            final int indexMin, final int indexMax) {

        if (!(chart.getXAxis() instanceof Axis)) {
            throw new InvalidParameterException("x Axis not a Axis derivative, xAxis = " + chart.getXAxis());
        }
        final Axis xAxis = chart.getXAxis();

        gc.save();
        setGraphicsContextAttributes(gc, dataSet.getStyle());
        gc.setTextAlign(TextAlignment.LEFT);

        final double height = chart.getCanvas().getHeight();
        double lastLabel = -Double.MAX_VALUE;
        double lastFontSize = 0;
        for (int i = indexMin; i < indexMax; i++) {
            final double screenX = (int) xAxis.getDisplayPosition(dataSet.getX(i));
            final String label = dataSet.getDataLabel(i);

            final String pointStyle = dataSet.getStyle(i);

            if (pointStyle != null) {
                gc.save();
                setGraphicsContextAttributes(gc, pointStyle);
            }

            gc.strokeLine(screenX, 0, screenX, height);

            if (Math.abs(screenX - lastLabel) > lastFontSize && !label.isEmpty()) {
                gc.save();
                gc.setLineWidth(0.8);
                gc.setLineDashes(1.0);
                gc.translate(Math.ceil(screenX + 3), Math.ceil(0.01 * height));
                gc.rotate(+90);
                gc.fillText(label, 0.0, 0);
                gc.restore();
                lastLabel = screenX;
                lastFontSize = gc.getFont().getSize();
            }
            if (pointStyle != null) {
                gc.restore();
            }
        }
        gc.restore();
    }

    protected void setGraphicsContextAttributes(final GraphicsContext gc, final String style) {

        final Color strokeColor = StyleParser.getColorPropertyValue(style, XYChartCss.STROKE_COLOR);
        if (strokeColor == null) {
            gc.setStroke(strokeColorMarker);
        } else {
            gc.setStroke(strokeColor);
        }

        final Color fillColor = StyleParser.getColorPropertyValue(style, XYChartCss.FILL_COLOR);
        if (fillColor == null) {
            gc.setFill(strokeColorMarker);
        } else {
            gc.setFill(fillColor);
        }

        final Double strokeWidth = StyleParser.getFloatingDecimalPropertyValue(style, XYChartCss.STROKE_WIDTH);
        if (strokeWidth == null) {
            gc.setLineWidth(strokeLineWidthMarker);
        } else {
            gc.setLineWidth(strokeWidth);
        }

        final Font font = StyleParser.getFontPropertyValue(style);
        if (font == null) {
            gc.setFont(Font.font(LabelledMarkerRenderer.DEFAULT_FONT, LabelledMarkerRenderer.DEFAULT_FONT_SIZE));
        } else {
            gc.setFont(font);
        }

        final double[] dashPattern = StyleParser.getFloatingDecimalArrayPropertyValue(style,
                XYChartCss.STROKE_DASH_PATTERN);
        if (dashPattern == null) {
            gc.setLineDashes(strokeDashPattern);
        } else {
            gc.setLineDashes(dashPattern);
        }

    }

    public BooleanProperty horizontalMarkerProperty() {
        return horizontalMarker;
    }

    public boolean isHorizontalMarker() {
        return horizontalMarker.get();
    }

    public LabelledMarkerRenderer enableHorizontalMarker(final boolean state) {
        horizontalMarker.set(state);
        return getThis();
    }

    public BooleanProperty verticalMarkerProperty() {
        return verticalMarker;
    }

    public boolean isVerticalMarker() {
        return verticalMarker.get();
    }

    public LabelledMarkerRenderer enableVerticalMarker(final boolean state) {
        verticalMarker.set(state);
        return getThis();
    }

    public StringProperty styleProperty() {
        return style;
    }

    public String getStyle() {
        return style.get();
    }

    public LabelledMarkerRenderer setStyle(final String newStyle) {
        style.set(newStyle);
        return getThis();
    }

    // ******************************* CSS Style Stuff *********************

    public final LabelledMarkerRenderer updateCSS() {
        // TODO add/complete CSS parser

        // parse CSS based definitions
        // find definition for STYLE_CLASS_LABELLED_MARKER
        // parse
        strokeColorMarker = LabelledMarkerRenderer.DEFAULT_GRID_LINE_COLOR;
        strokeLineWidthMarker = LabelledMarkerRenderer.DEFAULT_GRID_LINE_WIDTH;
        strokeDashPattern = LabelledMarkerRenderer.DEFAULT_GRID_DASH_PATTERM;

        if (getStyle() != null) {
            // parse user-specified marker
        }

        return getThis();
    }

    @Override
    public Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height) {
        // not applicable
        return null;
    }
}
