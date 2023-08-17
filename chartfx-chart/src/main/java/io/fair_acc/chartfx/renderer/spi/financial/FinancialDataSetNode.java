package io.fair_acc.chartfx.renderer.spi.financial;

import io.fair_acc.chartfx.renderer.spi.AbstractRenderer;
import io.fair_acc.chartfx.renderer.spi.AbstractRendererXY;
import io.fair_acc.chartfx.renderer.spi.financial.AbstractFinancialRenderer;
import io.fair_acc.chartfx.ui.css.*;
import io.fair_acc.dataset.DataSet;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.List;

/**
 * CSS styling node for financial datasets.
 *
 * @author ennerf
 */
public class FinancialDataSetNode extends DataSetNode {

    // The properties can be styled using the standard JavaFX naming convention, e.g.,
    //
    // -fx-shadow-line-width: 2.5; => setShadowLineWidth(2.5)

    /**
     * The line width  which is used for painting base shadow of the dataset
     */
    private final DoubleProperty shadowLineWidth = css().createDoubleProperty(this, "shadowLineWidth", 2.5d);

    /**
     * Transposition of original object to paint shadowed object in percent
     */
    private final DoubleProperty shadowTransPercent = css().createDoubleProperty(this, "shadowTransPercent", 0.5d);

    // Candlesticks ----------------------------------------------------------

    /**
     * The candle color for candle's upstick
     */
    private final ObjectProperty<Paint> candleLongColor = css().createPaintProperty(this, "candleLongColor", Color.GREEN);

    /**
     * The candle color for candle's downstick
     */
    private final ObjectProperty<Paint> candleShortColor = css().createPaintProperty(this, "candleShortColor", Color.RED);

    /**
     * The candle wicks color for candle's upstick
     */
    private final ObjectProperty<Paint> candleLongWickColor = css().createPaintProperty(this, "candleLongWickColor", Color.BLACK);

    /**
     * The candle wicks color for candle's downstick
     */
    private final ObjectProperty<Paint> candleShortWickColor = css().createPaintProperty(this, "candleShortWickColor", Color.BLACK);

    /**
     * If available, generated candlestick shadow with this defined color and transparency
     */
    private final ObjectProperty<Paint> candleShadowColor = css().createPaintProperty(this, "candleShadowColor", null);

    /**
     * Volume Long bars with this defined color and transparency, if paintVolume=true, the volume bars are painted.
     */
    private final ObjectProperty<Paint> candleVolumeLongColor = css().createPaintProperty(this, "candleVolumeLongColor", DEFAULT_CANDLE_VOLUME_LONG_COLOR );
    private static final Color DEFAULT_CANDLE_VOLUME_LONG_COLOR = Color.rgb(139, 199, 194, 0.2);

    /**
     * Volume Short bars with this defined color and transparency, if paintVolume=true, the volume bars are painted.
     */
    private final ObjectProperty<Paint> candleVolumeShortColor = css().createPaintProperty(this, "candleVolumeShortColor", DEFAULT_CANDLE_VOLUME_SHORT_COLOR);
    private static final Color DEFAULT_CANDLE_VOLUME_SHORT_COLOR = Color.rgb(235, 160, 159, 0.2);

    /**
     * Candle/bar relative width against actual scaled view. Defined in percentage range: {@literal <}0.0, 1.0{@literal >}
     */
    private final DoubleProperty barWidthPercent = css().createDoubleProperty(this, "barWidthPercent", 0.5d);

    // HiLow (OHLC) ----------------------------------------------------------

    /**
     * The ohlc body color for OHLC's upstick
     */
    private final ObjectProperty<Paint> highLowLongColor = css().createPaintProperty(this, "highLowLongColor", Color.GREEN);

    /**
     * The ohlc body color for OHLC's downstick
     */
    private final ObjectProperty<Paint> highLowShortColor = css().createPaintProperty(this, "highLowShortColor", Color.RED);

