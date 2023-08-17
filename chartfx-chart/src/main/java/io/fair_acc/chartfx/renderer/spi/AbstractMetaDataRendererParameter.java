package io.fair_acc.chartfx.renderer.spi;

import java.security.InvalidParameterException;

import javafx.beans.property.*;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import io.fair_acc.chartfx.renderer.spi.utils.ChartIconFactory;

public abstract class AbstractMetaDataRendererParameter<R extends AbstractMetaDataRendererParameter<R>> {
    protected static final String STYLE_CLASS_LABELLED_MARKER = "chart-meta-data";
    protected static final String DEFAULT_FONT = "Helvetica";
    protected static final int DEFAULT_FONT_SIZE = 18;
    protected static final Color DEFAULT_GRID_LINE_COLOR = Color.GREEN;
    protected static final double DEFAULT_GRID_LINE_WIDTH = 1;
    protected static final double[] DEFAULT_GRID_DASH_PATTERM = { 3.0, 3.0 };
    protected final StringProperty style = new SimpleStringProperty(this, "style", null);

    protected final IntegerProperty indexOffset = new SimpleIntegerProperty(this, "indexOffset", 0);
    protected Paint strokeColorMarker = AbstractMetaDataRendererParameter.DEFAULT_GRID_LINE_COLOR;
    protected double strokeLineWidthMarker = AbstractMetaDataRendererParameter.DEFAULT_GRID_LINE_WIDTH;
    protected double[] strokeDashPattern = AbstractMetaDataRendererParameter.DEFAULT_GRID_DASH_PATTERM;
    protected Node iconInfo = ChartIconFactory.getInfoIcon();
    protected Node iconWarning = ChartIconFactory.getWarningIcon();
    protected Node iconError = ChartIconFactory.getErrorIcon();
    protected final DoubleProperty iconSize = new SimpleDoubleProperty(this, "drawOnPane", 10.0) {
        @Override
        public void set(double newSize) {
            if (newSize <= 0) {
                throw new InvalidParameterException("size should be >= 0, requested = " + newSize);
            }
            super.set(newSize);

            iconInfo = ChartIconFactory.getInfoIcon(newSize);
            iconWarning = ChartIconFactory.getWarningIcon(newSize);
            iconError = ChartIconFactory.getErrorIcon(newSize);
        }
    };
    protected final BooleanProperty showInfoMessages = new SimpleBooleanProperty(this, "showInfoMessages", true);
    protected final BooleanProperty showWarningMessages = new SimpleBooleanProperty(this, "showWarningMessages", true);
    protected final BooleanProperty showErrorMessages = new SimpleBooleanProperty(this, "showErrorMessages", true);

    public String getStyle() {
        return style.get();
    }

    /**
     * @return the instance of this AbstractMetaDataRendererParameter.
     */
    protected abstract R getThis();

    public boolean isShowErrorMessages() {
        return showErrorMessages.get();
    }

    public boolean isShowInfoMessages() {
        return showInfoMessages.get();
    }

    public boolean isShowWarningMessages() {
        return showWarningMessages.get();
    }

    public void setshowErrorMessages(boolean state) {
        showErrorMessages.set(state);
    }

    public void setshowInfoMessages(boolean state) {
        showInfoMessages.set(state);
    }

    public void setshowWarningMessages(boolean state) {
        showWarningMessages.set(state);
    }

    public R setStyle(final String newStyle) {
        style.set(newStyle);
        return getThis();
    }

    public BooleanProperty showErrorMessagesProperty() {
        return showErrorMessages;
    }

    public BooleanProperty showInfoMessagesProperty() {
        return showInfoMessages;
    }

    public BooleanProperty showWarningMessagesProperty() {
        return showWarningMessages;
    }

    public StringProperty styleProperty() {
        return style;
    }

    public int getGlobalIndexOffset() {
        return indexOffset.get();
    }

    public IntegerProperty indexOffsetProperty() {
        return indexOffset;
    }

    public void setGlobalIndexOffset(int globalIndexOffset) {
        this.indexOffset.set(globalIndexOffset);
    }

    // ******************************* CSS Style Stuff *********************

    public final R updateCSS() {
        // TODO add/complete CSS parser

        // parse CSS based definitions
        // find definition for STYLE_CLASS_LABELLED_MARKER
        // parse
        strokeColorMarker = AbstractMetaDataRendererParameter.DEFAULT_GRID_LINE_COLOR;
        strokeLineWidthMarker = AbstractMetaDataRendererParameter.DEFAULT_GRID_LINE_WIDTH;
        strokeDashPattern = AbstractMetaDataRendererParameter.DEFAULT_GRID_DASH_PATTERM;

        // hint (getStyle() != null) -> parse user-specified marker

        return getThis();
    }
}
