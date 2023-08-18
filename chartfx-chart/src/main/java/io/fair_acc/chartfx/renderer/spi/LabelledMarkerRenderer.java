package io.fair_acc.chartfx.renderer.spi;

import java.util.Objects;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.chartfx.ui.css.DataSetStyleParser;
import io.fair_acc.dataset.utils.DataSetStyleBuilder;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Orientation;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.dataset.DataSet;

/**
 * Draws horizontal markers with horizontal (default) labels attached at the top.
 * If the labels are to close together, overlapping label texts are hidden.
 * For markers without any label text, add labels with the empty string {@code ("")}.
 *
 * Points without any label data are ignored by the renderer.
 */
public class LabelledMarkerRenderer extends AbstractRendererXY<LabelledMarkerRenderer> implements Renderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LabelledMarkerRenderer.class);
    private static final String STYLE_CLASS_LABELLED_MARKER = "chart-labelled-marker";
    private static final String DEFAULT_FONT = "Helvetica";
    private static final int DEFAULT_FONT_SIZE = 18;
    private static final Color DEFAULT_GRID_LINE_COLOR = Color.GREEN;
    private static final double DEFAULT_GRID_LINE_WIDTH = 1;
    private static final double[] DEFAULT_GRID_DASH_PATTERM = { 3.0, 3.0 };
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

    /**
     * Draws horizontal markers with horizontal (default) labels attached to the top
     *
     * @param gc the graphics context from the Canvas parent
     * @param chart instance of the calling chart
     * @param dataSet instance of the data set that is supposed to be drawn
     * @param indexMin minimum index of data set to be drawn
     * @param indexMax maximum index of data set to be drawn
     */
    protected void drawHorizontalLabelledMarker(final GraphicsContext gc, final XYChart chart, final DataSet dataSet,
            final int indexMin, final int indexMax) {
        final Axis yAxis = this.getFirstAxis(Orientation.VERTICAL, chart);

        gc.save();
        setGraphicsContextAttributes(gc, dataSet.getStyle());
        gc.setTextAlign(TextAlignment.RIGHT);

        final double width = chart.getCanvas().getWidth();
        double lastLabel = -Double.MAX_VALUE;
        double lastFontSize = 0;
        for (int i = indexMin; i < indexMax; i++) {
            final double screenY = (int) yAxis.getDisplayPosition(dataSet.get(DataSet.DIM_Y, i));
            final String label = dataSet.getDataLabel(i);
            if (label == null) {
                continue;
            }

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
     * @param gc the graphics context from the Canvas parent
     * @param chart instance of the calling chart
     * @param dataSet instance of the data set that is supposed to be drawn
     * @param indexMin minimum index of data set to be drawn
     * @param indexMax maximum index of data set to be drawn
     */
    protected void drawVerticalLabelledMarker(final GraphicsContext gc, final XYChart chart, final DataSet dataSet,
            final int indexMin, final int indexMax) {
        Axis xAxis = this.getFirstAxis(Orientation.HORIZONTAL);
        if (xAxis == null) {
            xAxis = chart.getFirstAxis(Orientation.HORIZONTAL);
        }
        if (xAxis == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.atWarn().addArgument(LabelledMarkerRenderer.class.getSimpleName()).log("{}::drawVerticalLabelledMarker(...) getFirstAxis(HORIZONTAL) returned null skip plotting");
            }
            return;
        }

        gc.save();
        setGraphicsContextAttributes(gc, dataSet.getStyle());
        gc.setTextAlign(TextAlignment.LEFT);

        final double height = chart.getCanvas().getHeight();
        double lastLabel = -Double.MAX_VALUE;
        double lastFontSize = 0;
        for (int i = indexMin; i < indexMax; i++) {
            final double screenX = (int) xAxis.getDisplayPosition(dataSet.get(DataSet.DIM_X, i));
            final String label = dataSet.getDataLabel(i);
            if (label == null) {
                continue;
            }

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

    public LabelledMarkerRenderer enableHorizontalMarker(final boolean state) {
        horizontalMarkerProperty().set(state);
        return getThis();
    }

    public LabelledMarkerRenderer enableVerticalMarker(final boolean state) {
        verticalMarkerProperty().set(state);
        return getThis();
    }

    @Override
    protected LabelledMarkerRenderer getThis() {
        return this;
    }

    public BooleanProperty horizontalMarkerProperty() {
        return horizontalMarker;
    }

    public boolean isHorizontalMarker() {
        return horizontalMarkerProperty().get();
    }

    public boolean isVerticalMarker() {
        return verticalMarkerProperty().get();
    }

    @Override
    protected void render(final GraphicsContext gc, final DataSet dataSet, final DataSetNode style) {

        // check for potentially reduced data range we are supposed to plot
        final int indexMin = Math.max(0, dataSet.getIndex(DataSet.DIM_X, xMin));
        final int indexMax = Math.min(dataSet.getIndex(DataSet.DIM_X, xMax) + 1,
                dataSet.getDataCount());

        // return if zero length data set
        if (indexMax - indexMin <= 0) {
            return;
        }

        if (isHorizontalMarker()) {
            // draw horizontal marker
            drawHorizontalLabelledMarker(gc, getChart(), dataSet, indexMin, indexMax);
        }

        if (isVerticalMarker()) {
            // draw vertical marker
            drawVerticalLabelledMarker(gc, getChart(), dataSet, indexMin, indexMax);
        }

    }

    protected void setGraphicsContextAttributes(final GraphicsContext gc, final String style) {
        if (!styleParser.tryParse(style)) {
            return;
        }
        styleParser.getStrokeColor().ifPresent(gc::setStroke);
        styleParser.getFillColor().ifPresent(gc::setFill);
        styleParser.getLineWidth().ifPresent(gc::setLineWidth);
        styleParser.getFont().ifPresent(gc::setFont);
        styleParser.getLineDashPattern().ifPresent(gc::setLineDashes);
    }

    public final LabelledMarkerRenderer updateCSS() {
        // TODO add/complete CSS parser

        // parse CSS based definitions
        // find definition for STYLE_CLASS_LABELLED_MARKER
        // parse
        strokeColorMarker = LabelledMarkerRenderer.DEFAULT_GRID_LINE_COLOR;
        strokeLineWidthMarker = LabelledMarkerRenderer.DEFAULT_GRID_LINE_WIDTH;
        strokeDashPattern = LabelledMarkerRenderer.DEFAULT_GRID_DASH_PATTERM;

        //if (getStyle() != null) {
        //    parse user-specified marker
        //}

        return getThis();
    }

    public BooleanProperty verticalMarkerProperty() {
        return verticalMarker;
    }

    private static final DataSetStyleParser styleParser = DataSetStyleParser.newInstance();

}