    /**
     * The ohlc body stroke for OHLC's
     */
     private final DoubleProperty highLowBodyLineWidth = css().createDoubleProperty(this, "highLowBodyLineWidth", 1.2d);

    /**
     * The ohlc color for OHLC's open/close ticks
     */
    private final ObjectProperty<Paint> highLowLongTickColor = css().createPaintProperty(this, "highLowLongTickColor", Color.GREEN);

    /**
     * The ohlc color for OHLC's open/close ticks
     */
    private final ObjectProperty<Paint> highLowShortTickColor = css().createPaintProperty(this, "highLowShortTickColor", Color.RED);

    /**
     * Volume Long bars with this defined color and transparency, if paintVolume=true, the volume bars are painted.
     * TODO: not used anywhere
     */
    private final ObjectProperty<Paint> highLowVolumeLongColor = css().createPaintProperty(this, "highLowVolumeLongColor", Color.GREEN);

    /**
     * Volume Short bars with this defined color and transparency, if paintVolume=true, the volume bars are painted.
     * TODO: not used anywhere
     */
    private final ObjectProperty<Paint> highLowVolumeShortColor = css().createPaintProperty(this, "highLowVolumeShortColor", Color.RED);

    /**
     * The ohlc open/close tick stroke for OHLC's
     */
     private final DoubleProperty highLowTickLineWidth = css().createDoubleProperty(this, "highLowTickLineWidth", 1.2d);

    /**
     * If available, generated HiLow OHLC shadow with this defined color and transparency
     */
    private final ObjectProperty<Paint> hiLowShadowColor = css().createPaintProperty(this, "hiLowShadowColor", null);

    /**
     * HiLow (OHLC) relative width against actual scaled view. Defined in percentage range: {@literal <}0.0, 1.0{@literal >}
     */
     private final DoubleProperty hiLowBarWidthPercent = css().createDoubleProperty(this, "hiLowBarWidthPercent", 0.6d);

    // Position / Order Renderers ----------------------------------------------------------

    /**
     * Position renderer the main ratio for resizing of the final position paint
     */
     private final DoubleProperty positionPaintMainRatio = css().createDoubleProperty(this, "positionPaintMainRatio", 5.157d);

    /**
     * Small triangle defines the filled price for entry long position color
     */
    private final ObjectProperty<Paint> positionTriangleLongColor = css().createPaintProperty(this, "positionTriangleLongColor", Color.GREEN);

    /**
     * Small triangle defines the filled price for entry short position color
     */
    private final ObjectProperty<Paint> positionTriangleShortColor = css().createPaintProperty(this, "positionTriangleShortColor", Color.RED);

    /**
     * Small triangle defines the filled price for exit long and short positions color
     */
    private final ObjectProperty<Paint> positionTriangleExitColor = css().createPaintProperty(this, "positionTriangleExitColor", Color.BLACK);

    /**
     * The arrow shows bars where the trade is present, this is a entry long position color
     */
    private final ObjectProperty<Paint> positionArrowLongColor = css().createPaintProperty(this, "positionArrowLongColor", Color.GREEN);

    /**
     * The arrow shows bars where the trade is present, this is a entry short position color
     */
    private final ObjectProperty<Paint> positionArrowShortColor = css().createPaintProperty(this, "positionArrowShortColor", Color.RED);

    /**
     * The arrow shows bars where the trade is present, this is a exit long and short positions color
     */
    private final ObjectProperty<Paint> positionArrowExitColor = css().createPaintProperty(this, "positionArrowExitColor", Color.BLACK);

    /**
     * Trade Order description text color
     */
    private final ObjectProperty<Paint> positionLabelTradeDescriptionColor = css().createPaintProperty(this, "positionLabelTradeDescriptionColor", Color.BLACK);

    /**
     * Text which is shown for trade order description, long positions
     */
    private final StringProperty positionLabelLongText = css().createStringProperty(this, "positionLabelLongText", "Buy%n%1.0f%n(%1.1f)");

