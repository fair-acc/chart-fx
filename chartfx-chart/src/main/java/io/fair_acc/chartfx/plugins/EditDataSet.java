package io.fair_acc.chartfx.plugins;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.util.Pair;

import org.kordamp.ikonli.javafx.FontIcon;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.EditConstraints;
import io.fair_acc.dataset.EditableDataSet;

/**
 *
 * Simple plugin to interact with DataSets implementing {@link EditableDataSet}.
 *
 * @author rstein
 */
public class EditDataSet extends TableViewer {
    protected static final String STYLE_CLASS_SELECT_RECT = "chart-select-rect";
    protected static final String STYLE_CLASS_SELECT_PATH = "chart-select-marker";
    protected static final PseudoClass NOEDIT_PSEUDO_CLASS = PseudoClass.getPseudoClass("noEdit");
    protected static final int DEFAULT_MARKER_RADIUS = 5;
    public static final int DEFAULT_PICKING_DISTANCE = 10;
    protected static final int SELECT_RECT_MIN_SIZE = 5;
    protected static final int FONT_SIZE_COMBO = FONT_SIZE - 4;
    private static boolean shiftDown;
    private static boolean controlDown;
    protected boolean isPointDragActive;
    // Default mouse select filter: left mouse- and control-button down (only).
    protected final Predicate<MouseEvent> defaultSelectFilter = event -> MouseEventsHelper.isOnlyPrimaryButtonDown(event) && event.isControlDown() && isMouseEventWithinCanvas(event) && !isPointDragActive;

    protected final ConcurrentHashMap<EditableDataSet, ConcurrentHashMap<Integer, SelectedDataPoint>> markedPoints = new ConcurrentHashMap<>();
    protected final Rectangle selectRectangle = new Rectangle();
    protected Point2D selectStartPoint;
    protected Point2D selectEndPoint;
    protected Cursor originalCursor;
    protected final ObjectProperty<Cursor> dragCursor = new SimpleObjectProperty<>(this, "dragCursor");
    protected BooleanProperty editEnable;
    protected BooleanProperty allowShiftX = new SimpleBooleanProperty(this, "allowShiftX", true);
    protected BooleanProperty allowShiftY = new SimpleBooleanProperty(this, "allowShiftY", true);
    protected Predicate<MouseEvent> zoomInMouseFilter = defaultSelectFilter;
    protected Pane markerPane = new Pane();
    private double mouseOriginX = -1;
    private double mouseOriginY = -1;
    protected PointEditionPopup popup = new PointEditionPopup();

    private final EventHandler<KeyEvent> keyReleasedHandler = keyEvent -> {
        if (keyEvent.getCode() == KeyCode.CONTROL) {
            uninstallCursor();
            controlDown = false;
        }

        if (keyEvent.getCode() == KeyCode.SHIFT) {
            shiftDown = false;
        }
    };

    private final EventHandler<KeyEvent> keyPressedHandler = keyEvent -> {
        if (keyEvent.getCode() == KeyCode.CONTROL) {
            installCursor();
            controlDown = true;
            isPointDragActive = false;
            keyEvent.consume();
        }

        if (keyEvent.getCode() == KeyCode.SHIFT) {
            shiftDown = true;
        }

        if (keyEvent.getCode() == KeyCode.ESCAPE && (!shiftDown || !controlDown)) {
            this.markerPane.getChildren().clear();
        }
    };

    private final EventHandler<MouseEvent> selectionStartHandler = event -> {
        if (getSelectionMouseFilter() == null || getSelectionMouseFilter().test(event)) {
            selectionStarted(event);
            event.consume();
        }

        if (event.isSecondaryButtonDown()) {
            // right mouse click on Marker
            if (event.getSource() != null && (event.getSource() instanceof SelectedDataPoint)) {
                popup.showPopup(event, (SelectedDataPoint) event.getSource());
                event.consume();
            } else {
                popup.showPopup(event, null);
                event.consume();
            }
        }
    };

    private final EventHandler<MouseEvent> selectionDragHandler = event -> {
        performPointDrag(event);

        if (selectionOngoing() && !isPointDragActive) {
            selectionDragged(event);
            event.consume();
        }
    };

    private final EventHandler<MouseEvent> selectionEndHandler = event -> {
        if (selectionOngoing() && !isPointDragActive) {
            selectionEnded();
            event.consume();
        }
    };

    /**
     * Creates an event handler that handles a mouse drag on the node.
     */
    protected EventHandler<MouseEvent> dragHandler = this::performPointDrag;

