package io.fair_acc.chartfx.renderer.spi;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.List;

import io.fair_acc.chartfx.ui.css.*;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.TextAlignment;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.TickMark;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.utils.DashPatternStyle;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.utils.NoDuplicatesList;

@SuppressWarnings("PMD.GodClass")
public class GridRenderer extends Parent implements Renderer {
    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static final String STYLE_CLASS_GRID_RENDERER = "grid-renderer";
    private static final String STYLE_CLASS_MAJOR_GRID_LINE = "chart-major-grid-lines";
    private static final String STYLE_CLASS_MAJOR_GRID_LINE_H = "chart-major-horizontal-lines";
    private static final String STYLE_CLASS_MAJOR_GRID_LINE_V = "chart-major-vertical-lines";
    private static final String STYLE_CLASS_MINOR_GRID_LINE = "chart-minor-grid-lines";
    private static final String STYLE_CLASS_MINOR_GRID_LINE_H = "chart-minor-horizontal-lines";
    private static final String STYLE_CLASS_MINOR_GRID_LINE_V = "chart-minor-vertical-lines";
    private static final String STYLE_CLASS_GRID_ON_TOP = "chart-grid-line-on-top";
    private static final PseudoClass WITH_MINOR_PSEUDO_CLASS = PseudoClass.getPseudoClass("withMinor");

    private final StyleGroup styles = new StyleGroup(getChildren());
    private final LineStyle horMajorGridStyleNode = styles.newLineStyle(STYLE_CLASS_MAJOR_GRID_LINE, STYLE_CLASS_MAJOR_GRID_LINE_H);
    private final LineStyle verMajorGridStyleNode = styles.newLineStyle(STYLE_CLASS_MAJOR_GRID_LINE, STYLE_CLASS_MAJOR_GRID_LINE_V);
    private final LineStyle horMinorGridStyleNode = styles.newLineStyle(STYLE_CLASS_MINOR_GRID_LINE, STYLE_CLASS_MINOR_GRID_LINE_H);
    private final LineStyle verMinorGridStyleNode = styles.newLineStyle(STYLE_CLASS_MINOR_GRID_LINE, STYLE_CLASS_MINOR_GRID_LINE_V);
    private final StyleableBooleanProperty drawGridOnTop = CSS.createBooleanProperty(this, "drawGridOnTop", true);

    protected final ObservableList<Axis> axesList = FXCollections.observableList(new NoDuplicatesList<>());

    public GridRenderer() {
        super();
        StyleUtil.hiddenStyleNode(this, STYLE_CLASS_GRID_RENDERER);
        StyleUtil.applyPseudoClass(horMajorGridStyleNode, GridRenderer.WITH_MINOR_PSEUDO_CLASS, horMinorGridStyleNode.visibleProperty());
        StyleUtil.applyPseudoClass(verMajorGridStyleNode, GridRenderer.WITH_MINOR_PSEUDO_CLASS, verMinorGridStyleNode.visibleProperty());
    }

    protected void drawEuclideanGrid(final GraphicsContext gc, XYChart xyChart) {
        final Axis xAxis = xyChart.getXAxis();
        final Axis yAxis = xyChart.getYAxis();
        final double xAxisWidth = xyChart.getCanvas().getWidth();
        final double xAxisWidthSnapped = snap(xAxisWidth);
        final double yAxisHeight = xyChart.getCanvas().getHeight();
        final double yAxisHeightSnapped = snap(yAxisHeight);
        if (xAxis instanceof Node) {
            ((Node) xAxis).setVisible(true);
        }

        gc.save();
        drawVerticalMajorGridLines(gc, xAxis, xAxisWidth, yAxisHeightSnapped);
        drawVerticalMinorGridLines(gc, xAxis, xAxisWidth, yAxisHeightSnapped);
        drawHorizontalMajorGridLines(gc, yAxis, xAxisWidthSnapped, yAxisHeight);
        drawHorizontalMinorGridLines(gc, yAxis, xAxisWidthSnapped, yAxisHeight);
        gc.restore();
    }