    /**
     * Text which is shown for trade order description, short positions
     */
    private final StringProperty positionLabelShortText = css().createStringProperty(this, "positionLabelShortText", "Sell%n%1.0f%n(%1.1f)");

    /**
     * The linkage line between entry and exit orders for specific position, the color for profitable position
     */
    private final ObjectProperty<Paint> positionOrderLinkageProfitColor = css().createPaintProperty(this, "positionOrderLinkageProfitColor",  Color.GREEN);

    /**
     * The linkage line between entry and exit orders for specific position, the color for loss position
     */
    private final ObjectProperty<Paint> positionOrderLinkageLossColor = css().createPaintProperty(this, "positionOrderLinkageLossColor", Color.RED);

    /**
     * The linkage line between entry and exit orders for specific position, the dash line style
     */
     private final DoubleProperty positionOrderLinkageLineDash = css().createDoubleProperty(this, "positionOrderLinkageLineDash", 8.0d);

    /**
     * The linkage line between entry and exit orders for specific position, the line width
     */
     private final DoubleProperty positionOrderLinkageLineWidth = css().createDoubleProperty(this, "positionOrderLinkageLineWidth", 2.0d);

    // FOOTPRINT ----------------------------------------------------------

    /**
     * Footprint bar relative width against actual scaled view. Defined in percentage range: {@literal <}0.0, 1.0{@literal >}
     */
     private final DoubleProperty footprintBarWidthPercent = css().createDoubleProperty(this, "footprintBarWidthPercent", 0.5d);

    /**
     * Footprint renderer the main ratio for resizing of the final footprint bar paint
     */
     private final DoubleProperty footprintPaintMainRatio = css().createDoubleProperty(this, "footprintPaintMainRatio", 5.157d);

    /**
     * The footprint candle boxes color for candle's upstick
     */
    private final ObjectProperty<Paint> footprintLongColor = css().createPaintProperty(this, "footprintLongColor", Color.GREEN);

    /**
     * The footprint candle boxed color for candle's downstick
     */
    private final ObjectProperty<Paint> footprintShortColor = css().createPaintProperty(this, "footprintShortColor", Color.RED);

    /**
     * Volume Long bars with this defined color and transparency, if paintVolume=true, the volume bars are painted.
     */
    private final ObjectProperty<Paint> footprintVolumeLongColor = css().createPaintProperty(this, "footprintVolumeLongColor", DEFAULT_FOOTPRINT_VOLUME_LONG_COLOR);
    private static final Paint DEFAULT_FOOTPRINT_VOLUME_LONG_COLOR = Color.rgb(139, 199, 194, 0.2);

    /**
     * Volume Short bars with this defined color and transparency, if paintVolume=true, the volume bars are painted.
     */
    private final ObjectProperty<Paint> footprintVolumeShortColor = css().createPaintProperty(this, "footprintVolumeShortColor", DEFAULT_FOOTPRINT_VOLUME_SHORT_COLOR);
    private static final Paint DEFAULT_FOOTPRINT_VOLUME_SHORT_COLOR = Color.rgb(235, 160, 159, 0.2);

    /**
     * Footprint division line between bid and ask numbers (cross-line vertical)
     */
    private final ObjectProperty<Paint> footprintCrossLineColor = css().createPaintProperty(this, "footprintCrossLineColor", Color.GRAY);

    /**
     * Footprint default font color. If the column color grouping is disabled, this color is taken.
     */
    private final ObjectProperty<Paint> footprintDefaultFontColor = css().createPaintProperty(this, "footprintDefaultFontColor", FOOTPRINT_DEFAULT_FONT_COLOR);
    private static final Paint FOOTPRINT_DEFAULT_FONT_COLOR = Color.rgb(255, 255, 255, 0.58);

    /**
     * Footprint POC color. POC = Point of control.
     */
    private final ObjectProperty<Paint> footprintPocColor = css().createPaintProperty(this, "footprintPocColor", Color.YELLOW);

    // REQUIRED INHERITANCE METHODS -------------------------------

