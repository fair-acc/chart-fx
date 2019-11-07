/*****************************************************************************
 *                                                                           *
 * Common Chart - labeled marker data                                        *
 *                                                                           *
 * modified: 2018-08-24 Harald Braeuning                                     *
 *                                                                           *
 ****************************************************************************/

package de.gsi.dataset.spi;

/**
 * A utility class containing all information about a marker used with a LabbeledMarkerRenderer.
 * 
 * @author braeun
 */
public class LabelledMarker {

    private static String defaultColor = null;
    private static double defaultLineWidth = 0;
    private static double defaultFontSize = 0;
    private double x;
    private double y;
    private String label;

    private String color;
    private double lineWidth;
    private double fontSize = 12.0;

    /**
     * 
     * @param x new X coordinate
     * @param label marker name
     */
    public LabelledMarker(final double x, final String label) {
        this.x = x;
        this.label = label;
        color = LabelledMarker.defaultColor;
        lineWidth = LabelledMarker.defaultLineWidth;
        fontSize = LabelledMarker.defaultFontSize;
    }

    /**
     * 
     * @param x new X coordinate
     * @param label marker name
     * @param color maker color
     */
    public LabelledMarker(final double x, final String label, final String color) {
        this.x = x;
        this.label = label;
        this.color = color;
        lineWidth = LabelledMarker.defaultLineWidth;
        fontSize = LabelledMarker.defaultFontSize;
    }

    /**
     * 
     * @param x new X coordinate
     * @param label marker name
     * @param color maker color
     * @param lineWidth marker line width
     */
    public LabelledMarker(final double x, final String label, final String color, final double lineWidth) {
        this.x = x;
        this.label = label;
        this.color = color;
        this.lineWidth = lineWidth;
        fontSize = LabelledMarker.defaultFontSize;
    }

    /**
     * 
     * @return nomen est omen
     */
    public String getColor() {
        return color;
    }

    /**
     * 
     * @return nomen est omen
     */
    public double getFontSize() {
        return fontSize;
    }

    /**
     * 
     * @return marker label
     */
    public String getLabel() {
        return label;
    }

    /**
     * 
     * @return nomen est omen
     */
    public double getLineWidth() {
        return lineWidth;
    }

    /**
     * Gets the style string for the marker.
     * 
     * @return the style string
     */
    public String getStyle() {
        final StringBuilder sb = new StringBuilder();
        if ((color != null) && !color.isEmpty()) {
            sb.append("strokeColor=").append(color).append("; ");
            sb.append("fillColor=").append(color).append("; ");
        }
        if (lineWidth > 0) {
            sb.append("strokeWidth=").append(lineWidth).append("; ");
        }
        if (fontSize > 0) {
            // sb.append(XYChartCss.FONT_SIZE + "=").append(fontSize).append(";
            // ");
            sb.append("fontSize =").append(fontSize).append("; ");
        }
        return sb.toString();
    }

    /**
     * 
     * @return horizontal marker position
     */
    public double getX() {
        return x;
    }

    /**
     * 
     * @return vertical marker position
     */
    public double getY() {
        return y;
    }

    /**
     * 
     * @param color nomen est omen
     */
    public void setColor(final String color) {
        this.color = color;
    }

    /**
     * 
     * @param fontSize nomen est omen
     */
    public void setFontSize(final double fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * 
     * @param label new maker label string
     */
    public void setLabel(final String label) {
        this.label = label;
    }

    /**
     * 
     * @param lineWidth nomen est omen
     */
    public void setLineWidth(final double lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * 
     * @param x new horizontal marker position
     */
    public void setX(final double x) {
        this.x = x;
    }

    /**
     * 
     * @param y new vertical marker position
     */
    public void setY(final double y) {
        this.y = y;
    }

    /**
     * shift horizontal marker coordinate by shift parameter 'v'
     * 
     * @param v horizontal shift parameter
     */
    public void shift(final double v) {
        x += v;
    }

    /**
     * 
     * @return default color
     */
    public static String getDefaultColor() {
        return LabelledMarker.defaultColor;
    }

    /**
     * 
     * @return nomen est omen
     */
    public static double getDefaultFontSize() {
        return LabelledMarker.defaultFontSize;
    }

    /**
     * 
     * @return nomen est omen
     */
    public static double getDefaultLineWidth() {
        return LabelledMarker.defaultLineWidth;
    }

    /**
     * 
     * @param defaultColor nomen est omen
     */
    public static void setDefaultColor(final String defaultColor) {
        LabelledMarker.defaultColor = defaultColor;
    }

    /**
     * 
     * @param defaultFontSize nomen est omen
     */
    public static void setDefaultFontSize(final double defaultFontSize) {
        LabelledMarker.defaultFontSize = defaultFontSize;
    }

    /**
     * 
     * @param defaultLineWidth nomen est omen
     */
    public static void setDefaultLineWidth(final double defaultLineWidth) {
        LabelledMarker.defaultLineWidth = defaultLineWidth;
    }

}