    private final DoubleProperty pickingDistance = new SimpleDoubleProperty(this, "pickingDistance",
            DEFAULT_PICKING_DISTANCE) {
        @Override
        public void set(final double newValue) {
            super.set(Math.max(1, newValue));
        }
    };

    public EditDataSet() {
        super();
        editable = true;

        setDragCursor(Cursor.CROSSHAIR);

        selectRectangle.setManaged(false);
        selectRectangle.getStyleClass().add(STYLE_CLASS_SELECT_RECT);
        getChartChildren().add(selectRectangle);

        registerMouseHandlers();
        registerKeyHandlers();

        markerPane.setManaged(false);

        // register marker pane
        chartProperty().addListener((change, o, n) -> {
            if (o != null) {
                o.getCanvasForeground().getChildren().remove(markerPane);
                o.getPlotArea().setBottom(null);
                // markerPane.prefWidthProperty().unbind();
                // markerPane.prefHeightProperty().unbind();
            }
            if (n != null) {
                n.getCanvasForeground().getChildren().add(markerPane);
                markerPane.toFront();
                markerPane.setVisible(true);
                // markerPane.prefWidthProperty().bind(n.getCanvas().widthProperty());
                // markerPane.prefHeightProperty().bind(n.getCanvas().heightProperty());
            }
        });
    }

    protected void addPoint(final double x, final double y) {
        if (!(getChart() instanceof XYChart)) {
            return;
        }
        final XYChart xyChart = (XYChart) getChart();

        // TODO: tidy up code and make it compatible with multiple renderer

        // find data set closes to screen coordinate x & y
        final Pane pane = getChart().getCanvasForeground();
        final Bounds bounds = pane.getBoundsInLocal();
        final Bounds screenBounds = pane.localToScreen(bounds);
        final int x0 = (int) screenBounds.getMinX();
        final int y0 = (int) screenBounds.getMinY();
        final DataPoint dataPoint = findNearestDataPoint(getChart(), new Point2D(x - x0, y - y0));

        if (dataPoint != null && (dataPoint.getDataSet() instanceof EditableDataSet)) {
            final Axis xAxis = xyChart.getFirstAxis(Orientation.HORIZONTAL);
            final Axis yAxis = xyChart.getFirstAxis(Orientation.VERTICAL);
            final int index = dataPoint.getIndex();
            final double newValX = xAxis.getValueForDisplay(x - x0);
            final double newValY = yAxis.getValueForDisplay(y - y0);
            final EditableDataSet ds = (EditableDataSet) (dataPoint.getDataSet());
            final double oldValX = ds.get(DataSet.DIM_X, index);
            if (oldValX <= newValX) {
                ds.add(index, newValX, newValY);
            } else {
                ds.add(index - 1, newValX, newValY);
            }
        }

        updateMarker();
    }

    protected void applyDrag(final double deltaX, final double deltaY) {
        for (final Node node : markerPane.getChildren()) {
            if (!(node instanceof SelectedDataPoint)) {
                continue;
            }
            final SelectedDataPoint marker = (SelectedDataPoint) node;
            marker.applyDrag(deltaX, deltaY);
        }
        this.updateMarker();
    }

    protected void deleteAllMarkedPoints() {
        for (final EditableDataSet dataSet : markedPoints.keySet()) {
            final ConcurrentHashMap<Integer, SelectedDataPoint> dataPoints = markedPoints.get(dataSet);
            for (final Integer dataPointIndex : dataPoints.keySet()) {
                final SelectedDataPoint dataPoint = dataPoints.get(dataPointIndex);

                if (dataPoint.delete()) {
                    dataPoints.remove(dataPointIndex);
                }
            }
        }
        updateMarker();
    }

    /**
     * Mouse cursor to be used during drag operation.
     *
     * @return the mouse cursor property
     */
    public final ObjectProperty<Cursor> dragCursorProperty() {
        return dragCursor;
    }

    /**
     * @return whether points can be edited
     */
    public final ReadOnlyBooleanProperty editEnableProperty() {
        return editEnable;
    }

