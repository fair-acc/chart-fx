package de.gsi.chart.renderer.spi;

import java.security.InvalidParameterException;
import java.util.List;

import com.sun.javafx.css.converters.BooleanConverter;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.TickMark;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.utils.DashPatternStyle;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.utils.NoDuplicatesList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.text.TextAlignment;

@SuppressWarnings("PMD.GodClass")
public class GridRenderer extends Pane implements Renderer {

    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static final String CHART_CSS = Chart.class.getResource("chart.css").toExternalForm();
    private static final String STYLE_CLASS_GRID_RENDERER = "grid-renderer";
    private static final String STYLE_CLASS_MAJOR_GRID_LINE = "chart-major-grid-lines";
    private static final String STYLE_CLASS_MAJOR_GRID_LINE_H = "chart-major-horizontal-lines";
    private static final String STYLE_CLASS_MAJOR_GRID_LINE_V = "chart-major-vertical-lines";
    private static final String STYLE_CLASS_MINOR_GRID_LINE = "chart-minor-grid-lines";
    private static final String STYLE_CLASS_MINOR_GRID_LINE_H = "chart-minor-horizontal-lines";
    private static final String STYLE_CLASS_MINOR_GRID_LINE_V = "chart-minor-vertical-lines";
    private static final String STYLE_CLASS_GRID_ON_TOP = "chart-grid-line-on-top";
    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("withMinor");

    private static final double[] DEFAULT_GRID_DASH_PATTERM = { 4.5, 2.5 };
    protected final Chart baseChart;
    // protected final BooleanProperty drawGridOnTop = new
    // SimpleStyleableBooleanProperty(StyleableProperties.GRID_ON_TOP,
    // this, "drawGridOnTop", true);
    private final Line horMajorGridStyleNode;
    private final Line verMajorGridStyleNode;
    private final Line horMinorGridStyleNode;
    private final Line verMinorGridStyleNode;
    private final Line drawGridOnTopNode;
    private final Group gridStyleNodes = new Group();
    protected final ObservableList<Axis> axesList = FXCollections.observableList(new NoDuplicatesList<Axis>());

    public GridRenderer(final XYChart chart) {
        super();
        if (chart == null) {
            throw new InvalidParameterException("chart must not be null");
        }
        baseChart = chart;
        getStylesheets().add(GridRenderer.CHART_CSS);
        getStyleClass().setAll(GridRenderer.STYLE_CLASS_GRID_RENDERER);
        horMajorGridStyleNode = new Line();
        horMajorGridStyleNode.getStyleClass().add(GridRenderer.STYLE_CLASS_MAJOR_GRID_LINE);
        horMajorGridStyleNode.getStyleClass().add(GridRenderer.STYLE_CLASS_MAJOR_GRID_LINE_H);

        verMajorGridStyleNode = new Line();
        verMajorGridStyleNode.getStyleClass().add(GridRenderer.STYLE_CLASS_MAJOR_GRID_LINE);
        verMajorGridStyleNode.getStyleClass().add(GridRenderer.STYLE_CLASS_MAJOR_GRID_LINE_V);

        horMinorGridStyleNode = new Line();
        horMinorGridStyleNode.getStyleClass().add(GridRenderer.STYLE_CLASS_MINOR_GRID_LINE);
        horMinorGridStyleNode.getStyleClass().add(GridRenderer.STYLE_CLASS_MINOR_GRID_LINE_H);
        horMinorGridStyleNode.setVisible(false);

        verMinorGridStyleNode = new Line();
        verMinorGridStyleNode.getStyleClass().add(GridRenderer.STYLE_CLASS_MINOR_GRID_LINE);
        verMinorGridStyleNode.getStyleClass().add(GridRenderer.STYLE_CLASS_MINOR_GRID_LINE_V);
        verMinorGridStyleNode.setVisible(false);

        drawGridOnTopNode = new Line();
        drawGridOnTopNode.getStyleClass().add(GridRenderer.STYLE_CLASS_GRID_ON_TOP);
        drawGridOnTopNode.getStyleClass().add(GridRenderer.STYLE_CLASS_GRID_ON_TOP);
        drawGridOnTopNode.setVisible(true);

        gridStyleNodes.getChildren().addAll(horMajorGridStyleNode, verMajorGridStyleNode, horMinorGridStyleNode,
                verMinorGridStyleNode, drawGridOnTopNode);

        getChildren().add(gridStyleNodes);
        final Scene scene = new Scene(this);
        scene.getStylesheets().add(GridRenderer.CHART_CSS);
        gridStyleNodes.applyCss();
        final SetChangeListener<? super PseudoClass> listener = evt -> gridStyleNodes.applyCss();
        horMajorGridStyleNode.getPseudoClassStates().addListener(listener);
        verMajorGridStyleNode.getPseudoClassStates().addListener(listener);
        horMinorGridStyleNode.getPseudoClassStates().addListener(listener);
        verMinorGridStyleNode.getPseudoClassStates().addListener(listener);
        drawGridOnTopNode.getPseudoClassStates().addListener(listener);

        ChangeListener<? super Boolean> change = (ob, o, n) -> {
            horMajorGridStyleNode.pseudoClassStateChanged(GridRenderer.SELECTED_PSEUDO_CLASS,
                    horMinorGridStyleNode.isVisible());
            verMajorGridStyleNode.pseudoClassStateChanged(GridRenderer.SELECTED_PSEUDO_CLASS,
                    verMinorGridStyleNode.isVisible());
            drawGridOnTopNode.pseudoClassStateChanged(GridRenderer.SELECTED_PSEUDO_CLASS,
                    drawGridOnTopNode.isVisible());
            chart.requestLayout();
        };

        horizontalGridLinesVisibleProperty().addListener(change);
        verticalGridLinesVisibleProperty().addListener(change);
        drawOnTopProperty().addListener(change);
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
        for (int i = 0; i < tickMarks.size(); i++) {
            double y = snap(yAxis.getDisplayPosition(tickMarks.get(i).getValue()));
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
        for (int i = 0; i < tickMarks.size(); i++) {
            double y = snap(yAxis.getDisplayPosition(tickMarks.get(i).getValue()));
            if (y >= 0 && y < yAxisHeight) {
                // gc.strokeLine(zeroSnapped, y, xAxisWidthSnapped, y);
                DashPatternStyle.strokeDashedLine(gc, zeroSnapped, y, xAxisWidthSnapped, y);
            }
        }
    }

    @Override
    public Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height) {
        // not applicable
        return null;
    }

