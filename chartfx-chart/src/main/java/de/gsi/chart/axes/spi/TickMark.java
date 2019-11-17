package de.gsi.chart.axes.spi;

import de.gsi.chart.ui.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * TickMark represents the label text, its associated tick mark value and position along the axis for each tick.
 *
 * @author rstein
 */
public class TickMark extends Text {
    private final Side side; // tick mark side (LEFT, TOP; RIGHT; BOTTOM)
    private double tickValue; // tick mark in data units
    private double tickPosition; // tick position along axis in display units
    private double tickRotation; // tick mark rotation (here: centre axis)

    /**
     * Creates and initialises an instance of TickMark.
     *
     * @param side where tick mark is supposed to be drawn (N.B. controls text alignment together with rotation)
     * @param tickValue numeric value of tick
     * @param tickPosition position of tick in canvas pixel coordinates
     * @param tickMarkLabel string label associated with tick
     */
    public TickMark(final Side side, final double tickValue, final double tickPosition, final double tickRotation,
            final String tickMarkLabel) {
        super();
        this.side = side;
        this.tickValue = tickValue;
        this.tickPosition = tickPosition;
        this.tickRotation = tickRotation;
        setText(tickMarkLabel);
        setRotate(tickRotation);
        recomputeAlignment(); // NOPMD may be overwritten in user-code
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TickMark)) {
            return false;
        }
        final TickMark other = (TickMark) obj;

        return (tickPosition == other.tickPosition) && (tickRotation == other.tickRotation)
                && (tickValue == other.tickValue);
    }

    /**
     * @return the height of the tick mark including rotation etc.
     */
    public double getHeight() {
        // N.B. important: usage of getBoundsInParent() which also takes into
        // account text rotations
        return getBoundsInParent().getHeight();
    }

    /**
     * @return value tick position along the axis in display units
     */
    public double getPosition() {
        return tickPosition;
    }

    /**
     * @return value tick rotation
     */
    public double getRotation() {
        return tickRotation;
    }

    /**
     * @return side of axis where tick mark is supposed to be drawn (N.B. controls text alignment together with
     *         rotation)
     */
    public Side getSide() {
        return side;
    }

    /**
     * @return tick mark value in data units
     */
    public double getValue() {
        return tickValue;
    }

    /**
     * @return the width of the tick mark including rotation etc.
     */
    public double getWidth() {
        // N.B. important: usage of getBoundsInParent() which also takes into
        // account text rotations
        return getBoundsInParent().getWidth();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;

        temp = Double.doubleToLongBits(tickPosition);
        result = (prime * result) + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(tickRotation);
        result = (prime * result) + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(tickValue);
        result = (prime * result) + (int) (temp ^ (temp >>> 32));
        final String text = getText();
        result = (prime * result) + ((text == null) ? 0 : text.hashCode());

        return result;
    }

    public void recomputeAlignment() {
        // normalise rotation to [-360, +360]
        final int rotation = ((int) getRotation() % 360);
        switch (side) {
        case TOP:
            setTextAlignment(TextAlignment.CENTER);
            setTextOrigin(VPos.BOTTOM);
            //special alignment treatment if axes labels are to be rotated
            if ((rotation != 0) && ((rotation % 90) == 0)) {
                setTextAlignment(TextAlignment.LEFT);
                setTextOrigin(VPos.CENTER);
            } else if ((rotation % 90) != 0) {
                // pivoting point to left-bottom label corner
                setTextAlignment(TextAlignment.LEFT);
                setTextOrigin(VPos.BOTTOM);
            }
            break;
        case BOTTOM:
        case CENTER_HOR:
            setTextAlignment(TextAlignment.CENTER);
            setTextOrigin(VPos.TOP);
            // special alignment treatment if axes labels are to be rotated
            if ((rotation != 0) && ((rotation % 90) == 0)) {
                setTextAlignment(TextAlignment.LEFT);
                setTextOrigin(VPos.CENTER);
            } else if ((rotation % 90) != 0) {
                // pivoting point to left-top label corner
                setTextAlignment(TextAlignment.LEFT);
                setTextOrigin(VPos.TOP);
            }
            break;
        case LEFT:
            setTextAlignment(TextAlignment.RIGHT);
            setTextOrigin(VPos.CENTER);
            // special alignment treatment if axes labels are to be rotated
            if ((rotation != 0) && ((rotation % 90) == 0)) {
                setTextAlignment(TextAlignment.CENTER);
                setTextOrigin(VPos.BOTTOM);
            }
            break;
        case RIGHT:
        case CENTER_VER:
            setTextAlignment(TextAlignment.LEFT);
            setTextOrigin(VPos.CENTER);
            // special alignment treatment if axes labels are to be rotated
            if ((rotation != 0) && ((rotation % 90) == 0)) {
                setTextAlignment(TextAlignment.CENTER);
                setTextOrigin(VPos.TOP);
            }
            break;
        default:
        }
    }

    /**
     * @param value tick position along the axis in display units
     */
    public void setPosition(final double value) {
        tickPosition = value;
    }

    /**
     * @param value tick rotation
     */
    public void setRotation(final double value) {
        tickRotation = value;
        setRotate(tickRotation);
        recomputeAlignment();
    }

    /**
     * @param newValue tick mark value in data units
     */
    public void setValue(final Double newValue) {
        tickValue = newValue;
    }
}
