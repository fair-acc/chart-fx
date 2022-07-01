package io.fair_acc.chartfx.renderer;

/**
 * enum to encode the various error styles that can be drawn by the ErrorDataSetRenderer and HistoryDataSetRenderer
 *
 * @author rstein
 */
public enum ErrorStyle {
    NONE("NONE"),
    ERRORBARS("ERRORBARS"),
    ERRORSURFACE("ERRORSURFACE"),
    ERRORCOMBO("ERRORCOMBO");
    private final String value;

    ErrorStyle(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}