    protected void findDataPoint(final Axis xAxis, final Axis yAxis, final List<DataSet> dataSets) {
        if (xAxis == null || yAxis == null || dataSets == null) {
            return;
        }
        final double xMinScreen = Math.min(selectStartPoint.getX(), selectEndPoint.getX());
        final double xMaxScreen = Math.max(selectStartPoint.getX(), selectEndPoint.getX());
        final double yMinScreen = Math.min(selectStartPoint.getY(), selectEndPoint.getY());
        final double yMaxScreen = Math.max(selectStartPoint.getY(), selectEndPoint.getY());

        for (final DataSet ds : dataSets) {
            if (!(ds instanceof EditableDataSet)) {
                continue;
            }
            final EditableDataSet dataSet = (EditableDataSet) ds;

            final int indexMin = Math.max(0, ds.getIndex(DataSet.DIM_X, xAxis.getValueForDisplay(xMinScreen)));
            final int indexMax = Math.min(ds.getIndex(DataSet.DIM_X, xAxis.getValueForDisplay(xMaxScreen)) + 1,
                    ds.getDataCount());

            // N.B. (0,0) screen coordinate is in the top left corner vs. normal
            // 0,0 in the bottom left -> need to invert limits
            final double yMax = yAxis.getValueForDisplay(yMinScreen);
            final double yMin = yAxis.getValueForDisplay(yMaxScreen);

            final ConcurrentHashMap<Integer, SelectedDataPoint> dataSetHashMap = markedPoints.computeIfAbsent(dataSet,
                    k -> new ConcurrentHashMap<>());
            for (int i = indexMin; i < indexMax; i++) {
                final double y = dataSet.get(DataSet.DIM_Y, i);
                if ((y >= yMin) && (y <= yMax)) {
                    if (isShiftDown()) {
                        // add if not existing/remove if existing
                        if (dataSetHashMap.get(i) != null) {
                            dataSetHashMap.remove(i);
                        } else {
                            dataSetHashMap.put(i, new SelectedDataPoint(xAxis, yAxis, dataSet, i));
                        }
                    } else {
                        dataSetHashMap.put(i, new SelectedDataPoint(xAxis, yAxis, dataSet, i));
                    }
                }
            }
        }
    }

    protected DataPoint findNearestDataPoint(final Chart chart, final Point2D mouseLocation) {
        if (!(chart instanceof XYChart)) {
            return null;
        }
        final XYChart xyChart = (XYChart) chart;
        // TODO: iterate through all axes, renderer and datasets
        final double xValue = xyChart.getXAxis().getValueForDisplay(mouseLocation.getX());

        DataPoint nearestDataPoint = null;
        for (final DataPoint dataPoint : findNeighborPoints(xyChart, xValue)) {
            if (getChart().getFirstAxis(Orientation.HORIZONTAL) != null) {
                final double x = xyChart.getXAxis().getDisplayPosition(dataPoint.getX());
                final double y = xyChart.getYAxis().getDisplayPosition(dataPoint.getY());
                final Point2D displayPoint = new Point2D(x, y);
                dataPoint.setDistanceFromMouse(displayPoint.distance(mouseLocation));
                if ((nearestDataPoint == null
                            || dataPoint.getDistanceFromMouse() < nearestDataPoint.getDistanceFromMouse())) {
                    nearestDataPoint = dataPoint;
                }
            }
        }
        return nearestDataPoint;
    }

    /**
     * Handles series that have data sorted or not sorted with respect to X coordinate.
     *
     * @param dataSet data set
     * @param searchedX X coordinates
     * @return pair of neighbouring data points
     */
    protected Pair<DataPoint, DataPoint> findNeighborPoints(final DataSet dataSet, final double searchedX) {
        int prevIndex = -1;
        int nextIndex = -1;
        double prevX = Double.MIN_VALUE;
        double nextX = Double.MAX_VALUE;

        for (int i = 0; i < dataSet.getDataCount(); i++) {
            final double currentX = dataSet.get(DataSet.DIM_X, i);

            if (currentX <= searchedX) {
                if (prevX <= currentX) {
                    prevIndex = i;
                    prevX = currentX;
                }
            } else if (nextX > currentX) {
                nextIndex = i;
                nextX = currentX;
            }
        }
        final DataPoint prevPoint = prevIndex == -1 ? null
                                                    : new DataPoint(getChart(), dataSet, prevIndex, dataSet.get(DataSet.DIM_X, prevIndex),
                                                            dataSet.get(DataSet.DIM_Y, prevIndex), dataSet.getDataLabel(prevIndex));
        final DataPoint nextPoint = nextIndex == -1 || nextIndex == prevIndex ? null
                                                                              : new DataPoint(getChart(), dataSet, nextIndex, dataSet.get(DataSet.DIM_X, nextIndex),
                                                                                      dataSet.get(DataSet.DIM_Y, nextIndex), dataSet.getDataLabel(nextIndex));

        return new Pair<>(prevPoint, nextPoint);
    }