    public FinancialDataSetNode(AbstractRenderer<?> renderer, DataSet dataSet) {
        super(renderer, dataSet);
        StyleUtil.addStyles(this, "financial");
    }

    protected CssPropertyFactory<DataSetNodeParameter> css() {
        return CSS;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return css().getCssMetaData();
    }

    private static final CssPropertyFactory<DataSetNodeParameter> CSS = new CssPropertyFactory<>(TextStyle.getClassCssMetaData());

    // GENERATED ACCESSOR METHODS -------------------------------


    public double getShadowLineWidth() {
        return shadowLineWidth.get();
    }

    public DoubleProperty shadowLineWidthProperty() {
        return shadowLineWidth;
    }

    public void setShadowLineWidth(double shadowLineWidth) {
        this.shadowLineWidth.set(shadowLineWidth);
    }

    public double getShadowTransPercent() {
        return shadowTransPercent.get();
    }

    public DoubleProperty shadowTransPercentProperty() {
        return shadowTransPercent;
    }

    public void setShadowTransPercent(double shadowTransPercent) {
        this.shadowTransPercent.set(shadowTransPercent);
    }

    public Paint getCandleLongColor() {
        return candleLongColor.get();
    }

    public ObjectProperty<Paint> candleLongColorProperty() {
        return candleLongColor;
    }

    public void setCandleLongColor(Paint candleLongColor) {
        this.candleLongColor.set(candleLongColor);
    }

    public Paint getCandleShortColor() {
        return candleShortColor.get();
    }

    public ObjectProperty<Paint> candleShortColorProperty() {
        return candleShortColor;
    }

    public void setCandleShortColor(Paint candleShortColor) {
        this.candleShortColor.set(candleShortColor);
    }

    public Paint getCandleLongWickColor() {
        return candleLongWickColor.get();
    }

    public ObjectProperty<Paint> candleLongWickColorProperty() {
        return candleLongWickColor;
    }

    public void setCandleLongWickColor(Paint candleLongWickColor) {
        this.candleLongWickColor.set(candleLongWickColor);
    }

    public Paint getCandleShortWickColor() {
        return candleShortWickColor.get();
    }

    public ObjectProperty<Paint> candleShortWickColorProperty() {
        return candleShortWickColor;
    }

    public void setCandleShortWickColor(Paint candleShortWickColor) {
        this.candleShortWickColor.set(candleShortWickColor);
    }

    public Paint getCandleShadowColor() {
        return candleShadowColor.get();
    }

    public ObjectProperty<Paint> candleShadowColorProperty() {
        return candleShadowColor;
    }

    public void setCandleShadowColor(Paint candleShadowColor) {
        this.candleShadowColor.set(candleShadowColor);
    }

    public Paint getCandleVolumeLongColor() {
        return candleVolumeLongColor.get();
    }

    public ObjectProperty<Paint> candleVolumeLongColorProperty() {
        return candleVolumeLongColor;
    }

    public void setCandleVolumeLongColor(Paint candleVolumeLongColor) {
        this.candleVolumeLongColor.set(candleVolumeLongColor);
    }

    public Paint getCandleVolumeShortColor() {
        return candleVolumeShortColor.get();
    }

    public ObjectProperty<Paint> candleVolumeShortColorProperty() {
        return candleVolumeShortColor;
    }

    public void setCandleVolumeShortColor(Paint candleVolumeShortColor) {
        this.candleVolumeShortColor.set(candleVolumeShortColor);
    }

    public double getBarWidthPercent() {
        return barWidthPercent.get();
    }

    public DoubleProperty barWidthPercentProperty() {
        return barWidthPercent;
    }

    public void setBarWidthPercent(double barWidthPercent) {
        this.barWidthPercent.set(barWidthPercent);
    }

    public Paint getHighLowLongColor() {
        return highLowLongColor.get();
    }

    public ObjectProperty<Paint> highLowLongColorProperty() {
        return highLowLongColor;
    }

    public void setHighLowLongColor(Paint highLowLongColor) {
        this.highLowLongColor.set(highLowLongColor);
    }