    protected void drawHorizontalMajorGridLines(final GraphicsContext gc, final Axis yAxis,
            final double xAxisWidthSnapped, final double yAxisHeight) {
        if (!horMajorGridStyleNode.isVisible() && !horMinorGridStyleNode.isVisible()) {
            return;
        }
        final double zeroSnapped = snap(0);
        applyGraphicsStyleFromLineStyle(gc, horMajorGridStyleNode);
        ObservableList<TickMark> tickMarks = yAxis.getTickMarks();
        for (TickMark tickMark : tickMarks) {
            double y = snap(yAxis.getDisplayPosition(tickMark.getValue()));
            if (y >= 0 && y < yAxisHeight) {
                // gc.strokeLine(zeroSnapped, y, xAxisWidthSnapped, y);
                DashPatternStyle.strokeDashedLine(gc, zeroSnapped, y, xAxisWidthSnapped, y);
            }
        }
    }

    protected void drawHorizontalMinorGridLines(final GraphicsContext gc, final Axis yAxis,
            final double xAxisWidthSnapped, final double yAxisHeight) {
        if (!yAxis.isLogAxis() && !horMinorGridStyleNode.isVisible()) {
            return;
        }
        final double zeroSnapped = snap(0);
        applyGraphicsStyleFromLineStyle(gc, horMinorGridStyleNode);
        ObservableList<TickMark> tickMarks = yAxis.getMinorTickMarks();
        for (TickMark tickMark : tickMarks) {
            double y = snap(yAxis.getDisplayPosition(tickMark.getValue()));
            if (y >= 0 && y < yAxisHeight) {
                // gc.strokeLine(zeroSnapped, y, xAxisWidthSnapped, y);
                DashPatternStyle.strokeDashedLine(gc, zeroSnapped, y, xAxisWidthSnapped, y);
            }
        }
    }

    protected void drawPolarCircle(final GraphicsContext gc, final Axis yAxis, final double yRange,
            final double xCentre, final double yCentre, final double maxRadius) {
        if (!horMajorGridStyleNode.isVisible() && !horMinorGridStyleNode.isVisible()) {
            return;
        }

        applyGraphicsStyleFromLineStyle(gc, horMajorGridStyleNode);
        final ObservableList<TickMark> yTickMarks = yAxis.getTickMarks();

        gc.strokeOval(xCentre - maxRadius, yCentre - maxRadius, 2 * maxRadius, 2 * maxRadius);

        // draw major tick circle
        yTickMarks.forEach(tick -> {
            final double yPos = yRange - yAxis.getDisplayPosition(tick.getValue());
            final String label = yAxis.getTickMarkLabel(tick.getValue());
            final double yNorm = yPos / yRange * maxRadius;

            if (yNorm >= 0 && yNorm < maxRadius) {
                gc.strokeOval(xCentre - yNorm, yCentre - yNorm, 2 * yNorm, 2 * yNorm);

                gc.save();
                gc.setFont(yAxis.getTickLabelStyle().getFont());
                gc.setStroke(yAxis.getTickLabelStyle().getFill()); // TODO: why stroke rather than fill?
                gc.setLineDashes((double[]) null);
                gc.setTextBaseline(VPos.CENTER);
                gc.strokeText(label, xCentre + (int) yAxis.getTickLabelGap(), yCentre - yNorm);
                gc.restore();
            }
        });

        if (!yAxis.isLogAxis() && !horMinorGridStyleNode.isVisible()) {
            return;
        }

        // draw minor tick circle
        applyGraphicsStyleFromLineStyle(gc, horMinorGridStyleNode);
        yAxis.getMinorTickMarks().stream().mapToDouble(minorTick -> yRange - minorTick.getPosition()).forEach(yPos -> {
            final double yNorm = yPos / yRange * maxRadius;
            if (yNorm >= 0 && yNorm < maxRadius) {
                gc.strokeOval(xCentre - yNorm, yCentre - yNorm, 2 * yNorm, 2 * yNorm);
            }
        });
    }

