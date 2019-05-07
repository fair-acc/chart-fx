/*****************************************************************************
 *                                                                           *
 * Common Chart - labeled marker data                                        *
 *                                                                           *
 * modified: 2018-08-24 Harald Braeuning                                     *
 *                                                                           *
 ****************************************************************************/

package de.gsi.chart.data.spi;

import de.gsi.chart.XYChartCss;

/**
 * A utility class containing all information about a marker used with a
 * LabbeledMarkerRenderer.
 * @author braeun
 */
public class LabelledMarker {

  private double x;
  private double y;
  private String label;
  private String color;
  private double lineWidth;
  private double fontSize = 12.0;

  private static String defaultColor = null;
  private static double defaultLineWidth = 0;
  private static double defaultFontSize = 0;

  public static String getDefaultColor()
  {
    return LabelledMarker.defaultColor;
  }

  public static void setDefaultColor(String defaultColor)
  {
    LabelledMarker.defaultColor = defaultColor;
  }

  public static double getDefaultLineWidth()
  {
    return LabelledMarker.defaultLineWidth;
  }

  public static void setDefaultLineWidth(double defaultLineWidth)
  {
    LabelledMarker.defaultLineWidth = defaultLineWidth;
  }

  public static double getDefaultFontSize()
  {
    return LabelledMarker.defaultFontSize;
  }

  public static void setDefaultFontSize(double defaultFontSize)
  {
    LabelledMarker.defaultFontSize = defaultFontSize;
  }




  public LabelledMarker(double x, String label)
  {
    this.x = x;
    this.label = label;
    color = LabelledMarker.defaultColor;
    lineWidth = LabelledMarker.defaultLineWidth;
    fontSize = LabelledMarker.defaultFontSize;
  }

  public LabelledMarker(double x, String label, String color)
  {
    this.x = x;
    this.label = label;
    this.color = color;
    lineWidth = LabelledMarker.defaultLineWidth;
    fontSize = LabelledMarker.defaultFontSize;
  }

  public LabelledMarker(double x, String label, String color, double lineWidth)
  {
    this.x = x;
    this.label = label;
    this.color = color;
    this.lineWidth = lineWidth;
    fontSize = LabelledMarker.defaultFontSize;
  }

  public void shift(double v)
  {
    x += v;
  }

  public double getX()
  {
    return x;
  }

  public void setX(double x)
  {
    this.x = x;
  }

  public double getY()
  {
    return y;
  }

  public void setY(double y)
  {
    this.y = y;
  }

  public String getLabel()
  {
    return label;
  }

  public void setLabel(String label)
  {
    this.label = label;
  }

  public String getColor()
  {
    return color;
  }

  public void setColor(String color)
  {
    this.color = color;
  }

  public double getLineWidth()
  {
    return lineWidth;
  }

  public void setLineWidth(double lineWidth)
  {
    this.lineWidth = lineWidth;
  }

  public double getFontSize()
  {
    return fontSize;
  }

  public void setFontSize(double fontSize)
  {
    this.fontSize = fontSize;
  }

  /**
   * Gets the style string for the marker.
   * @return the style string
   */
  public String getStyle()
  {
    final StringBuilder sb = new StringBuilder();
    if (color != null && !color.isEmpty())
    {
      sb.append("strokeColor=").append(color).append("; ");
      sb.append("fillColor=").append(color).append("; ");
    }
    if (lineWidth > 0) {
		sb.append("strokeWidth=").append(lineWidth).append("; ");
	}
    if (fontSize > 0) {
		sb.append(XYChartCss.FONT_SIZE + "=").append(fontSize).append("; ");
	}
    return sb.toString();
  }

}