    public Paint getHighLowShortColor() {
        return highLowShortColor.get();
    }

    public ObjectProperty<Paint> highLowShortColorProperty() {
        return highLowShortColor;
    }

    public void setHighLowShortColor(Paint highLowShortColor) {
        this.highLowShortColor.set(highLowShortColor);
    }

    public double getHighLowBodyLineWidth() {
        return highLowBodyLineWidth.get();
    }

    public DoubleProperty highLowBodyLineWidthProperty() {
        return highLowBodyLineWidth;
    }

    public void setHighLowBodyLineWidth(double highLowBodyLineWidth) {
        this.highLowBodyLineWidth.set(highLowBodyLineWidth);
    }

    public Paint getHighLowLongTickColor() {
        return highLowLongTickColor.get();
    }

    public ObjectProperty<Paint> highLowLongTickColorProperty() {
        return highLowLongTickColor;
    }

    public void setHighLowLongTickColor(Paint highLowLongTickColor) {
        this.highLowLongTickColor.set(highLowLongTickColor);
    }

    public Paint getHighLowShortTickColor() {
        return highLowShortTickColor.get();
    }

    public ObjectProperty<Paint> highLowShortTickColorProperty() {
        return highLowShortTickColor;
    }

    public void setHighLowShortTickColor(Paint highLowShortTickColor) {
        this.highLowShortTickColor.set(highLowShortTickColor);
    }

    public Paint getHighLowVolumeLongColor() {
        return highLowVolumeLongColor.get();
    }

    public ObjectProperty<Paint> highLowVolumeLongColorProperty() {
        return highLowVolumeLongColor;
    }

    public void setHighLowVolumeLongColor(Paint highLowVolumeLongColor) {
        this.highLowVolumeLongColor.set(highLowVolumeLongColor);
    }

    public Paint getHighLowVolumeShortColor() {
        return highLowVolumeShortColor.get();
    }

    public ObjectProperty<Paint> highLowVolumeShortColorProperty() {
        return highLowVolumeShortColor;
    }

    public void setHighLowVolumeShortColor(Paint highLowVolumeShortColor) {
        this.highLowVolumeShortColor.set(highLowVolumeShortColor);
    }

    public double getHighLowTickLineWidth() {
        return highLowTickLineWidth.get();
    }

    public DoubleProperty highLowTickLineWidthProperty() {
        return highLowTickLineWidth;
    }

    public void setHighLowTickLineWidth(double highLowTickLineWidth) {
        this.highLowTickLineWidth.set(highLowTickLineWidth);
    }

    public Paint getHiLowShadowColor() {
        return hiLowShadowColor.get();
    }

    public ObjectProperty<Paint> hiLowShadowColorProperty() {
        return hiLowShadowColor;
    }

    public void setHiLowShadowColor(Paint hiLowShadowColor) {
        this.hiLowShadowColor.set(hiLowShadowColor);
    }

    public double getHiLowBarWidthPercent() {
        return hiLowBarWidthPercent.get();
    }

    public DoubleProperty hiLowBarWidthPercentProperty() {
        return hiLowBarWidthPercent;
    }

    public void setHiLowBarWidthPercent(double hiLowBarWidthPercent) {
        this.hiLowBarWidthPercent.set(hiLowBarWidthPercent);
    }

    public double getPositionPaintMainRatio() {
        return positionPaintMainRatio.get();
    }

    public DoubleProperty positionPaintMainRatioProperty() {
        return positionPaintMainRatio;
    }

    public void setPositionPaintMainRatio(double positionPaintMainRatio) {
        this.positionPaintMainRatio.set(positionPaintMainRatio);
    }

    public Paint getPositionTriangleLongColor() {
        return positionTriangleLongColor.get();
    }

    public ObjectProperty<Paint> positionTriangleLongColorProperty() {
        return positionTriangleLongColor;
    }

    public void setPositionTriangleLongColor(Paint positionTriangleLongColor) {
        this.positionTriangleLongColor.set(positionTriangleLongColor);
    }