    protected void drawPolarGrid(final GraphicsContext gc, XYChart xyChart) {
        final Axis xAxis = xyChart.getXAxis();
        final Axis yAxis = xyChart.getYAxis();
        final double xAxisWidth = xyChart.getCanvas().getWidth();
        final double yAxisHeight = xyChart.getCanvas().getHeight();
        final double xRange = xAxis.getWidth();
        final double yRange = yAxis.getHeight();
        final double xCentre = xRange / 2;
        final double yCentre = yRange / 2;
        final double maxRadius = 0.5 * Math.min(xRange, yRange) * 0.9;
        if (xAxis instanceof Node) {
            ((Node) xAxis).setVisible(false);
        }

        gc.save();
        if (verMajorGridStyleNode.isVisible() || verMinorGridStyleNode.isVisible()) {
            applyGraphicsStyleFromLineStyle(gc, verMajorGridStyleNode);
            for (double phi = 0.0; phi <= 360; phi += xyChart.getPolarStepSize().get()) {
                final double x = xCentre + maxRadius * Math.sin(phi * GridRenderer.DEG_TO_RAD);
                final double y = yCentre - maxRadius * Math.cos(phi * GridRenderer.DEG_TO_RAD);
                final double xl = xCentre + maxRadius * Math.sin(phi * GridRenderer.DEG_TO_RAD) * 1.05;
                final double yl = yCentre - maxRadius * Math.cos(phi * GridRenderer.DEG_TO_RAD) * 1.05;

                gc.strokeLine(xCentre, yCentre, x, y);

                gc.save();
                gc.setFont(yAxis.getTickLabelStyle().getFont());
                gc.setStroke(yAxis.getTickLabelStyle().getFill()); // TODO: why stroke rather than fill?
                gc.setLineDashes((double[]) null);
                gc.setTextBaseline(VPos.CENTER);
                if (phi < 350) {
                    if (phi < 20) {
                        gc.setTextAlign(TextAlignment.CENTER);
                    } else if (phi <= 160) {
                        gc.setTextAlign(TextAlignment.LEFT);
                    } else if (phi <= 200) {
                        gc.setTextAlign(TextAlignment.CENTER);
                    } else {
                        gc.setTextAlign(TextAlignment.RIGHT);
                    }
                    gc.strokeText(String.valueOf(phi), xl, yl);
                }
                gc.restore();
            }

            if (xAxis.isLogAxis() || verMinorGridStyleNode.isVisible()) {
                applyGraphicsStyleFromLineStyle(gc, verMinorGridStyleNode);
                xAxis.getMinorTickMarks().stream().mapToDouble(TickMark::getPosition).forEach(xPos -> {
                    if (xPos > 0 && xPos <= xAxisWidth) {
                        gc.strokeLine(xPos, 0, xPos, yAxisHeight);
                    }
                });
            }
        }

        drawPolarCircle(gc, yAxis, yRange, xCentre, yCentre, maxRadius);

        gc.restore();
    }

    protected void drawVerticalMajorGridLines(final GraphicsContext gc, final Axis xAxis, final double xAxisWidth,
            final double yAxisHeightSnapped) {
        if (!verMajorGridStyleNode.isVisible() && !verMinorGridStyleNode.isVisible()) {
            return;
        }
        final double zeroSnapped = snap(0);
        applyGraphicsStyleFromLineStyle(gc, verMajorGridStyleNode);
        ObservableList<TickMark> tickMarks = xAxis.getTickMarks();
        for (TickMark tickMark : tickMarks) {
            double x = snap(xAxis.getDisplayPosition(tickMark.getValue()));
            if (x > 0 && x <= xAxisWidth) {
                // gc.strokeLine(x, zeroSnapped, x, yAxisHeightSnapped);
                DashPatternStyle.strokeDashedLine(gc, x, zeroSnapped, x, yAxisHeightSnapped);
            }
        }
    }

