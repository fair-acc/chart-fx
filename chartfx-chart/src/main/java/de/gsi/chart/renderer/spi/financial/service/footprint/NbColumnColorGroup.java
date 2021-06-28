package de.gsi.chart.renderer.spi.financial.service.footprint;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * The domain object provides data for footprint column colors.
 * Defines specific colors for each footprint lines by column color services.
 */
public class NbColumnColorGroup {
    public Map<Double, FontColor> fontColorMap = new HashMap<>();

    public static class FontColor {
        public final Font bidFont;
        public final Color bidColor;
        public final Font askFont;
        public final Color askColor;

        public FontColor(Font bidFont, Color bidColor, Font askFont, Color askColor) {
            this.bidFont = bidFont;
            this.bidColor = bidColor;
            this.askFont = askFont;
            this.askColor = askColor;
        }
    }
}
