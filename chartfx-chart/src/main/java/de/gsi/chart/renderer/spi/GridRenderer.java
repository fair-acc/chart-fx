package de.gsi.chart.renderer.spi;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.css.converters.BooleanConverter;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.TickMark;
import de.gsi.dataset.DataSet;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.utils.DashPatternStyle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.SimpleStyleableBooleanProperty;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
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
    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("withMinor");
    private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
    static {
        final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Region.getClassCssMetaData());
        styleables.add(StyleableProperties.GRID_ON_TOP);
        STYLEABLES = Collections.unmodifiableList(styleables);
    }

    private static final double[] DEFAULT_GRID_DASH_PATTERM = { 4.5, 2.5 };
    protected final Chart baseChart;
    protected final BooleanProperty drawGridOnTop = new SimpleStyleableBooleanProperty(StyleableProperties.GRID_ON_TOP,
            this, "drawGridOnTop", true);
    private final Line horMajorGridStyleNode;
    private final Line verMajorGridStyleNode;
    private final Line horMinorGridStyleNode;
    private final Line verMinorGridStyleNode;
    private final Group gridStyleNodes = new Group();
    private boolean isPolarPlot = false;
    protected final ObservableList<Axis> axesList = FXCollections.observableArrayList();

    public GridRenderer(final XYChart chart) {
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

        gridStyleNodes.getChildren().addAll(horMajorGridStyleNode, verMajorGridStyleNode, horMinorGridStyleNode,
                verMinorGridStyleNode);

        getChildren().add(gridStyleNodes);
        final Scene scene = new Scene(this);
        scene.getStylesheets().add(GridRenderer.CHART_CSS);
        gridStyleNodes.applyCss();
        impl_reapplyCSS();
        final SetChangeListener<? super PseudoClass> listener = evt -> gridStyleNodes.applyCss();
        horMajorGridStyleNode.getPseudoClassStates().addListener(listener);
        verMajorGridStyleNode.getPseudoClassStates().addListener(listener);
        horMinorGridStyleNode.getPseudoClassStates().addListener(listener);
        verMinorGridStyleNode.getPseudoClassStates().addListener(listener);

        ChangeListener<? super Boolean> change = (ob, o, n) -> {
            horMajorGridStyleNode.pseudoClassStateChanged(GridRenderer.SELECTED_PSEUDO_CLASS, horMinorGridStyleNode.isVisible());
            verMajorGridStyleNode.pseudoClassStateChanged(GridRenderer.SELECTED_PSEUDO_CLASS, verMinorGridStyleNode.isVisible());
        };
        
        horizontalGridLinesVisibleProperty().addListener(change);
        verticalGridLinesVisibleProperty().addListener(change);
    }

    @Override
    public void render(final GraphicsContext gc, final Chart chart, final int dataSetOffset,
            final ObservableList<DataSet> datasets) {
        if (!(chart instanceof XYChart)) {
            throw new InvalidParameterException(
                    "must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
        }
        final XYChart xyChart = (XYChart) chart;
        isPolarPlot = xyChart.isPolarPlot();
      
        if (isPolarPlot) {
            drawPolarGrid(gc, xyChart);
        } else {
//        	 drawEuclideanGrid(gc, xyChart);
        	// for testing
            drawEuclideanGrid2(gc, xyChart);
        }

    }

    protected void applyGraphicsStyleFromLineStyle(final GraphicsContext gc, final Line style) {
        gc.setStroke(style.getStroke());
        gc.setLineWidth(style.getStrokeWidth());
        if (style.getStrokeDashArray() != null && !style.getStrokeDashArray().isEmpty()) {
            final double[] dashes = style.getStrokeDashArray().stream().mapToDouble(d -> d).toArray();
            gc.setLineDashes(dashes);
        } else {
            gc.setLineDashes(DEFAULT_GRID_DASH_PATTERM);
        }
    }

    private double snap(final double value) {
        return (int) value + 0.5;
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

                if (phi >= 0 && phi <= 360) {
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
            }

            if (xAxis instanceof Axis && xAxis.isLogAxis() || verMinorGridStyleNode.isVisible()) {
                applyGraphicsStyleFromLineStyle(gc, verMinorGridStyleNode);
                xAxis.getMinorTickMarks().stream().mapToDouble(TickMark::getPosition).forEach(xPos -> {
                    if (xPos > 0 && xPos <= xAxisWidth) {
                        gc.strokeLine(xPos, 0, xPos, yAxisHeight);
                    }
                });
            }
        }

        if (horMajorGridStyleNode.isVisible() || horMinorGridStyleNode.isVisible()) {
            applyGraphicsStyleFromLineStyle(gc, horMajorGridStyleNode);
            final ObservableList<TickMark> yTickMarks = yAxis.getTickMarks();

            gc.strokeOval(xCentre - maxRadius, yCentre - maxRadius, 2 * maxRadius, 2 * maxRadius);

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

            if (yAxis instanceof Axis && yAxis.isLogAxis() || horMinorGridStyleNode.isVisible()) {
                applyGraphicsStyleFromLineStyle(gc, horMinorGridStyleNode);

                yAxis.getMinorTickMarks().stream().mapToDouble(minorTick -> yRange - minorTick.getPosition())
                        .forEach(yPos -> {
                            final double yNorm = yPos / yRange * maxRadius;
                            if (yNorm >= 0 && yNorm < maxRadius) {
                                gc.strokeOval(xCentre - yNorm, yCentre - yNorm, 2 * yNorm, 2 * yNorm);
                            }
                        });
            }
        }

        gc.restore();
    }

    protected void drawEuclideanGrid(final GraphicsContext gc, XYChart xyChart) {
        final Axis xAxis = xyChart.getXAxis();
        final Axis yAxis = xyChart.getYAxis();
        final double xAxisWidth = xyChart.getCanvas().getWidth();
        final double yAxisHeight = xyChart.getCanvas().getHeight();
        if (xAxis instanceof Node) {
            ((Node) xAxis).setVisible(true);
        }

        gc.save();
        // draw vertical major grid lines
        if (verMajorGridStyleNode.isVisible() || verMinorGridStyleNode.isVisible()) {
            applyGraphicsStyleFromLineStyle(gc, verMajorGridStyleNode);
            xAxis.getTickMarks().stream().mapToDouble(tick -> xAxis.getDisplayPosition(tick.getValue()))
                    .forEach(xPos -> {
                        if (xPos > 0 && xPos <= xAxisWidth) {
                            gc.strokeLine(snap(xPos), snap(0), snap(xPos), snap(yAxisHeight));
                        }
                    });
        }

        // draw vertical minor grid lines
        if (xAxis instanceof Axis && xAxis.isLogAxis() || verMinorGridStyleNode.isVisible()) {
            applyGraphicsStyleFromLineStyle(gc, verMinorGridStyleNode);

            xAxis.getMinorTickMarks().stream().mapToDouble(TickMark::getPosition).forEach(xPos -> {
                if (xPos > 0 && xPos <= xAxisWidth) {
                    gc.strokeLine(snap(xPos), snap(0), snap(xPos), snap(yAxisHeight));
                }
            });
        }

        // draw horizontal major grid lines
        if (horMajorGridStyleNode.isVisible() || horMinorGridStyleNode.isVisible()) {
            applyGraphicsStyleFromLineStyle(gc, horMajorGridStyleNode);

            yAxis.getTickMarks().stream().mapToDouble(tick -> yAxis.getDisplayPosition(tick.getValue()))
                    .forEach(yPos -> {
                        if (yPos >= 0 && yPos < yAxisHeight) {
                            gc.strokeLine(snap(0), snap(yPos), snap(xAxisWidth), snap(yPos));
                        }
                    });
        }

        // draw horizontal minor grid lines
        if (yAxis instanceof Axis && yAxis.isLogAxis() || horMinorGridStyleNode.isVisible()) {
            applyGraphicsStyleFromLineStyle(gc, horMinorGridStyleNode);

            yAxis.getMinorTickMarks().stream().mapToDouble(TickMark::getPosition).forEach(yPos -> {
                if (yPos >= 0 && yPos < yAxisHeight) {
                    gc.strokeLine(snap(0), snap(yPos), snap(xAxisWidth), snap(yPos));
                }
            });
        }
        gc.restore();
    }
    
    protected void drawEuclideanGrid2(final GraphicsContext gc, XYChart xyChart) {
        final Axis xAxis = xyChart.getXAxis();
        final Axis yAxis = xyChart.getYAxis();
        final double xAxisWidth = xyChart.getCanvas().getWidth();
        final double xAxisWidthSnapped = snap(xAxisWidth);        
        final double yAxisHeight = xyChart.getCanvas().getHeight();
        final double yAxisHeightSnapped = snap(yAxisHeight);
        final double zeroSnapped = snap(0);
        if (xAxis instanceof Node) {
            ((Node) xAxis).setVisible(true);
        }

        gc.save();
        // draw vertical major grid lines
        if (verMajorGridStyleNode.isVisible() || verMinorGridStyleNode.isVisible()) {
            applyGraphicsStyleFromLineStyle(gc, verMajorGridStyleNode);
            ObservableList<TickMark> tickMarks = xAxis.getTickMarks();
            for (int i=0; i<tickMarks.size(); i++) {
                double x = snap(xAxis.getDisplayPosition(tickMarks.get(i).getValue()));
                if (x > 0 && x <= xAxisWidth) {
//                    gc.strokeLine(x, zeroSnapped, x, yAxisHeightSnapped);
                    DashPatternStyle.strokeDashedLine(gc, x, zeroSnapped, x, yAxisHeightSnapped);
                }
            }
        }

        // draw vertical minor grid lines
        if (xAxis instanceof Axis && xAxis.isLogAxis() || verMinorGridStyleNode.isVisible()) {
            applyGraphicsStyleFromLineStyle(gc, verMinorGridStyleNode);
            ObservableList<TickMark> tickMarks = xAxis.getMinorTickMarks();
            for (int i=0; i<tickMarks.size(); i++) {
                double x = snap(xAxis.getDisplayPosition(tickMarks.get(i).getValue()));
                if (x > 0 && x <= xAxisWidth) {
//                    gc.strokeLine(x, zeroSnapped, x, yAxisHeightSnapped);
                    DashPatternStyle.strokeDashedLine(gc, x, zeroSnapped, x, yAxisHeightSnapped);
                }
            }                       
        }

        // draw horizontal major grid lines
        if (horMajorGridStyleNode.isVisible() || horMinorGridStyleNode.isVisible()) {
            applyGraphicsStyleFromLineStyle(gc, horMajorGridStyleNode);
            ObservableList<TickMark> tickMarks = yAxis.getTickMarks();
            for (int i=0; i<tickMarks.size(); i++) {
                double y = snap(yAxis.getDisplayPosition(tickMarks.get(i).getValue()));
                if (y >= 0 && y < yAxisHeight) {
//                    gc.strokeLine(zeroSnapped, y, xAxisWidthSnapped, y);
                    DashPatternStyle.strokeDashedLine(gc, zeroSnapped, y, xAxisWidthSnapped, y);
                }
            }              

        }

        // draw horizontal minor grid lines
        if (yAxis instanceof Axis && yAxis.isLogAxis() || horMinorGridStyleNode.isVisible()) {
            applyGraphicsStyleFromLineStyle(gc, horMinorGridStyleNode);
            ObservableList<TickMark> tickMarks = yAxis.getMinorTickMarks();
            for (int i=0; i<tickMarks.size(); i++) {
                double y = snap(yAxis.getDisplayPosition(tickMarks.get(i).getValue()));
                if (y >= 0 && y < yAxisHeight) {
                    //gc.strokeLine(zeroSnapped, y, xAxisWidthSnapped, y);
                    DashPatternStyle.strokeDashedLine(gc, zeroSnapped, y, xAxisWidthSnapped, y);
                }
            }                      
        }
        gc.restore();
    }


    /**
     * @return observable list of axes that are supposed to be used by the renderer
     */
    @Override
    public ObservableList<Axis> getAxes() {
        return axesList;
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
     * Indicates whether horizontal major grid lines are visible or not.
     *
     * @return verticalGridLinesVisible property
     */
    public final BooleanProperty horizontalGridLinesVisibleProperty() {
        return horMajorGridStyleNode.visibleProperty();
    }

    /**
     * Indicates whether vertical minor grid lines are visible or not.
     *
     * @return verticalGridLinesVisible property
     */
    public final BooleanProperty verticalMinorGridLinesVisibleProperty() {
        return verMinorGridStyleNode.visibleProperty();
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
     * @return drawOnTop property
     */
    public final BooleanProperty drawOnTopProperty() {
        return drawGridOnTop;
    }

    /**
     * Indicates whether grid lines should be drawn on top or beneath graphs
     *
     * @return drawOnTop state
     */
    public final boolean isDrawOnTop() {
        return drawGridOnTop.get();
    }

    /**
     * Indicates whether grid lines should be drawn on top or beneath graphs
     *
     * @param state true: draw on top
     */
    public final void setDrawOnTop(boolean state) {
        drawGridOnTop.set(state);
    }

    @Override
    public ObservableList<DataSet> getDatasets() {
        return null;
    }

    @Override
    public ObservableList<DataSet> getDatasetsCopy() {
        return null;
    }

    @Override
    public BooleanProperty showInLegendProperty() {
        return null;
    }

    @Override
    public boolean showInLegend() {
        return false;
    }

    @Override
    public Renderer setShowInLegend(final boolean state) {
        return this;
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
     * modify this to change drawing of vertical major grid lines
     *
     * @return the Line node to be styled
     */
    public Line getVerticalMajorGrid() {
        return verMajorGridStyleNode;
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
     * modify this to change drawing of vertical minor grid lines
     *
     * @return the Line node to be styled
     */
    public Line getVerticalMinorGrid() {
        return verMinorGridStyleNode;
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return GridRenderer.STYLEABLES;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return GridRenderer.getClassCssMetaData();
    }

    private static class StyleableProperties {

        private static final CssMetaData<GridRenderer, Boolean> GRID_ON_TOP = new CssMetaData<GridRenderer, Boolean>(
                "-fx-grid-on-top", BooleanConverter.getInstance(), Boolean.TRUE, false) {

            @Override
            public boolean isSettable(final GridRenderer node) {
                return node.drawGridOnTop == null || !node.drawGridOnTop.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final GridRenderer node) {
                return (StyleableProperty<Boolean>) node.drawGridOnTop;
            }
        };
    }

    @Override
    public Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height) {
        // not applicable
        return null;
    }
}