    protected void drawVerticalMinorGridLines(final GraphicsContext gc, final Axis xAxis, final double xAxisWidth,
            final double yAxisHeightSnapped) {
        if (!xAxis.isLogAxis() && !verMinorGridStyleNode.isVisible()) {
            return;
        }
        final double zeroSnapped = snap(0);
        applyGraphicsStyleFromLineStyle(gc, verMinorGridStyleNode);
        ObservableList<TickMark> tickMarks = xAxis.getMinorTickMarks();
        for (TickMark tickMark : tickMarks) {
            double x = snap(xAxis.getDisplayPosition(tickMark.getValue()));
            if (x > 0 && x <= xAxisWidth) {
                // gc.strokeLine(x, zeroSnapped, x, yAxisHeightSnapped);
                DashPatternStyle.strokeDashedLine(gc, x, zeroSnapped, x, yAxisHeightSnapped);
            }
        }
    }

    /**
     * @return observable list of axes that are supposed to be used by the renderer
     */
    @Override
    public ObservableList<Axis> getAxes() {
        return axesList;
    }

    @Override
    public ObservableList<DataSet> getDatasets() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public ObservableList<DataSet> getDatasetsCopy() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public ObservableList<DataSetNode> getDatasetNodes() {
        return FXCollections.emptyObservableList();
    }

    /**
     * modify this to change drawing of horizontal major grid lines
     *
     * @return the Line node to be styled
     */
    public LineStyle getHorizontalMajorGrid() {
        return horMajorGridStyleNode;
    }

    /**
     * modify this to change drawing of horizontal minor grid lines
     *
     * @return the Line node to be styled
     */
    public LineStyle getHorizontalMinorGrid() {
        return horMinorGridStyleNode;
    }

    /**
     * modify this to change drawing of vertical major grid lines
     *
     * @return the Line node to be styled
     */
    public LineStyle getVerticalMajorGrid() {
        return verMajorGridStyleNode;
    }

    /**
     * modify this to change drawing of vertical minor grid lines
     *
     * @return the Line node to be styled
     */
    public LineStyle getVerticalMinorGrid() {
        return verMinorGridStyleNode;
    }

    /**
     * Indicates whether grid lines should be drawn on top or beneath graphs
     *
     * @param state true: draw on top
     */
    public final void setDrawOnTop(boolean state) {
        drawOnTopProperty().set(state);
    }

    /**
     * Indicates whether grid lines should be drawn on top or beneath graphs
     *
     * @return drawOnTop state
     */
    public final boolean isDrawOnTop() {
        return drawOnTopProperty().get();
    }

    /**
     * Indicates whether grid lines should be drawn on top or beneath graphs
     *
     * @return drawOnTop property
     */
    public final BooleanProperty drawOnTopProperty() {
        return drawGridOnTop;
    }

    @Override
    public void render(final GraphicsContext gc, final Chart chart, final int dataSetOffset) {
        if (!(chart instanceof XYChart)) {
            throw new InvalidParameterException(
                    "must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
        }
        final XYChart xyChart = (XYChart) chart;

        if (xyChart.isPolarPlot()) {
            drawPolarGrid(gc, xyChart);
        } else {
            drawEuclideanGrid(gc, xyChart);
        }
    }

    @Override
    public Renderer setShowInLegend(final boolean state) {
        return this;
    }

    @Override
    public boolean showInLegend() {
        return false;
    }

    @Override
    public BooleanProperty showInLegendProperty() {
        return null;
    }

    @Override
    public void setIndexOffset(int value) {
    }

    @Override
    public int getIndexOffset() {
        return 0;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS.getCssMetaData();
    }

    private static final CssPropertyFactory<GridRenderer> CSS = new CssPropertyFactory<>(Parent.getClassCssMetaData());

    protected static void applyGraphicsStyleFromLineStyle(final GraphicsContext gc, final LineStyle style) {
        style.copyStyleTo(gc);
    }

    private static double snap(final double value) {
        return (int) value + 0.5;
    }
}
