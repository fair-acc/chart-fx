package de.gsi.chart.axes.spi;

import static javafx.scene.paint.CycleMethod.NO_CYCLE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.utils.ColorGradient;
import de.gsi.chart.ui.geometry.Side;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;

/**
 * An Axis with a color gradient e.g. for use with HeatMap plots.
 * By default this axis is excluded from the Zoomer Plugin.
 * TODO:
 * - Fix LEFT, CENTER_HOR/VERT
 * - Reduce Boilerplate Code
 * - Allow free Positioning? e.g legend style, outside of chart, ...
 *
 * @author Alexander Krimm
 */
public class ColorGradientAxis extends DefaultNumericAxis {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColorGradientAxis.class);
    protected final Rectangle gradientRect = new Rectangle();

    private final ObjectProperty<ColorGradient> colorGradient = new SimpleObjectProperty<>(this, "colorGradient",
            ColorGradient.DEFAULT);

    private final DoubleProperty gradientWidth = new SimpleDoubleProperty(this, "gradientWidth", 20);

    /**
     * @param lowerBound
     * @param upperBound
     * @param tickUnit
     */
    public ColorGradientAxis(double lowerBound, double upperBound, double tickUnit) {
        super(lowerBound, upperBound, tickUnit);
        this.colorGradient.addListener((p, o, n) -> this.forceRedraw());
        this.getProperties().put(Zoomer.ZOOMER_OMIT_AXIS, Boolean.TRUE);
    }

    public ColorGradientAxis(double lowerBound, double upperBound, double tickUnit, ColorGradient colorGradient) {
        this(lowerBound, upperBound, tickUnit);
        this.colorGradient.set(colorGradient);
    }

    /**
     * @param axisLabel
     */
    public ColorGradientAxis(String axisLabel) {
        super(axisLabel);
        this.colorGradient.addListener((p, o, n) -> this.forceRedraw());
        this.getProperties().put(Zoomer.ZOOMER_OMIT_AXIS, Boolean.TRUE);
    }

    public ColorGradientAxis(String axisLabel, ColorGradient colorGradient) {
        this(axisLabel);
        this.colorGradient.set(colorGradient);
    }

    /**
     * @param axisLabel
     * @param lowerBound
     * @param upperBound
     * @param tickUnit
     */
    public ColorGradientAxis(String axisLabel, double lowerBound, double upperBound, double tickUnit) {
        super(axisLabel, lowerBound, upperBound, tickUnit);
        this.colorGradient.addListener((p, o, n) -> this.forceRedraw());
        this.getProperties().put(Zoomer.ZOOMER_OMIT_AXIS, Boolean.TRUE);
    }

    public ColorGradientAxis(String axisLabel, double lowerBound, double upperBound, double tickUnit,
            ColorGradient colorGradient) {
        this(axisLabel, lowerBound, upperBound, tickUnit);
        this.colorGradient.set(colorGradient);
    }

    /**
     * @param axisLabel
     * @param unit
     */
    public ColorGradientAxis(String axisLabel, String unit) {
        super(axisLabel, unit);
        this.colorGradient.addListener((p, o, n) -> this.forceRedraw());
        this.getProperties().put(Zoomer.ZOOMER_OMIT_AXIS, Boolean.TRUE);
    }

    public ColorGradientAxis(String axisLabel, String unit, ColorGradient colorGradient) {
        this(axisLabel, unit);
        this.colorGradient.set(colorGradient);
    }

    @Override
    public void drawAxis(final GraphicsContext gc, final double axisWidth, final double axisHeight) {
        if ((gc == null) || (getSide() == null)) {
            return;
        }

        clearAxisCanvas(gc, axisWidth, axisHeight); // seems not to work?

        drawAxisPre();

        // update CSS data
        updateCSS();
        final double axisLength = getSide().isHorizontal() ? axisWidth : axisHeight;

        // draw colorBar
        drawAxisLine(gc, axisLength, axisWidth, axisHeight);

        // translate 
        final double gradientWidth = getGradientWidth();
        gc.save();
        switch (getSide()) {
        case LEFT:
            gc.translate(-gradientWidth, 0);
            break;
        case RIGHT:
            gc.translate(gradientWidth, 0);
            break;
        case TOP:
            gc.translate(0, -gradientWidth);
            break;
        case BOTTOM:
            gc.translate(0, gradientWidth);
            break;
        case CENTER_HOR:
            gc.translate(gradientWidth, 0);
            break;
        case CENTER_VER:
            gc.translate(-gradientWidth, 0);
            break;
        default:
            break;
        }
        if (!isTickMarkVisible()) {
            // draw axis title w/o major TickMark
            drawAxisLabel(gc, axisWidth, axisHeight, getAxisLabel(), null, getTickLength());
            drawAxisPost();
            return;
        }

        final ObservableList<TickMark> majorTicks = getTickMarks();
        final ObservableList<TickMark> minorTicks = getMinorTickMarks();

        // neededLength assumes tick-mark width of one, needed to suppress minor
        // ticks if tick-mark pixel are overlapping
        final double neededLength = (getTickMarks().size() + minorTicks.size()) * 2.0;
        // Don't draw minor tick marks if there isn't enough space for them!
        if (isMinorTickVisible() && (axisLength > neededLength)) {
            drawTickMarks(gc, axisLength, axisWidth, axisHeight, minorTicks, getMinorTickLength(), getMinorTickStyle());
            drawTickLabels(gc, axisWidth, axisHeight, minorTicks, getMinorTickLength());
        }

        // draw major tick-mark over minor tick-marks so that the visible
        // (long) line along the axis with the style of the major-tick is
        // visible
        drawTickMarks(gc, axisLength, axisWidth, axisHeight, majorTicks, getTickLength(), getMajorTickStyle());
        drawTickLabels(gc, axisWidth, axisHeight, majorTicks, getTickLength());

        // draw axis title
        drawAxisLabel(gc, axisWidth, axisHeight, getAxisLabel(), majorTicks, getTickLength());
        drawAxisPost();
        gc.restore(); // restore colorBar offset
    }

    @Override
    protected void drawAxisLine(final GraphicsContext gc, final double axisLength, final double axisWidth,
            final double axisHeight) {
        // N.B. axis canvas is (by-design) larger by 'padding' w.r.t.
        // required/requested axis length (needed for nicer label placements on
        // border.
        final double paddingX = getSide().isHorizontal() ? getAxisPadding() : 0.0;
        final double paddingY = getSide().isVertical() ? getAxisPadding() : 0.0;
        // for relative positioning of axes drawn on top of the main canvas
        final double axisCentre = getCenterAxisPosition();

        final double gradientWidth = getGradientWidth();

        // save css-styled line parameters
        final Path tickStyle = getMajorTickStyle();
        gc.save();
        gc.setStroke(tickStyle.getStroke());
        gc.setLineWidth(tickStyle.getStrokeWidth());

        if (getSide().isHorizontal()) {
            gc.setFill(new LinearGradient(0, 0, axisLength, 0, false, NO_CYCLE, getColorGradient().getStops()));
        } else {
            gc.setFill(new LinearGradient(0, axisLength, 0, 0, false, NO_CYCLE, getColorGradient().getStops()));
        }

        // N.B. important: translate by padding ie. canvas is +padding larger on
        // all size compared to region
        gc.translate(paddingX, paddingY);

        switch (getSide()) {
        case LEFT:
            // axis line on right side of canvas
            gc.fillRect(snap(axisWidth - gradientWidth), snap(0), snap(axisWidth), snap(axisLength));
            gc.strokeRect(snap(axisWidth - gradientWidth), snap(0), snap(axisWidth), snap(axisLength));
            break;
        case RIGHT:
            // axis line on left side of canvas
            gc.fillRect(snap(0), snap(0), snap(gradientWidth), snap(axisLength));
            gc.strokeRect(snap(0), snap(0), snap(gradientWidth), snap(axisLength));
            break;
        case TOP:
            // line on bottom side of canvas (N.B. (0,0) is top left corner)
            gc.fillRect(snap(0), snap(axisHeight - gradientWidth), snap(axisLength), snap(axisHeight));
            gc.strokeRect(snap(0), snap(axisHeight - gradientWidth), snap(axisLength), snap(axisHeight));
            break;
        case BOTTOM:
            // line on top side of canvas (N.B. (0,0) is top left corner)
            gc.rect(snap(0), snap(0), snap(axisLength), snap(gradientWidth));
            break;
        case CENTER_HOR:
            // axis line at the centre of the canvas
            gc.fillRect(snap(0), axisCentre * axisHeight - 0.5 * gradientWidth, snap(axisLength),
                    snap(axisCentre * axisHeight + 0.5 * gradientWidth));
            gc.strokeRect(snap(0), axisCentre * axisHeight - 0.5 * gradientWidth, snap(axisLength),
                    snap(axisCentre * axisHeight + 0.5 * gradientWidth));
            break;
        case CENTER_VER:
            // axis line at the centre of the canvas
            gc.fillRect(snap(axisCentre * axisWidth - 0.5 * gradientWidth), snap(0),
                    snap(axisCentre * axisWidth + 0.5 * gradientWidth), snap(axisLength));
            gc.strokeRect(snap(axisCentre * axisWidth - 0.5 * gradientWidth), snap(0),
                    snap(axisCentre * axisWidth + 0.5 * gradientWidth), snap(axisLength));
            break;
        default:
            break;
        }
        gc.restore();
    }

    public double getGradientWidth() {
        return gradientWidth.get();
    }

    public void setGradientWidth(final double newGradientWidth) {
        gradientWidth.set(newGradientWidth);
    }

    public DoubleProperty gradientWidthProperty() {
        return gradientWidth;
    }

    /**
     * Color gradient (linear) used to encode data point values.
     *
     * @return gradient property
     */
    public ObjectProperty<ColorGradient> colorGradientProperty() {
        return colorGradient;
    }

    /**
     * Sets the value of the {@link #colorGradientProperty()}.
     *
     * @param value the gradient to be used
     */
    public void setColorGradient(final ColorGradient value) {
        colorGradientProperty().set(value);
    }

    /**
     * Returns the value of the {@link #colorGradientProperty()}.
     *
     * @return the color gradient used for encoding data values
     */
    public ColorGradient getColorGradient() {
        return colorGradientProperty().get();
    }

    /**
     * @param value z-Value, values outside of the visible limit are clamped to the extrema
     * @return the color representing the input value on the z-Axis
     */
    public Color getColor(final double value) {
        final double offset = (value - getRange().getLowerBound())
                / (getRange().getUpperBound() - getRange().getLowerBound());

        double lowerOffset = 0.0;
        double upperOffset = 1.0;
        Color lowerColor = Color.TRANSPARENT;
        Color upperColor = Color.TRANSPARENT;

        for (final Stop stop : getColorGradient().getStops()) {
            final double currentOffset = stop.getOffset();
            if (currentOffset == offset) {
                return stop.getColor();
            } else if (currentOffset < offset) {
                lowerOffset = currentOffset;
                lowerColor = stop.getColor();
            } else {
                upperOffset = currentOffset;
                upperColor = stop.getColor();
                break;
            }
        }

        final double interpolationOffset = (offset - lowerOffset) / (upperOffset - lowerOffset);
        return lowerColor.interpolate(upperColor, interpolationOffset);
    }

    /**
     * Return the color for a value as an integer with the color values in its bytes.
     * For use e.g. with an IntBuffer backed PixelBuffer.
     * 
     * @param value z-Value
     * @return integer with one byte each set to alpha, red, green, blue
     */
    public int getIntColor(final double value) {
        final Color color = getColor(value);
        return ((byte) (color.getOpacity() * 255) << 24) + ((byte) (color.getRed() * 255) << 16)
                + ((byte) (color.getGreen() * 255) << 8) + ((byte) (color.getBlue() * 255));
    }

    @Override
    protected double computePrefHeight(final double width) {
        // add width of the bar
        final Side side = getSide();
        if ((side == null) || (side == Side.CENTER_HOR) || side.isVertical()) {
            return super.computePrefHeight(width);
        }
        return super.computePrefHeight(width) + getGradientWidth();
    }

    @Override
    protected double computePrefWidth(final double height) {
        // add width of the bar
        final Side side = getSide();
        if ((side == null) || (side == Side.CENTER_VER) || side.isHorizontal()) {
            return super.computePrefWidth(height);
        }
        return super.computePrefWidth(height) + getGradientWidth();
    }
}