    public Paint getPositionTriangleShortColor() {
        return positionTriangleShortColor.get();
    }

    public ObjectProperty<Paint> positionTriangleShortColorProperty() {
        return positionTriangleShortColor;
    }

    public void setPositionTriangleShortColor(Paint positionTriangleShortColor) {
        this.positionTriangleShortColor.set(positionTriangleShortColor);
    }

    public Paint getPositionTriangleExitColor() {
        return positionTriangleExitColor.get();
    }

    public ObjectProperty<Paint> positionTriangleExitColorProperty() {
        return positionTriangleExitColor;
    }

    public void setPositionTriangleExitColor(Paint positionTriangleExitColor) {
        this.positionTriangleExitColor.set(positionTriangleExitColor);
    }

    public Paint getPositionArrowLongColor() {
        return positionArrowLongColor.get();
    }

    public ObjectProperty<Paint> positionArrowLongColorProperty() {
        return positionArrowLongColor;
    }

    public void setPositionArrowLongColor(Paint positionArrowLongColor) {
        this.positionArrowLongColor.set(positionArrowLongColor);
    }

    public Paint getPositionArrowShortColor() {
        return positionArrowShortColor.get();
    }

    public ObjectProperty<Paint> positionArrowShortColorProperty() {
        return positionArrowShortColor;
    }

    public void setPositionArrowShortColor(Paint positionArrowShortColor) {
        this.positionArrowShortColor.set(positionArrowShortColor);
    }

    public Paint getPositionArrowExitColor() {
        return positionArrowExitColor.get();
    }

    public ObjectProperty<Paint> positionArrowExitColorProperty() {
        return positionArrowExitColor;
    }

    public void setPositionArrowExitColor(Paint positionArrowExitColor) {
        this.positionArrowExitColor.set(positionArrowExitColor);
    }

    public Paint getPositionLabelTradeDescriptionColor() {
        return positionLabelTradeDescriptionColor.get();
    }

    public ObjectProperty<Paint> positionLabelTradeDescriptionColorProperty() {
        return positionLabelTradeDescriptionColor;
    }

    public void setPositionLabelTradeDescriptionColor(Paint positionLabelTradeDescriptionColor) {
        this.positionLabelTradeDescriptionColor.set(positionLabelTradeDescriptionColor);
    }

    public String getPositionLabelLongText() {
        return positionLabelLongText.get();
    }

    public StringProperty positionLabelLongTextProperty() {
        return positionLabelLongText;
    }

    public void setPositionLabelLongText(String positionLabelLongText) {
        this.positionLabelLongText.set(positionLabelLongText);
    }

    public String getPositionLabelShortText() {
        return positionLabelShortText.get();
    }

    public StringProperty positionLabelShortTextProperty() {
        return positionLabelShortText;
    }

    public void setPositionLabelShortText(String positionLabelShortText) {
        this.positionLabelShortText.set(positionLabelShortText);
    }

    public Paint getPositionOrderLinkageProfitColor() {
        return positionOrderLinkageProfitColor.get();
    }

    public ObjectProperty<Paint> positionOrderLinkageProfitColorProperty() {
        return positionOrderLinkageProfitColor;
    }

    public void setPositionOrderLinkageProfitColor(Paint positionOrderLinkageProfitColor) {
        this.positionOrderLinkageProfitColor.set(positionOrderLinkageProfitColor);
    }

    public Paint getPositionOrderLinkageLossColor() {
        return positionOrderLinkageLossColor.get();
    }

    public ObjectProperty<Paint> positionOrderLinkageLossColorProperty() {
        return positionOrderLinkageLossColor;
    }

    public void setPositionOrderLinkageLossColor(Paint positionOrderLinkageLossColor) {
        this.positionOrderLinkageLossColor.set(positionOrderLinkageLossColor);
    }

    public double getPositionOrderLinkageLineDash() {
        return positionOrderLinkageLineDash.get();
    }

