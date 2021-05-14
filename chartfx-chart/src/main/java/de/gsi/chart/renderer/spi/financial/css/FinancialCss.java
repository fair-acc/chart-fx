package de.gsi.chart.renderer.spi.financial.css;

public class FinancialCss { // NOPMD decide not to rename it for the time being

    // Common ----------------------------------------------------------------

    /**
     * The line width  which is used for painting base shadow of the dataset
     */
    public static final String DATASET_SHADOW_LINE_WIDTH = "shadowLineWidth";

    /**
     * Transposition of original object to paint shadowed object in percent
     */
    public static final String DATASET_SHADOW_TRANSPOSITION_PERCENT = "shadowTransPercent";

    // Candlesticks ----------------------------------------------------------

    /**
     * The candle color for candle's upstick
     */
    public static final String DATASET_CANDLESTICK_LONG_COLOR = "candleLongColor";

    /**
     * The candle color for candle's downstick
     */
    public static final String DATASET_CANDLESTICK_SHORT_COLOR = "candleShortColor";

    /**
     * The candle wicks color for candle's upstick
     */
    public static final String DATASET_CANDLESTICK_LONG_WICK_COLOR = "candleLongWickColor";

    /**
     * The candle wicks color for candle's downstick
     */
    public static final String DATASET_CANDLESTICK_SHORT_WICK_COLOR = "candleShortWickColor";

    /**
     * If available, generated candlestick shadow with this defined color and transparency
     */
    public static final String DATASET_CANDLESTICK_SHADOW_COLOR = "candleShadowColor";

    /**
     * Volume Long bars with this defined color and transparency, if paintVolume=true, the volume bars are painted.
     */
    public static final String DATASET_CANDLESTICK_VOLUME_LONG_COLOR = "candleVolumeLongColor";

    /**
     * Volume Short bars with this defined color and transparency, if paintVolume=true, the volume bars are painted.
     */
    public static final String DATASET_CANDLESTICK_VOLUME_SHORT_COLOR = "candleVolumeShortColor";

    /**
     * Candle/bar relative width against actual scaled view. Defined in percentage range: {@literal <}0.0, 1.0{@literal >}
     */
    public static final String DATASET_CANDLESTICK_BAR_WIDTH_PERCENTAGE = "barWidthPercent";

    // HiLow (OHLC) ----------------------------------------------------------

    /**
     * The ohlc body color for OHLC's upstick
     */
    public static final String DATASET_HILOW_BODY_LONG_COLOR = "highLowLongColor";

    /**
     * The ohlc body color for OHLC's downstick
     */
    public static final String DATASET_HILOW_BODY_SHORT_COLOR = "highLowShortColor";

    /**
     * The ohlc body stroke for OHLC's
     */
    public static final String DATASET_HILOW_BODY_LINEWIDTH = "highLowBodyLineWidth";

    /**
     * The ohlc color for OHLC's open/close ticks
     */
    public static final String DATASET_HILOW_TICK_LONG_COLOR = "highLowLongTickColor";

    /**
     * The ohlc color for OHLC's open/close ticks
     */
    public static final String DATASET_HILOW_TICK_SHORT_COLOR = "highLowShortTickColor";

    /**
     * Volume Long bars with this defined color and transparency, if paintVolume=true, the volume bars are painted.
     */
    public static final String DATASET_HILOW_VOLUME_LONG_COLOR = "highLowVolumeLongColor";

    /**
     * Volume Short bars with this defined color and transparency, if paintVolume=true, the volume bars are painted.
     */
    public static final String DATASET_HILOW_VOLUME_SHORT_COLOR = "highLowVolumeShortColor";

    /**
     * The ohlc open/close tick stroke for OHLC's
     */
    public static final String DATASET_HILOW_TICK_LINEWIDTH = "highLowTickLineWidth";

    /**
     * If available, generated HiLow OHLC shadow with this defined color and transparency
     */
    public static final String DATASET_HILOW_SHADOW_COLOR = "hiLowShadowColor";

    /**
     * HiLow (OHLC) relative width against actual scaled view. Defined in percentage range: {@literal <}0.0, 1.0{@literal >}
     */
    public static final String DATASET_HILOW_BAR_WIDTH_PERCENTAGE = "hiLowBarWidthPercent";

    // Position / Order Renderers ----------------------------------------------------------

    /**
     * Position renderer the main ratio for resizing of the final position paint
     */
    public static final String DATASET_POSITION_PAINT_MAIN_RATIO = "positionPaintMainRatio";

