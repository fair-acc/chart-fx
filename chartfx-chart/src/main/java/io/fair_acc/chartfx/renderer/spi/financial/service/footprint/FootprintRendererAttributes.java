package io.fair_acc.chartfx.renderer.spi.financial.service.footprint;

import io.fair_acc.chartfx.renderer.spi.financial.FinancialTheme;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import io.fair_acc.dataset.spi.financial.api.attrs.AttributeKey;
import io.fair_acc.dataset.spi.financial.api.attrs.AttributeModel;

public class FootprintRendererAttributes extends AttributeModel {
    /** Column coloring group feature active, default true */
    public static final AttributeKey<Boolean> COLUMN_COLORING_FEATURE_ACTIVE = AttributeKey.create(Boolean.class, "COLUMN_COLORING_FEATURE_ACTIVE");

    /** Draw pullback column, default true */
    public static final AttributeKey<Boolean> DRAW_PULLBACK_COLUMN = AttributeKey.create(Boolean.class, "DRAW_PULLBACK_COLUMN");

    /** Draw rectangle of POC of each bar, default true */
    public static final AttributeKey<Boolean> DRAW_POC_RECTANGLE_OF_EACH_BAR = AttributeKey.create(Boolean.class, "DRAW_POC_RECTANGLE_OF_EACH_BAR");

    /**
     * Column color group settings:
     * 1st column bid [0, 1, 2, 3] groups
     * 2nd column ask [0, 1, 2, 3] groups
     */
    public static final AttributeKey<Color[][]> COLUMN_COLOR_GROUP_SETTINGS = AttributeKey.create(Color[][].class, "COLUMN_COLOR_GROUP_SETTINGS");

    /**
     * Column color group thresholds:
     * three thresholds for calculation of column color group choosing process
     */
    public static final AttributeKey<Double[]> COLUMN_COLOR_GROUP_THRESHOLDS = AttributeKey.create(Double[].class, "COLUMN_COLOR_GROUP_THRESHOLDS");

    /** Bolding/Plain font bid/ask under defined threshold, 0 means disabled, default 30 */
    public static final AttributeKey<Double> BID_ASK_BOLD_THRESHOLD = AttributeKey.create(Double.class, "BID_ASK_BOLD_THRESHOLD");

    /**
     * Bid/Ask volume fonts:
     * 0 - plain normal font, number is less BID_ASK_BOLD_THRESHOLD
     * 1 - bold normal font, number is higher or equal than BID_ASK_BOLD_THRESHOLD
     * 2 - bold big font, the number is last bar and last price
     */
    public static final AttributeKey<Font[]> BID_ASK_VOLUME_FONTS = AttributeKey.create(Font[].class, "BID_ASK_VOLUME_FONTS");

    /**
      * Configure Footprint by default values. Good practise is create these defaults and apply your changes to this instance by direct call setAttribute method.
      *
      * @param scheme the coloring scheme
      * @return define default values
      */
    public static FootprintRendererAttributes getDefaultValues(FinancialTheme scheme) {
        FootprintRendererAttributes model = new FootprintRendererAttributes();

        model.setAttribute(COLUMN_COLORING_FEATURE_ACTIVE, true);

        model.setAttribute(DRAW_POC_RECTANGLE_OF_EACH_BAR, true);

        model.setAttribute(DRAW_PULLBACK_COLUMN, true);

        model.setAttribute(COLUMN_COLOR_GROUP_THRESHOLDS, new Double[] { 40.0d, 100.0d, 150.0d });

        model.setAttribute(BID_ASK_BOLD_THRESHOLD, 30.0d);

        model.setAttribute(BID_ASK_VOLUME_FONTS, new Font[] {
                                                         Font.font("Segoe UI", FontWeight.NORMAL, 13), // plain normal font, number is less BID_ASK_BOLD_THRESHOLD
                                                         Font.font("Segoe UI", FontWeight.BOLD, 12), // bold normal font, number is higher or equal than BID_ASK_BOLD_THRESHOLD
                                                         Font.font("Segoe UI", FontWeight.BOLD, 15) // bold big font, the number is last bar and last price
                                                 });

        Color[][] columnColorGroupSettings;
        switch (scheme) {
        case Sand:
        case Classic:
        case Clearlook:
            columnColorGroupSettings = new Color[][] {
                {
                        Color.rgb(0, 128, 255), // RANGE 0 BID COLOR, color: light blue
                        Color.rgb(128, 128, 128), // RANGE 1 BID COLOR, color: white
                        Color.rgb(255, 128, 192), // RANGE 2 BID COLOR, color: pink
                        Color.rgb(242, 0, 0) // RANGE 3 BID COLOR, color: red
                },
                {
                        Color.rgb(0, 128, 255), // RANGE 0 ASK COLOR, color: light blue
                        Color.rgb(128, 128, 128), // RANGE 1 ASK COLOR, color: white
                        Color.rgb(124, 190, 190), // RANGE 2 ASK COLOR, color: light green
                        Color.rgb(0, 128, 0) // RANGE 3 ASK COLOR, color: green
                }
            };
            break;
        default:
            columnColorGroupSettings = new Color[][] {
                {
                        Color.rgb(0, 128, 255), // RANGE 0 BID COLOR, color: light blue
                        Color.rgb(255, 255, 255), // RANGE 1 BID COLOR, color: white
                        Color.rgb(255, 128, 192), // RANGE 2 BID COLOR, color: pink
                        Color.rgb(242, 0, 0) // RANGE 3 BID COLOR, color: red
                },
                {
                        Color.rgb(0, 128, 255), // RANGE 0 ASK COLOR, color: light blue
                        Color.rgb(255, 255, 255), // RANGE 1 ASK COLOR, color: white
                        Color.rgb(124, 190, 190), // RANGE 2 ASK COLOR, color: light green
                        Color.rgb(0, 128, 0) // RANGE 3 ASK COLOR, color: green
                }
            };
            break;
        }
        model.setAttribute(COLUMN_COLOR_GROUP_SETTINGS, columnColorGroupSettings);

        return model;
    }
}