    protected List<DataPoint> findNeighborPoints(final XYChart chart, final double searchedX) {
        final List<DataPoint> points = new LinkedList<>();
        for (final DataSet dataSet : chart.getAllDatasets()) {
            final Pair<DataPoint, DataPoint> neighborPoints = findNeighborPoints(dataSet, searchedX);
            if (neighborPoints.getKey() != null) {
                points.add(neighborPoints.getKey());
            }
            if (neighborPoints.getValue() != null) {
                points.add(neighborPoints.getValue());
            }
        }
        return points;
    }

    /**
     * Returns the value of the {@link #dragCursorProperty()}
     *
     * @return the current cursor
     */
    public final Cursor getDragCursor() {
        return dragCursorProperty().get();
    }

    enum ShiftConstraint {
        SHIFTXY,
        SHIFTX,
        SHIFTY
    }

    static class ShiftConstraintListCell extends ListCell<ShiftConstraint> {
        public ShiftConstraintListCell() {
            super();
            setContentDisplay(ContentDisplay.LEFT);
        }

        @Override
        protected void updateItem(ShiftConstraint shiftConstraint, boolean empty) {
            super.updateItem(shiftConstraint, empty);
            if (shiftConstraint == null || empty) {
                setGraphic(null);
                return;
            }
            FontIcon result;
            Tooltip tooltip = new Tooltip();
            switch (shiftConstraint) {
            case SHIFTX:
                result = new FontIcon("fa-arrows-h:" + FONT_SIZE_COMBO);
                tooltip.setText("Allow to modify the points's x values");
                setText("shift x");
                break;
            case SHIFTXY:
                result = new FontIcon("fa-arrows:" + FONT_SIZE_COMBO);
                tooltip.setText("Allow to modify the points freely");
                setText("shift xy");
                break;
            case SHIFTY:
                result = new FontIcon("fa-arrows-v:" + FONT_SIZE_COMBO);
                tooltip.setText("Allow to modify the point's y values");
                setText("shift y");
                break;
            default:
                result = new FontIcon("fa-question-circle-o:" + FONT_SIZE_COMBO);
                setText("-");
            }
            result.setFill(Color.DARKBLUE);
            setGraphic(result);
        }
    }

    @Override
    protected HBox getInteractorBar() {
        final HBox interactorBar = super.getInteractorBar();

        final FontIcon editIcon = new FontIcon("fa-edit:" + FONT_SIZE);
        final FontIcon addIcon = new FontIcon("fa-plus-circle:" + FONT_SIZE);
        final FontIcon removeIcon = new FontIcon("fa-minus-circle:" + FONT_SIZE);

        final Button editButton = new Button(null, editIcon);
        editButton.setPadding(new Insets(3, 3, 3, 3));
        editButton.setTooltip(new Tooltip("enables edit interactor"));
        interactorBar.getChildren().add(editButton);
        // editButton.setOnAction(evt -> {
        // editEnable.set(!editEnable.get());
        // });

        final Button addButton = new Button(null, addIcon);
        addIcon.setFill(Color.DARKGREEN);
        addButton.setPadding(new Insets(3, 3, 3, 3));
        addButton.setTooltip(new Tooltip("add data point"));
        interactorBar.getChildren().add(addButton);

        final Button removeButton = new Button(null, removeIcon);
        removeIcon.setFill(Color.DARKRED);
        removeButton.setPadding(new Insets(3, 3, 3, 3));
        removeButton.setTooltip(new Tooltip("remove data point"));
        interactorBar.getChildren().add(removeButton);

        final ComboBox<ShiftConstraint> comBox = new ComboBox<>();
        comBox.setPrefSize(-1, -1);
        comBox.setPadding(Insets.EMPTY);
        comBox.setBorder(null);
        comBox.setValue(ShiftConstraint.SHIFTXY);
        comBox.getItems().addAll(ShiftConstraint.values());
        comBox.setButtonCell(new ShiftConstraintListCell());
        comBox.setCellFactory((listView) -> new ShiftConstraintListCell());
        comBox.valueProperty().addListener((ch, o, n) -> {
            if (n == null) {
                return;
            }
            switch (n) {
            case SHIFTX: // allow shifts in X
                allowShiftX.set(true);
                allowShiftY.set(false);
                break;
            case SHIFTY: // allow shifts in Y
                allowShiftX.set(false);
                allowShiftY.set(true);
                break;
            case SHIFTXY:
            default:
                allowShiftX.set(true);
                allowShiftY.set(true);
                break;
            }
        });
        interactorBar.getChildren().add(comBox);

        if (editEnable == null) {
            editEnable = new SimpleBooleanProperty(this, "editEnable", false);
        }
        editEnable.addListener((ch, o, n) -> {
            removeButton.setDisable(!n);
            comBox.setDisable(!n);
        });

        return interactorBar;
    }