    public DoubleProperty positionOrderLinkageLineDashProperty() {
        return positionOrderLinkageLineDash;
    }

    public void setPositionOrderLinkageLineDash(double positionOrderLinkageLineDash) {
        this.positionOrderLinkageLineDash.set(positionOrderLinkageLineDash);
    }

    public double getPositionOrderLinkageLineWidth() {
        return positionOrderLinkageLineWidth.get();
    }

    public DoubleProperty positionOrderLinkageLineWidthProperty() {
        return positionOrderLinkageLineWidth;
    }

    public void setPositionOrderLinkageLineWidth(double positionOrderLinkageLineWidth) {
        this.positionOrderLinkageLineWidth.set(positionOrderLinkageLineWidth);
    }

    public double getFootprintBarWidthPercent() {
        return footprintBarWidthPercent.get();
    }

    public DoubleProperty footprintBarWidthPercentProperty() {
        return footprintBarWidthPercent;
    }

    public void setFootprintBarWidthPercent(double footprintBarWidthPercent) {
        this.footprintBarWidthPercent.set(footprintBarWidthPercent);
    }

    public double getFootprintPaintMainRatio() {
        return footprintPaintMainRatio.get();
    }

    public DoubleProperty footprintPaintMainRatioProperty() {
        return footprintPaintMainRatio;
    }

    public void setFootprintPaintMainRatio(double footprintPaintMainRatio) {
        this.footprintPaintMainRatio.set(footprintPaintMainRatio);
    }

    public Paint getFootprintLongColor() {
        return footprintLongColor.get();
    }

    public ObjectProperty<Paint> footprintLongColorProperty() {
        return footprintLongColor;
    }

    public void setFootprintLongColor(Paint footprintLongColor) {
        this.footprintLongColor.set(footprintLongColor);
    }

    public Paint getFootprintShortColor() {
        return footprintShortColor.get();
    }

    public ObjectProperty<Paint> footprintShortColorProperty() {
        return footprintShortColor;
    }

    public void setFootprintShortColor(Paint footprintShortColor) {
        this.footprintShortColor.set(footprintShortColor);
    }

    public Paint getFootprintVolumeLongColor() {
        return footprintVolumeLongColor.get();
    }

    public ObjectProperty<Paint> footprintVolumeLongColorProperty() {
        return footprintVolumeLongColor;
    }

    public void setFootprintVolumeLongColor(Paint footprintVolumeLongColor) {
        this.footprintVolumeLongColor.set(footprintVolumeLongColor);
    }

    public Paint getFootprintVolumeShortColor() {
        return footprintVolumeShortColor.get();
    }

    public ObjectProperty<Paint> footprintVolumeShortColorProperty() {
        return footprintVolumeShortColor;
    }

    public void setFootprintVolumeShortColor(Paint footprintVolumeShortColor) {
        this.footprintVolumeShortColor.set(footprintVolumeShortColor);
    }

    public Paint getFootprintCrossLineColor() {
        return footprintCrossLineColor.get();
    }

    public ObjectProperty<Paint> footprintCrossLineColorProperty() {
        return footprintCrossLineColor;
    }

    public void setFootprintCrossLineColor(Paint footprintCrossLineColor) {
        this.footprintCrossLineColor.set(footprintCrossLineColor);
    }

    public Paint getFootprintDefaultFontColor() {
        return footprintDefaultFontColor.get();
    }

    public ObjectProperty<Paint> footprintDefaultFontColorProperty() {
        return footprintDefaultFontColor;
    }

    public void setFootprintDefaultFontColor(Paint footprintDefaultFontColor) {
        this.footprintDefaultFontColor.set(footprintDefaultFontColor);
    }

    public Paint getFootprintPocColor() {
        return footprintPocColor.get();
    }

    public ObjectProperty<Paint> footprintPocColorProperty() {
        return footprintPocColor;
    }

    public void setFootprintPocColor(Paint footprintPocColor) {
        this.footprintPocColor.set(footprintPocColor);
    }
}