    /**
     * Indicates whether grid lines should be drawn on top or beneath graphs
     *
     * @return drawOnTop property
     */
    public final BooleanProperty drawOnTopProperty() {
        return drawGridOnTopNode.visibleProperty();
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
                gc.setFont(yAxis.getTickLabelFont());
                gc.setStroke(yAxis.getTickLabelFill());
                gc.setLineDashes(null);
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
                gc.setFont(yAxis.getTickLabelFont());
                gc.setStroke(yAxis.getTickLabelFill());
                gc.setLineDashes(null);
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
        for (int i = 0; i < tickMarks.size(); i++) {
            double x = snap(xAxis.getDisplayPosition(tickMarks.get(i).getValue()));
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
        for (int i = 0; i < tickMarks.size(); i++) {
            double x = snap(xAxis.getDisplayPosition(tickMarks.get(i).getValue()));
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
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return GridRenderer.getClassCssMetaData();
    }

    @Override
    public ObservableList<DataSet> getDatasets() {
        return null;
    }

    @Override
    public ObservableList<DataSet> getDatasetsCopy() {
        return null;
    }

    /**
     * modify this to change drawing of horizontal major grid lines
     *
     * @return the Line node to be styled
     */
    public Line getHorizontalMajorGrid() {
        return horMajorGridStyleNode;
    }

    /**
     * modify this to change drawing of horizontal minor grid lines
     *
     * @return the Line node to be styled
     */
    public Line getHorizontalMinorGrid() {
        return horMinorGridStyleNode;
    }

    /**
     * modify this to change drawing of vertical major grid lines
     *
     * @return the Line node to be styled
     */
    public Line getVerticalMajorGrid() {
        return verMajorGridStyleNode;
    }

    /**
     * modify this to change drawing of vertical minor grid lines
     *
     * @return the Line node to be styled
     */
    public Line getVerticalMinorGrid() {
        return verMinorGridStyleNode;
    }

    /**
     * Indicates whether horizontal major grid lines are visible or not.
     *
     * @return verticalGridLinesVisible property
     */
    public final BooleanProperty horizontalGridLinesVisibleProperty() {
        return horMajorGridStyleNode.visibleProperty();
    }

    /**
     * Indicates whether horizontal minor grid lines are visible or not.
     *
     * @return verticalGridLinesVisible property
     */
    public final BooleanProperty horizontalMinorGridLinesVisibleProperty() {
        return horMinorGridStyleNode.visibleProperty();
    }

    /**
     * Indicates whether grid lines should be drawn on top or beneath graphs
     *
     * @return drawOnTop state
     */
    public final boolean isDrawOnTop() {
        return drawGridOnTopNode.isVisible();
    }

    @Override
    public void render(final GraphicsContext gc, final Chart chart, final int dataSetOffset,
            final ObservableList<DataSet> datasets) {
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

    /**
     * Indicates whether grid lines should be drawn on top or beneath graphs
     *
     * @param state true: draw on top
     */
    public final void setDrawOnTop(boolean state) {
        drawGridOnTopNode.setVisible(state);
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

    /**
     * Indicates whether vertical major grid lines are visible or not.
     *
     * @return verticalGridLinesVisible property
     */
    public final BooleanProperty verticalGridLinesVisibleProperty() {
        return verMajorGridStyleNode.visibleProperty();
    }

    /**
     * Indicates whether vertical minor grid lines are visible or not.
     *
     * @return verticalGridLinesVisible property
     */
    public final BooleanProperty verticalMinorGridLinesVisibleProperty() {
        return verMinorGridStyleNode.visibleProperty();
    }

    protected static void applyGraphicsStyleFromLineStyle(final GraphicsContext gc, final Line style) {
        gc.setStroke(style.getStroke());
        gc.setLineWidth(style.getStrokeWidth());
        if (style.getStrokeDashArray() == null || style.getStrokeDashArray().isEmpty()) {
            gc.setLineDashes(DEFAULT_GRID_DASH_PATTERM);
        } else {
            final double[] dashes = style.getStrokeDashArray().stream().mapToDouble(d -> d).toArray();
            gc.setLineDashes(dashes);
        }
    }

    private static double snap(final double value) {
        return (int) value + 0.5;
    }

}