    /**
     * Small triangle defines the filled price for entry long position color
     */
    public static final String DATASET_POSITION_TRIANGLE_LONG_COLOR = "positionTriangleLongColor";

    /**
     * Small triangle defines the filled price for entry short position color
     */
    public static final String DATASET_POSITION_TRIANGLE_SHORT_COLOR = "positionTriangleShortColor";

    /**
     * Small triangle defines the filled price for exit long and short positions color
     */
    public static final String DATASET_POSITION_TRIANGLE_EXIT_COLOR = "positionTriangleExitColor";

    /**
     * The arrow shows bars where the trade is present, this is a entry long position color
     */
    public static final String DATASET_POSITION_ARROW_LONG_COLOR = "positionArrowLongColor";

    /**
     * The arrow shows bars where the trade is present, this is a entry short position color
     */
    public static final String DATASET_POSITION_ARROW_SHORT_COLOR = "positionArrowShortColor";

    /**
     * The arrow shows bars where the trade is present, this is a exit long and short positions color
     */
    public static final String DATASET_POSITION_ARROW_EXIT_COLOR = "positionArrowExitColor";

    /**
     * Trade Order description text color
     */
    public static final String DATASET_POSITION_LABEL_TRADE_DESCRIPTION_COLOR = "positionLabelTradeDescriptionColor";

    /**
     * Text which is shown for trade order description, long positions
     */
    public static final String DATASET_POSITION_LABEL_LONG_TEXT = "positionLabelLongText";

    /**
     * Text which is shown for trade order description, short positions
     */
    public static final String DATASET_POSITION_LABEL_SHORT_TEXT = "positionLabelShortText";

    /**
     * The linkage line between entry and exit orders for specific position, the color for profitable position
     */
    public static final String DATASET_POSITION_ORDER_LINKAGE_PROFIT_COLOR = "positionOrderLinkageProfitColor";

    /**
     * The linkage line between entry and exit orders for specific position, the color for loss position
     */
    public static final String DATASET_POSITION_ORDER_LINKAGE_LOSS_COLOR = "positionOrderLinkageLossColor";

    /**
     * The linkage line between entry and exit orders for specific position, the dash line style
     */
    public static final String DATASET_POSITION_ORDER_LINKAGE_LINE_DASH = "positionOrderLinkageLineDash";

    /**
     * The linkage line between entry and exit orders for specific position, the line width
     */
    public static final String DATASET_POSITION_ORDER_LINKAGE_LINE_WIDTH = "positionOrderLinkageLineWidth";

    // FOOTPRINT ----------------------------------------------------------

    /**
     * Footprint bar relative width against actual scaled view. Defined in percentage range: {@literal <}0.0, 1.0{@literal >}
     */
    public static final String DATASET_FOOTPRINT_BAR_WIDTH_PERCENTAGE = "footprintBarWidthPercent";

    /**
     * Footprint renderer the main ratio for resizing of the final footprint bar paint
     */
    public static final String DATASET_FOOTPRINT_PAINT_MAIN_RATIO = "footprintPaintMainRatio";

    /**
     * The footprint candle boxes color for candle's upstick
     */
    public static final String DATASET_FOOTPRINT_LONG_COLOR = "footprintLongColor";

    /**
     * The footprint candle boxed color for candle's downstick
     */
    public static final String DATASET_FOOTPRINT_SHORT_COLOR = "footprintShortColor";

    /**
     * Volume Long bars with this defined color and transparency, if paintVolume=true, the volume bars are painted.
     */
    public static final String DATASET_FOOTPRINT_VOLUME_LONG_COLOR = "footprintVolumeLongColor";

    /**
     * Volume Short bars with this defined color and transparency, if paintVolume=true, the volume bars are painted.
     */
    public static final String DATASET_FOOTPRINT_VOLUME_SHORT_COLOR = "footprintVolumeShortColor";

    /**
     * Footprint division line between bid and ask numbers (cross-line vertical)
     */
    public static final String DATASET_FOOTPRINT_CROSS_LINE_COLOR = "footprintCrossLineColor";

    /**
     * Footprint default font color. If the column color grouping is disabled, this color is taken.
     */
    public static final String DATASET_FOOTPRINT_DEFAULT_FONT_COLOR = "footprintDefaultFontColor";

    /**
     * Footprint POC color. POC = Point of control.
     */
    public static final String DATASET_FOOTPRINT_POC_COLOR = "footprintPocColor";

    private FinancialCss() {
    }
}
