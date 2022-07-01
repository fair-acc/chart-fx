package io.fair_acc.chartfx.renderer.spi;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.ImageView;

import io.fair_acc.chartfx.renderer.ContourType;
import io.fair_acc.chartfx.renderer.datareduction.ReductionType;
import io.fair_acc.chartfx.renderer.spi.utils.ColorGradient;

public abstract class AbstractContourDataSetRendererParameter<R extends AbstractContourDataSetRendererParameter<R>>
        extends AbstractPointReductionManagment<R> {
    private final BooleanProperty altImplementation = new SimpleBooleanProperty(this, "altImplementation", false);
    private final IntegerProperty reductionFactorX = new SimpleIntegerProperty(this, "reductionFactorX", 2);
    private final IntegerProperty reductionFactorY = new SimpleIntegerProperty(this, "reductionFactorY", 2);
    private final ObjectProperty<ReductionType> reductionType = new SimpleObjectProperty<>(this, "reductionType",
            ReductionType.AVERAGE);
    private final ObjectProperty<ColorGradient> colorGradient = new SimpleObjectProperty<>(this, "colorGradient",
            ColorGradient.DEFAULT);

    private final BooleanProperty computeLocalZRange = new SimpleBooleanProperty(this, "computeLocalZRange", true);
    private final ObjectProperty<ContourType> contourType = new SimpleObjectProperty<>(this, "contourType",
            ContourType.HEATMAP);

    /**
     * suppresses contour segments being drawn that have more than the specified number of sub-segments
     */
    private final IntegerProperty maxContourSegments = new SimpleIntegerProperty(this, "maxContourSegments", 500) {
        @Override
        public void set(int newValue) {
            super.set(Math.max(2, newValue));
        }
    };

    private final IntegerProperty minHexTileSize = new SimpleIntegerProperty(this, "minHexTileSize", 5) {
        @Override
        public void set(int newValue) {
            super.set(Math.max(2, newValue));
        }
    };

    private final IntegerProperty quantisationLevels = new SimpleIntegerProperty(this, "quantisationLevels", 20) {
        @Override
        public void set(int newValue) {
            super.set(Math.max(2, newValue));
        }
    };

    private final BooleanProperty smooth = new SimpleBooleanProperty(this, "smooth", false) {
        @Override
        protected void invalidated() {
            // requestChartLayout();
        }
    };

    public AbstractContourDataSetRendererParameter() {
        super();
        setMinRequiredReductionSize(3);
    }

    /**
     * Property to track internal alternate implementation. This is used to compare different implementation and to
     * potentially fall-back to an older reference implementation
     *
     * @return altImplementation property
     */
    public BooleanProperty altImplementationProperty() {
        return altImplementation;
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
     * Returns the value of the {@link #computeLocalRangeProperty()}.
     *
     * @return {@code true} if the local range calculation is applied, {@code false} otherwise
     */
    public boolean computeLocalRange() {
        return computeLocalRangeProperty().get();
    }

    /**
     * Indicates if the chart should compute the min/max z-Axis for the local (true) or global (false) visible range
     *
     * @return computeLocalRange property
     */
    public BooleanProperty computeLocalRangeProperty() {
        return computeLocalZRange;
    }

    /**
     * Indicates if the chart should plot contours (true) or color gradient map (false)
     *
     * @return plotContourProperty property
     */
    public ObjectProperty<ContourType> contourTypeProperty() {
        return contourType;
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
     * Returns the value of the {@link #contourTypeProperty()}.
     *
     * @return if the chart should plot contours (true) or color gradient map (false)
     */
    public ContourType getContourType() {
        return contourTypeProperty().get();
    }

    /**
     * @return the maximum number of segments for which a contour is being drawn
     */
    public int getMaxContourSegments() {
        return maxContourSegmentsProperty().get();
    }

    public int getMinHexTileSizeProperty() {
        return minHexTileSizeProperty().get();
    }

    public int getNumberQuantisationLevels() {
        return quantisationLevelsProperty().get();
    }

    public int getReductionFactorX() {
        return reductionFactorXProperty().get();
    }

    public int getReductionFactorY() {
        return reductionFactorYProperty().get();
    }

    public ReductionType getReductionType() {
        return reductionTypeProperty().get();
    }

    /**
     * This is used to compare different implementation and to potentially fall-back to an older reference
     * implementation
     *
     * @return {@code true} if alternate implementation is being used
     */
    public boolean isAltImplementation() {
        return altImplementationProperty().get();
    }

    /**
     * Returns the value of the {@link #smoothProperty()}.
     *
     * @return {@code true} if the smoothing should be applied, {@code false} otherwise
     */
    public boolean isSmooth() {
        return smoothProperty().get();
    }

    /**
     * @return the property controlling the maximum number of sub-segments allowed for a contour to be drawn.
     */
    public IntegerProperty maxContourSegmentsProperty() {
        return maxContourSegments;
    }

    public IntegerProperty minHexTileSizeProperty() {
        return minHexTileSize;
    }

    public IntegerProperty quantisationLevelsProperty() {
        return quantisationLevels;
    }

    public IntegerProperty reductionFactorXProperty() {
        return reductionFactorX;
    }

    public IntegerProperty reductionFactorYProperty() {
        return reductionFactorY;
    }

    public ObjectProperty<ReductionType> reductionTypeProperty() {
        return reductionType;
    }

    /**
     * This is used to compare different implementation and to potentially fall-back to an older reference
     * implementation
     *
     * @param state {@code true} if alternate implementation is being used
     */
    public void setAltImplementation(final boolean state) {
        altImplementationProperty().set(state);
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
     * Sets the value of the {@link #computeLocalRangeProperty()}.
     *
     * @param value {@code true} if the local range calculation is applied, {@code false} otherwise
     */
    public void setComputeLocalRange(final boolean value) {
        computeLocalRangeProperty().set(value);
    }

    /**
     * Sets the value of the {@link #contourTypeProperty()}.
     *
     * @param value if the chart should plot contours (true) or color gradient map (false)
     */
    public void setContourType(final ContourType value) {
        contourTypeProperty().set(value);
    }

    /**
     * suppresses contour segments being drawn that have more than the specified number of sub-segments
     *
     * @param nSegments the maximum number of segments
     */
    public void setMaxContourSegments(final int nSegments) {
        maxContourSegmentsProperty().set(nSegments);
    }

    public void setMinHexTileSizeProperty(final int minSize) {
        minHexTileSizeProperty().set(minSize);
    }

    public void setNumberQuantisationLevels(final int nQuantisation) {
        quantisationLevelsProperty().set(nQuantisation);
    }

    public void setReductionFactorX(final int factor) {
        reductionFactorXProperty().set(factor);
    }

    public void setReductionFactorY(final int factor) {
        reductionFactorYProperty().set(factor);
    }

    public void setReductionType(final ReductionType value) {
        reductionTypeProperty().set(value);
    }

    /**
     * Sets the value of the {@link #smoothProperty()}.
     *
     * @param value {@code true} to enable smoothing
     */
    public void setSmooth(final boolean value) {
        smoothProperty().set(value);
    }

    /**
     * Indicates if the chart should smooth colors between data points or render each data point as a rectangle with
     * uniform color.
     * <p>
     * By default smoothing is disabled.
     * </p>
     *
     * @return smooth property
     * @see ImageView#setFitWidth(double)
     * @see ImageView#setFitHeight(double)
     * @see ImageView#setSmooth(boolean)
     */
    public BooleanProperty smoothProperty() {
        return smooth;
    }
}
