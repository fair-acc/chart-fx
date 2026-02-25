package io.fair_acc.chartfx.fxinternals;

import com.sun.javafx.tk.Toolkit;
import javafx.scene.text.Font;

/**
 * Accesses internal OpenJFX font metrics. Only available if runtime started with
 * --add-opens=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED
 * <p>
 * Required compilation flags:
 * --add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED
 *
 * @author ennerf
 */
public class FxFontMetrics {

    public static boolean isAvailable() {
        return IS_AVAILABLE;
    }

    public static void checkAvailable() {
        if (!IS_AVAILABLE) {
            throw new UnsupportedOperationException("FxFontMetrics not available. Requires: --add-opens=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED");
        }
    }

    public static double getLineHeight(Font font) {
        checkAvailable();
        return Toolkit.getToolkit().getFontLoader().getFontMetrics(font).getLineHeight();
    }

    public static double getCharWidth(Font font, char c) {
        checkAvailable();
        return Toolkit.getToolkit().getFontLoader().getCharWidth(c, font);
    }

    /**
     * Returns the combined width of all individual characters. Note that this may
     * not be perfectly accurate as a character's width may depend on the neighbors.
     */
    public static double getWidth(Font font, CharSequence str) {
        checkAvailable();
        var loader = Toolkit.getToolkit().getFontLoader();
        double totalWidth = 0;
        for (int i = 0; i < str.length(); i++) {
            totalWidth += loader.getCharWidth(str.charAt(i), font);
        }
        return totalWidth;
    }

    private static final boolean IS_AVAILABLE;

    static {
        boolean available = false;
        try {
            var loader = Toolkit.getToolkit().getFontLoader();
            available = true;
        } catch (Throwable e) {
            // NoClassDefFoundError or illegal access when
            // properties are not set.
        }
        IS_AVAILABLE = available;
    }

}