    /**
     * Returns the value of the {@link #pickingDistanceProperty()}.
     *
     * @return the current picking distance
     */
    public final double getPickingDistance() {
        return pickingDistanceProperty().get();
    }

    /**
     * Returns zoom-in mouse event filter.
     *
     * @return zoom-in mouse event filter
     * @see #setZoomInMouseFilter(Predicate)
     */
    public Predicate<MouseEvent> getSelectionMouseFilter() {
        return zoomInMouseFilter;
    }

    protected void installCursor() {
        final Region chart = getChart();
        originalCursor = chart.getCursor();
        if (getDragCursor() != null) {
            chart.setCursor(getDragCursor());
        }
    }

    /**
     * Returns the value of the {@link #editEnableProperty()}
     *
     * @return true: can edit point
     */
    public final boolean isEditable() {
        return editEnableProperty().get();
    }

    protected void performPointDrag(final MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && !controlDown && isPointDragActive) {
            if (mouseOriginX < 0) {
                mouseOriginX = event.getSceneX();
            }
            if (mouseOriginY < 0) {
                mouseOriginY = event.getSceneY();
            }
            final double deltaX = event.getSceneX() - mouseOriginX;
            final double deltaY = event.getSceneY() - mouseOriginY;

            // get the latest mouse coordinate.
            mouseOriginX = event.getSceneX();
            mouseOriginY = event.getSceneY();

            applyDrag(deltaX, deltaY);
            event.consume();
        } else {
            isPointDragActive = false;
        }
    }

    protected void performSelection() {
        if (!(getChart() instanceof XYChart)) {
            return;
        }
        final XYChart xyChart = (XYChart) getChart();

        if (!isShiftDown()) {
            markedPoints.clear();
        }

        findDataPoint(xyChart.getFirstAxis(Orientation.HORIZONTAL), xyChart.getFirstAxis(Orientation.VERTICAL),
                xyChart.getDatasets());

        for (final Renderer rend : xyChart.getRenderers()) {
            final ObservableList<Axis> axes = rend.getAxes();
            findDataPoint(getFirstAxis(axes, Orientation.HORIZONTAL), getFirstAxis(axes, Orientation.VERTICAL),
                    rend.getDatasets());
        }

        editEnable.set(!markedPoints.isEmpty());

        updateMarker();
    }

    /**
     * Distance of the mouse cursor from the data point (expressed in display units) that should trigger showing the
     * tool tip. By default initialised to {@value #DEFAULT_PICKING_DISTANCE}.
     *
     * @return the picking distance property
     */
    public final DoubleProperty pickingDistanceProperty() {
        return pickingDistance;
    }

    private void registerKeyHandlers() {
        registerInputEventHandler(KeyEvent.KEY_PRESSED, keyPressedHandler);
        registerInputEventHandler(KeyEvent.KEY_RELEASED, keyReleasedHandler);
    }

    private void registerMouseHandlers() {
        registerInputEventHandler(MouseEvent.MOUSE_PRESSED, selectionStartHandler);
        registerInputEventHandler(MouseEvent.MOUSE_DRAGGED, selectionDragHandler);
        registerInputEventHandler(MouseEvent.MOUSE_RELEASED, selectionEndHandler);
    }

    protected void selectionDragged(final MouseEvent event) {
        final Bounds plotAreaBounds = getChart().getPlotArea().getBoundsInLocal();
        selectEndPoint = limitToPlotArea(event, plotAreaBounds);

        selectRectangle.setX(Math.min(selectStartPoint.getX(), selectEndPoint.getX())); // zoomRectX
        selectRectangle.setY(Math.min(selectStartPoint.getY(), selectEndPoint.getY())); // zoomRectY
        selectRectangle.setWidth(Math.abs(selectEndPoint.getX() - selectStartPoint.getX())); // zoomRectWidth
        selectRectangle.setHeight(Math.abs(selectEndPoint.getY() - selectStartPoint.getY())); // zoomRectHeight
    }

    protected void selectionEnded() {
        selectRectangle.setVisible(false);
        if (selectRectangle.getWidth() > SELECT_RECT_MIN_SIZE && selectRectangle.getHeight() > SELECT_RECT_MIN_SIZE) {
            performSelection();
        }
        selectStartPoint = selectEndPoint = null;
        // uninstallCursor();
    }

    protected boolean selectionOngoing() {
        return selectStartPoint != null;
    }

    protected void selectionStarted(final MouseEvent event) {
        selectStartPoint = new Point2D(event.getX(), event.getY());

        selectRectangle.setX(selectStartPoint.getX());
        selectRectangle.setY(selectStartPoint.getY());
        selectRectangle.setWidth(0);
        selectRectangle.setHeight(0);
        selectRectangle.setVisible(true);
        // installCursor();
    }

    /**
     * Sets value of the {@link #dragCursorProperty()}.
     *
     * @param cursor the cursor to be used by the plugin
     */
    public final void setDragCursor(final Cursor cursor) {
        dragCursorProperty().set(cursor);
    }

    /**
     * Sets the value of {@link #pickingDistanceProperty()}.
     *
     * @param distance the new picking distance
     */
    public final void setPickingDistance(final double distance) {
        pickingDistanceProperty().set(distance);
    }

    /**
     * Sets filter on {@link MouseEvent#DRAG_DETECTED DRAG_DETECTED} events that should start zoom-in operation.
     *
     * @param zoomInMouseFilter the filter to accept zoom-in mouse event. If {@code null} then any DRAG_DETECTED event
     *        will start zoom-in operation. By default it's set to {@link #defaultSelectFilter}.
     * @see #getSelectionMouseFilter()
     */
    public void setZoomInMouseFilter(final Predicate<MouseEvent> zoomInMouseFilter) {
        this.zoomInMouseFilter = zoomInMouseFilter;
    }

    /**
     * Creates an event handler that handles a mouse press on the node.
     *
     * @param dataPoint corresponding to clicked data point
     * @return the event handler.
     */
    protected EventHandler<MouseEvent> startDragHandler(final SelectedDataPoint dataPoint) {
        return event -> {
            if (event.isPrimaryButtonDown() && !controlDown && !shiftDown) {
                // start drag
                isPointDragActive = true;

                // get the current mouse coordinates according to the scene.
                mouseOriginX = event.getSceneX();
                mouseOriginY = event.getSceneY();

                dataPoint.setCursor(Cursor.CLOSED_HAND);
                event.consume();
            }

            if (event.isSecondaryButtonDown()) {
                // right mouse click on Marker show popup;
                popup.showPopup(event, dataPoint);
            }
        };
    }

    protected void uninstallCursor() {
        getChart().setCursor(originalCursor);
    }

    protected void updateMarker() {
        this.markerPane.getChildren().clear();

        markerPane.getParent().setMouseTransparent(false);

        for (final EditableDataSet dataSet : markedPoints.keySet()) {
            final ConcurrentHashMap<Integer, SelectedDataPoint> dataPoints = markedPoints.get(dataSet);
            if (dataPoints == null) {
                // workaround... key shouldn't be here in the first place
                continue;
            }
            for (final Integer dataPointIndex : dataPoints.keySet()) {
                final SelectedDataPoint dataPoint = dataPoints.get(dataPointIndex);

                // dataPoint.setOnMouseClicked(evt -> {
                // if (evt.isSecondaryButtonDown()) {
                // // right clicked on circle
                // }
                // });

                dataPoint.update();
                markerPane.getChildren().add(dataPoint);
            }
        }
        if (markerPane.getChildren().isEmpty()) {
            markerPane.getParent().setMouseTransparent(true);
        }
    }

    private static Axis getFirstAxis(final List<Axis> axes, final Orientation orientation) {
        for (final Axis axis : axes) {
            if (axis.getSide() == null) {
                continue;
            }
            switch (orientation) {
            case VERTICAL:
                if (axis.getSide().isVertical()) {
                    return axis;
                }
                break;
            case HORIZONTAL:
            default:
                if (axis.getSide().isHorizontal()) {
                    return axis;
                }
                break;
            }
        }
        return null;
    }

    public static boolean isControlDown() {
        return controlDown;
    }

    public static boolean isShiftDown() {
        return shiftDown;
    }

    /**
     * limits the mouse event position to the min/max range of the canvas (N.B. event can occur to be
     * negative/larger/outside than the canvas) This is to avoid zooming outside the visible canvas range
     *
     * @param event the mouse event
     * @param plotBounds of the canvas
     * @return the clipped mouse location
     */
    private static Point2D limitToPlotArea(final MouseEvent event, final Bounds plotBounds) {
        final double limitedX = Math.max(Math.min(event.getX() - plotBounds.getMinX(), plotBounds.getMaxX()),
                plotBounds.getMinX());
        final double limitedY = Math.max(Math.min(event.getY() - plotBounds.getMinY(), plotBounds.getMaxY()),
                plotBounds.getMinY());
        return new Point2D(limitedX, limitedY);
    }

    public static class DataPoint {
        private final Chart chart;
        private final double x;
        private final double y;
        private final String label;
        private double distanceFromMouse;
        private final DataSet dataSet;
        private final int index;

        public DataPoint(final Chart chart, final DataSet dataSet, final int index, final double x, final double y,
                final String label) {
            this.chart = chart;
            this.dataSet = dataSet;
            this.index = index;
            this.x = x;
            this.y = y;
            this.label = label;
        }

        public Chart getChart() {
            return chart;
        }

        public DataSet getDataSet() {
            return dataSet;
        }

        public double getDistanceFromMouse() {
            return distanceFromMouse;
        }

        public int getIndex() {
            return index;
        }

        public String getLabel() {
            return label;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public void setDistanceFromMouse(final double distance) {
            distanceFromMouse = distance;
        }

        @Override
        public String toString() {
            return "DataSet= '" + dataSet.getName() + "' index=" + index;
        }
    }

    protected class PointEditionPopup extends Popup {
        private final Button addPoint = new Button("add");
        private final Button deletePoint = new Button("delete");
        private final Button deletePoints = new Button("delete all");

        public PointEditionPopup() {
            super();
            setAutoFix(true);
            setAutoHide(true);
            setHideOnEscape(true);
            setAutoHide(true);
            getContent().add(initContent());

            addPoint.setOnAction(evt -> {
                final double x = getX();
                final double y = getY();
                addPoint(x, y);
            });
            deletePoints.setOnAction(evt -> deleteAllMarkedPoints());
        }

        private VBox initContent() {
            final VBox pane = new VBox();
            pane.getChildren().add(new Label("popup"));
            pane.getChildren().add(addPoint);
            pane.getChildren().add(deletePoint);
            pane.getChildren().add(deletePoints);

            return pane;
        }

        public void showPopup(final MouseEvent event, final SelectedDataPoint selectedPoint) {
            deletePoints.setDisable(markerPane.getChildren().isEmpty());

            if (selectedPoint == null) {
                deletePoint.setDisable(true);
            } else {
                deletePoint.setDisable(false);
                deletePoint.setOnAction(evt -> {
                    if (selectedPoint.delete()) {
                        markedPoints.get(selectedPoint.getDataSet()).remove(selectedPoint.getIndex());
                    }
                    updateMarker();
                });
            }

            show(getChart().getScene().getWindow(), event.getScreenX(), event.getScreenY());
        }
    }

    /**
     * Finds the index of the given x and y values within a dataSet. This exists to
     * allow subclasses to easier implement specialized use cases such as searching
     * in sorted data sets, ignoring one axis, or to allow nan equality checks.
     *
     * @param dataSet     dataSet
     * @param cachedIndex result of previous search, or -1 if not available
     * @param x           desired x value
     * @param y           desired y value
     * @return index of the data point or -1 if not found
     */
    protected int findIndex(DataSet dataSet, int cachedIndex, double x, double y) {
        // fast path: cached index matches
        if (cachedIndex >= 0 && cachedIndex < dataSet.getDataCount() && dataSet.get(DataSet.DIM_X, cachedIndex) == x && dataSet.get(DataSet.DIM_Y, cachedIndex) == y) {
            return cachedIndex;
        }

        // slow path: index may have changed, so search again
        for (int i = 0; i < dataSet.getDataCount(); i++) {
            if (dataSet.get(DataSet.DIM_X, i) == x && dataSet.get(DataSet.DIM_Y, i) == y) {
                return i;
            }
        }
        return -1;
    }

    protected class SelectedDataPoint extends Circle {
        private final Axis xAxis;
        private final Axis yAxis;
        private final EditableDataSet dataSet;
        private double xValue;
        private double yValue;
        private int cachedIndex;

        SelectedDataPoint(final Axis xAxis, final Axis yAxis, final EditableDataSet dataSet, final int index) {
            super();
            getStyleClass().add(STYLE_CLASS_SELECT_PATH);
            // this.setPickOnBounds(true);
            setManaged(false);

            final EditConstraints constraints = dataSet.getEditConstraints();
            if (constraints == null) {
                pseudoClassStateChanged(NOEDIT_PSEUDO_CLASS, false);
            } else {
                final boolean canChange = constraints.canChange(index);
                if (!canChange) {
                    pseudoClassStateChanged(NOEDIT_PSEUDO_CLASS, true);
                }
            }

            this.xAxis = xAxis;
            this.yAxis = yAxis;
            this.dataSet = dataSet;
            this.xValue = dataSet.get(DataSet.DIM_X, index);
            this.yValue = dataSet.get(DataSet.DIM_Y, index);
            this.cachedIndex = index;
            this.setCenterX(getX()); // NOPMD by rstein on 13/06/19 14:14
            this.setCenterY(getY()); // NOPMD by rstein on 13/06/19 14:14
            this.setRadius(DEFAULT_MARKER_RADIUS);

            final EventHandler<? super InputEvent> dragOver = e -> {
                // finished drag
                isPointDragActive = false;
                setCursor(Cursor.DEFAULT);
            };

            setOnMouseEntered(e -> setCursor(Cursor.OPEN_HAND));
            addEventFilter(MouseDragEvent.MOUSE_DRAG_OVER, dragOver);

            setOnMousePressed(startDragHandler(this));
            // setOnMouseDragged(dragHandler(this));
            setOnMouseReleased(dragOver);
            setOnMouseDragOver(dragOver);
            // this.setOnMouseExited(dragOver);

            // TODO: what are these?
//            xAxis.addListener(evt -> FXUtils.runFX(() -> this.setCenterX(getX())));
//            yAxis.addListener(evt -> FXUtils.runFX(() -> this.setCenterY(getY())));
//            dataSet.addListener(e -> FXUtils.runFX(this::update));
        }

        public void applyDrag(final double deltaX, final double deltaY) {
            // TODO: replace with dataSetNode.isVisible() - it uses XYChart::getDataSets(), so maybe needs more refactoring
            /*if (!dataSet.isVisible()) {
                return;
            }*/

            double nX = getX();
            double nY = getY();
            int index = getIndex();

            final EditConstraints constraints = dataSet.getEditConstraints();
            if (constraints == null) {
                if (allowShiftX.get()) {
                    nX += deltaX;
                }

                if (allowShiftY.get()) {
                    nY += deltaY;
                }

                final double x = xAxis.getValueForDisplay(nX);
                final double y = yAxis.getValueForDisplay(nY);
                dataSet.set(index, x, y);
                xValue = x;
                yValue = y;
                return;
            }
            final boolean canChange = constraints.canChange(index);

            if (canChange && constraints.isEditable(DataSet.DIM_X) && allowShiftX.get()) {
                nX += deltaX;
            }

            if (canChange && constraints.isEditable(DataSet.DIM_Y) && allowShiftY.get()) {
                nY += deltaY;
            }

            final double x = xAxis.getValueForDisplay(nX);
            final double y = yAxis.getValueForDisplay(nY);
            dataSet.set(index, x, y);
            xValue = x;
            yValue = y;

            // update();
        }

        public boolean delete() {
            // TODO: replace with dataSetNode.isVisible() - it uses XYChart::getDataSets(), so maybe needs more refactoring
            /*if (!dataSet.isVisible()) {
                return false;
            }*/

            final EditConstraints constraints = dataSet.getEditConstraints();
            int index = getIndex();
            if (constraints == null || constraints.canDelete(index)) {
                dataSet.remove(index);
                return true;
            }
            return false;
        }

        public EditableDataSet getDataSet() {
            return dataSet;
        }

        public int getIndex() {
            cachedIndex = findIndex(dataSet, cachedIndex, xValue, yValue);
            return cachedIndex;
        }

        public double getX() {
            return xAxis.getDisplayPosition(xValue);
        }

        public double getY() {
            return yAxis.getDisplayPosition(yValue);
        }

        @Override
        public String toString() {
            return "selected index=" + getIndex();
        }

        public void update() {
            setCenterX(getX());
            setCenterY(getY());
        }
    }
}
