package de.gsi.chart.plugins;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.EditConstraints;
import de.gsi.dataset.EditableDataSet;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.utils.FXUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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

/**
 * @author rstein
 */
public class EditDataSet extends TableViewer {

    private static final String STYLE_CLASS_SELECT_RECT = "chart-select-rect";
    private static final String STYLE_CLASS_SELECT_PATH = "chart-select-marker";
    private static final int DEFAULT_MARKER_RADIUS = 5;
    public static final int DEFAULT_PICKING_DISTANCE = 10;
    private static final int SELECT_RECT_MIN_SIZE = 5;
    private static final int FONT_SIZE_COMBO = FONT_SIZE - 4;
    private static boolean isShiftDown;
    private static boolean isControlDown;
    private boolean isPointDragActive;
    // Default mouse select filter: left mouse- and control-button down (only).
    private final Predicate<MouseEvent> defaultSelectFilter = event -> MouseEventsHelper.isOnlyPrimaryButtonDown(event)
            && event.isControlDown() && isMouseEventWithinCanvas(event) && !isPointDragActive;

    protected ConcurrentHashMap<EditableDataSet, ConcurrentHashMap<Integer, SelectedDataPoint>> markedPoints = new ConcurrentHashMap<>();
    protected final Rectangle selectRectangle = new Rectangle();
    protected Point2D selectStartPoint = null;
    protected Point2D selectEndPoint = null;
    protected Cursor originalCursor;
    protected final ObjectProperty<Cursor> dragCursor = new SimpleObjectProperty<>(this, "dragCursor");
    protected BooleanProperty editEnable;
    protected BooleanProperty allowShiftX = new SimpleBooleanProperty(this, "allowShiftX", true);
    protected BooleanProperty allowShiftY = new SimpleBooleanProperty(this, "allowShiftY", true);
    protected Predicate<MouseEvent> zoomInMouseFilter = defaultSelectFilter;
    protected Pane markerPane = new Pane();
    protected PointEditionPopup popup = new PointEditionPopup();

    public static boolean isShiftDown() {
        return isShiftDown;
    }

    public static boolean isControlDown() {
        return isControlDown;
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
     * Sets value of the {@link #dragCursorProperty()}.
     *
     * @param cursor the cursor to be used by the plugin
     */
    public final void setDragCursor(final Cursor cursor) {
        dragCursorProperty().set(cursor);
    }

    /**
     * Returns the value of the {@link #dragCursorProperty()}
     *
     * @return the current cursor
     */
    public final Cursor getDragCursor() {
        return dragCursorProperty().get();
    }

    /**
     * @return whether points can be edited
     */
    public final ReadOnlyBooleanProperty editEnableProperty() {
        return editEnable;
    }

    /**
     * Returns the value of the {@link #editEnableProperty()}
     * @return true: can edit point
     */
    public final boolean isEditable() {
        return editEnableProperty().get();
    }

    private void installCursor() {
        final Region chart = getChart();
        originalCursor = chart.getCursor();
        if (getDragCursor() != null) {
            chart.setCursor(getDragCursor());
        }
    }

    private void uninstallCursor() {
        getChart().setCursor(originalCursor);
    }

    private final DoubleProperty pickingDistance = new SimpleDoubleProperty(this, "pickingDistance",
            DEFAULT_PICKING_DISTANCE) {

        @Override
        public void set(final double newValue) {
            super.set(Math.max(1, newValue));
        }
    };

    /**
     * Distance of the mouse cursor from the data point (expressed in display units) that should trigger showing the
     * tool tip. By default initialised to {@value #DEFAULT_PICKING_DISTANCE}.
     *
     * @return the picking distance property
     */
    public final DoubleProperty pickingDistanceProperty() {
        return pickingDistance;
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
     * Sets the value of {@link #pickingDistanceProperty()}.
     *
     * @param distance the new picking distance
     */
    public final void setPickingDistance(final double distance) {
        pickingDistanceProperty().set(distance);
    }

    private final EventHandler<MouseEvent> selectionStartHandler = event -> {
        if (getSelectionMouseFilter() == null || getSelectionMouseFilter().test(event)) {
            selectionStarted(event);
            event.consume();
        }

        if (event.isSecondaryButtonDown()) {
            // System.out.println("right mouse click on Marker2");
            if (event.getSource() != null && (event.getSource() instanceof SelectedDataPoint)) {
                popup.showPopup(event, (SelectedDataPoint) event.getSource());
                event.consume();
            } else {
                popup.showPopup(event, null);
                event.consume();
                return;
            }
            //
            // if (event.getSource() == null || (event.getSource() instanceof
            // SelectedDataPoint)) {
            // popup.showPopup(null);
            // event.consume();
            // }

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

    private final EventHandler<KeyEvent> keyPressedHandler = keyEvent -> {

        if (keyEvent.getCode() == KeyCode.CONTROL) {
            installCursor();
            isControlDown = true;
            isPointDragActive = false;
            keyEvent.consume();
        }

        if (keyEvent.getCode() == KeyCode.SHIFT) {
            isShiftDown = true;
        }

        if (keyEvent.getCode() == KeyCode.ESCAPE && (!isShiftDown || !isControlDown)) {
            this.markerPane.getChildren().clear();
        }
    };

    private final EventHandler<KeyEvent> keyReleasedHandler = keyEvent -> {

        if (keyEvent.getCode() == KeyCode.CONTROL) {
            uninstallCursor();
            isControlDown = false;
            // keyEvent.consume();
        }

        if (keyEvent.getCode() == KeyCode.SHIFT) {
            isShiftDown = false;
        }
    };

    public EditDataSet() {
        super();
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

    @Override
    protected void datasetsChanged(final ListChangeListener.Change<? extends DataSet> change) {
        super.datasetsChanged(change);

        this.updateMarker();
    }

    private void registerMouseHandlers() {
        registerInputEventHandler(MouseEvent.MOUSE_PRESSED, selectionStartHandler);
        registerInputEventHandler(MouseEvent.MOUSE_DRAGGED, selectionDragHandler);
        registerInputEventHandler(MouseEvent.MOUSE_RELEASED, selectionEndHandler);

    }

    private void registerKeyHandlers() {
        registerInputEventHandler(KeyEvent.KEY_PRESSED, keyPressedHandler);
        registerInputEventHandler(KeyEvent.KEY_RELEASED, keyReleasedHandler);
    }

    @Override
    public HBox getInteractorBar() {
        HBox interactorBar = super.getInteractorBar();

        final Glyph editGlyph = new Glyph(FONT_AWESOME, FontAwesome.Glyph.EDIT).size(FONT_SIZE);
        final Glyph addGlyph = new Glyph(FONT_AWESOME, FontAwesome.Glyph.PLUS_CIRCLE).size(FONT_SIZE);
        final Glyph removeGlyph = new Glyph(FONT_AWESOME, FontAwesome.Glyph.MINUS_CIRCLE).size(FONT_SIZE);
        final Glyph shiftXYGlyph = new Glyph(FONT_AWESOME, FontAwesome.Glyph.ARROWS).size(FONT_SIZE_COMBO);
        final Glyph shiftXGlyph = new Glyph(FONT_AWESOME, FontAwesome.Glyph.ARROWS_H).size(FONT_SIZE_COMBO);
        final Glyph shiftYGlyph = new Glyph(FONT_AWESOME, FontAwesome.Glyph.ARROWS_V).size(FONT_SIZE_COMBO);

        final Button editButton = new Button(null, editGlyph);
        editButton.setPadding(new Insets(3, 3, 3, 3));
        editButton.setTooltip(new Tooltip("enables edit interactor"));
        interactorBar.getChildren().add(editButton);
        // editButton.setOnAction(evt -> {
        // editEnable.set(!editEnable.get());
        // });

        final Button addButton = new Button(null, addGlyph);
        addGlyph.setTextFill(Color.DARKGREEN);
        addButton.setPadding(new Insets(3, 3, 3, 3));
        addButton.setTooltip(new Tooltip("add data point"));
        interactorBar.getChildren().add(addButton);

        final Button removeButton = new Button(null, removeGlyph);
        removeGlyph.setTextFill(Color.DARKRED);
        removeButton.setPadding(new Insets(3, 3, 3, 3));
        removeButton.setTooltip(new Tooltip("remove data point"));
        interactorBar.getChildren().add(removeButton);

        shiftXYGlyph.setTextFill(Color.DARKBLUE);
        shiftXYGlyph.setPadding(Insets.EMPTY);
        shiftXGlyph.setTextFill(Color.DARKBLUE);
        shiftXGlyph.setPadding(Insets.EMPTY);
        shiftYGlyph.setTextFill(Color.DARKBLUE);
        shiftYGlyph.setPadding(Insets.EMPTY);

        ComboBox<Glyph> comBox = new ComboBox<>();
        comBox.setPrefSize(-1, -1);
        comBox.setPadding(Insets.EMPTY);
        comBox.setBorder(null);
        comBox.getItems().addAll(shiftXYGlyph, shiftXGlyph, shiftYGlyph);
        comBox.getSelectionModel().select(0);
        comBox.getSelectionModel().selectedIndexProperty().addListener((ch, o, n) -> {
            if (n == null) {
                return;
            }
            switch (n.intValue()) {
            case 1: // allow shifts in X
                allowShiftX.set(true);
                allowShiftY.set(false);
                break;
            case 2:// allow shifts in Y
                allowShiftX.set(false);
                allowShiftY.set(true);
                break;
            default:
            case 0:
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

    private void selectionStarted(final MouseEvent event) {
        selectStartPoint = new Point2D(event.getX(), event.getY());

        selectRectangle.setX(selectStartPoint.getX());
        selectRectangle.setY(selectStartPoint.getY());
        selectRectangle.setWidth(0);
        selectRectangle.setHeight(0);
        selectRectangle.setVisible(true);
        // installCursor();
    }

    private boolean selectionOngoing() {
        return selectStartPoint != null;
    }

    private void selectionDragged(final MouseEvent event) {
        final Bounds plotAreaBounds = getChart().getPlotArea().getBoundsInLocal();
        selectEndPoint = limitToPlotArea(event, plotAreaBounds);

        double zoomRectX = plotAreaBounds.getMinX();
        double zoomRectY = plotAreaBounds.getMinY();
        double zoomRectWidth = plotAreaBounds.getWidth();
        double zoomRectHeight = plotAreaBounds.getHeight();

        zoomRectX = Math.min(selectStartPoint.getX(), selectEndPoint.getX());
        zoomRectWidth = Math.abs(selectEndPoint.getX() - selectStartPoint.getX());
        zoomRectY = Math.min(selectStartPoint.getY(), selectEndPoint.getY());
        zoomRectHeight = Math.abs(selectEndPoint.getY() - selectStartPoint.getY());

        selectRectangle.setX(zoomRectX);
        selectRectangle.setY(zoomRectY);
        selectRectangle.setWidth(zoomRectWidth);
        selectRectangle.setHeight(zoomRectHeight);
    }

    /**
     * limits the mouse event position to the min/max range of the canavs (N.B. event can occur to be
     * negative/larger/outside than the canvas) This is to avoid zooming outside the visible canvas range
     *
     * @param event the mouse event
     * @param plotBounds of the canvas
     * @return the clipped mouse location
     */
    private Point2D limitToPlotArea(final MouseEvent event, final Bounds plotBounds) {
        final double limitedX = Math.max(Math.min(event.getX() - plotBounds.getMinX(), plotBounds.getMaxX()),
                plotBounds.getMinX());
        final double limitedY = Math.max(Math.min(event.getY() - plotBounds.getMinY(), plotBounds.getMaxY()),
                plotBounds.getMinY());
        return new Point2D(limitedX, limitedY);
    }

    private void selectionEnded() {
        selectRectangle.setVisible(false);
        if (selectRectangle.getWidth() > SELECT_RECT_MIN_SIZE && selectRectangle.getHeight() > SELECT_RECT_MIN_SIZE) {
            performSelection();
        }
        selectStartPoint = selectEndPoint = null;
        // uninstallCursor();
    }

    private void performSelection() {
        if (!(getChart() instanceof XYChart)) {
            return;
        }
        final XYChart xyChart = (XYChart) getChart();

        if (!isShiftDown()) {
            markedPoints.clear();
        }

        findDataPoint(xyChart.getFirstAxis(Orientation.HORIZONTAL), xyChart.getFirstAxis(Orientation.VERTICAL),
                xyChart.getDatasets());

        for (Renderer rend : xyChart.getRenderers()) {
            ObservableList<Axis> axes = rend.getAxes();
            findDataPoint(getFirstAxis(axes, Orientation.HORIZONTAL), getFirstAxis(axes, Orientation.VERTICAL),
                    rend.getDatasets());
        }

        if (markedPoints.isEmpty()) {
            editEnable.set(false);
        } else {
            editEnable.set(true);
        }

        updateMarker();
    }

    private Axis getFirstAxis(final List<Axis> axes, final Orientation orientation) {
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

    private void updateMarker() {
        this.markerPane.getChildren().clear();

        markerPane.getParent().setMouseTransparent(false);

        for (EditableDataSet dataSet : markedPoints.keySet()) {
            ConcurrentHashMap<Integer, SelectedDataPoint> dataPoints = markedPoints.get(dataSet);
            for (Integer dataPointIndex : dataPoints.keySet()) {
                SelectedDataPoint dataPoint = dataPoints.get(dataPointIndex);

                dataPoint.setOnMouseClicked(evt -> {
                    if (evt.isSecondaryButtonDown()) {
                        System.err.println("right clicked on circle");
                    }
                });

                dataPoint.update();
                markerPane.getChildren().add(dataPoint);

            }
        }
        if (markerPane.getChildren().isEmpty()) {
            markerPane.getParent().setMouseTransparent(true);
        }
    }

    private void findDataPoint(final Axis xAxis, final Axis yAxis, final List<DataSet> dataSets) {
        final double xMinScreen = Math.min(selectStartPoint.getX(), selectEndPoint.getX());
        final double xMaxScreen = Math.max(selectStartPoint.getX(), selectEndPoint.getX());
        final double yMinScreen = Math.min(selectStartPoint.getY(), selectEndPoint.getY());
        final double yMaxScreen = Math.max(selectStartPoint.getY(), selectEndPoint.getY());

        for (DataSet ds : dataSets) {
            if (!(ds instanceof EditableDataSet)) {
                continue;
            }
            EditableDataSet dataSet = (EditableDataSet) ds;

            ConcurrentHashMap<Integer, SelectedDataPoint> dataSetHashMap = markedPoints.computeIfAbsent(dataSet,
                    k -> new ConcurrentHashMap<Integer, SelectedDataPoint>());

            final int indexMin = Math.max(0, dataSet.getXIndex(xAxis.getValueForDisplay(xMinScreen)));
            final int indexMax = Math.min(dataSet.getXIndex(xAxis.getValueForDisplay(xMaxScreen)) + 1,
                    dataSet.getDataCount());

            // N.B. (0,0) screen coordinate is in the top left corner vs. normal
            // 0,0 in the bottom left -> need to invert limits
            final double yMax = yAxis.getValueForDisplay(yMinScreen);
            final double yMin = yAxis.getValueForDisplay(yMaxScreen);

            for (int i = indexMin; i < indexMax; i++) {
                final double y = dataSet.getY(i);
                if ((y >= yMin) && (y <= yMax)) {
                    if (!isShiftDown()) {
                        dataSetHashMap.put(i, new SelectedDataPoint(xAxis, yAxis, dataSet, i));
                    } else {
                        // add if not existing/remove if existing
                        if (dataSetHashMap.get(i) != null) {
                            dataSetHashMap.remove(i);
                        } else {
                            dataSetHashMap.put(i, new SelectedDataPoint(xAxis, yAxis, dataSet, i));
                        }
                    }

                }
            }
        }

    }

    private boolean isMouseEventWithinCanvas(final MouseEvent mouseEvent) {
        final Canvas canvas = getChart().getCanvas();
        // listen to only events within the canvas
        final Point2D mouseLoc = new Point2D(mouseEvent.getScreenX(), mouseEvent.getScreenY());
        final Bounds screenBounds = canvas.localToScreen(canvas.getBoundsInLocal());
        return screenBounds.contains(mouseLoc);
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

    /**
     * Sets filter on {@link MouseEvent#DRAG_DETECTED DRAG_DETECTED} events that should start zoom-in operation.
     *
     * @param zoomInMouseFilter the filter to accept zoom-in mouse event. If {@code null} then any DRAG_DETECTED event
     *            will start zoom-in operation. By default it's set to {@link #defaultSelectFilter}.
     * @see #getSelectionMouseFilter()
     */
    public void setZoomInMouseFilter(final Predicate<MouseEvent> zoomInMouseFilter) {
        this.zoomInMouseFilter = zoomInMouseFilter;
    }

    private double mouseOriginX = -1;
    private double mouseOriginY = -1;

    /**
     * Creates an event handler that handles a mouse press on the node.
     * @param dataPoint corresponding to clicked data point
     * 
     * @return the event handler.
     */
    protected EventHandler<MouseEvent> startDragHandler(SelectedDataPoint dataPoint) {
        return event -> {
            if (event.isPrimaryButtonDown() && !isControlDown && !isShiftDown) {
                System.err.println("start drag");
                // start drag
                isPointDragActive = true;

                // get the current mouse coordinates according to the scene.
                mouseOriginX = event.getSceneX();
                mouseOriginY = event.getSceneY();

                dataPoint.setCursor(Cursor.CLOSED_HAND);
                event.consume();
            }

            if (event.isSecondaryButtonDown()) {
                System.out.println("right mouse click on Marker");
                popup.showPopup(event, dataPoint);
            }
        };
    }

    /**
     * Creates an event handler that handles a mouse drag on the node.
     */
    protected EventHandler<MouseEvent> dragHandler = this::performPointDrag;

    protected void performPointDrag(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && !isControlDown && isPointDragActive) {
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

    protected void applyDrag(final double deltaX, final double deltaY) {
        for (Node node : markerPane.getChildren()) {
            if (!(node instanceof SelectedDataPoint)) {
                continue;
            }
            SelectedDataPoint marker = (SelectedDataPoint) node;
            marker.applyDrag(deltaX, deltaY);
        }
        this.updateMarker();
    }

    private static final PseudoClass NOEDIT_PSEUDO_CLASS = PseudoClass.getPseudoClass("noEdit");

    protected class SelectedDataPoint extends Circle {

        private final Axis xAxis;
        private final Axis yAxis;
        private final EditableDataSet dataSet;
        private double xValue;
        private double yValue;

        SelectedDataPoint(final Axis xAxis, final Axis yAxis, final EditableDataSet dataSet, final int index) {
            super();
            getStyleClass().add(STYLE_CLASS_SELECT_PATH);
            // this.setPickOnBounds(true);
            // setManaged(false);

            EditConstraints constraints = dataSet.getEditConstraints();
            if (constraints != null) {
                boolean canChange = constraints.canChange(index);
                if (!canChange) {
                    pseudoClassStateChanged(NOEDIT_PSEUDO_CLASS, true);
                }
            } else {
                pseudoClassStateChanged(NOEDIT_PSEUDO_CLASS, false);
            }

            this.xAxis = xAxis;
            this.yAxis = yAxis;
            this.dataSet = dataSet;
            this.xValue = dataSet.getX(index);
            this.yValue = dataSet.getY(index);
            this.setCenterX(getX()); // NOPMD by rstein on 13/06/19 14:14
            this.setCenterY(getY()); // NOPMD by rstein on 13/06/19 14:14
            this.setRadius(DEFAULT_MARKER_RADIUS);

            final EventHandler<? super InputEvent> dragOver = e -> {
                System.err.println("drag is over1");
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

            xAxis.addListener(evt -> this.setCenterX(getX()));
            yAxis.addListener(evt -> this.setCenterY(getY()));
            dataSet.addListener(e -> FXUtils.runFX(this::update));
        }

        public EditableDataSet getDataSet() {
            return dataSet;
        }

        public int getIndex() {
            for (int i = 0; i < dataSet.getDataCount(); i++) {
                final double x0 = dataSet.getX(i);
                final double y0 = dataSet.getY(i);
                if (x0 == xValue && y0 == yValue) {
                    return i;
                }
            }
            return -1;
        }

        public double getX() {
            return xAxis.getDisplayPosition(xValue);
        }

        public double getY() {
            return yAxis.getDisplayPosition(yValue);
        }

        public void update() {
            setCenterX(getX());
            setCenterY(getY());
        }

        public boolean delete() {
            EditConstraints constraints = dataSet.getEditConstraints();
            if (constraints == null) {
                dataSet.remove(getIndex());
                return true;
            }

            if (constraints.canDelete(getIndex())) {
                dataSet.remove(getIndex());
                return true;
            }

            return false;
        }

        public void applyDrag(final double deltaX, final double deltaY) {
            final double oX = getX();
            final double oY = getY();
            double nX = oX;
            double nY = oY;

            EditConstraints constraints = dataSet.getEditConstraints();
            if (constraints == null) {
                if (allowShiftX.get()) {
                    nX += deltaX;
                }

                if (allowShiftY.get()) {
                    nY += deltaY;
                }

                double x = xAxis.getValueForDisplay(nX);
                double y = yAxis.getValueForDisplay(nY);
                dataSet.set(getIndex(), x, y);
                xValue = x;
                yValue = y;
                return;
            }
            boolean canChange = constraints.canChange(getIndex());

            if (canChange && constraints.isXEditable() && allowShiftX.get()) {
                nX += deltaX;
            }

            if (canChange && constraints.isYEditable() && allowShiftY.get()) {
                nY += deltaY;
            }

            double x = xAxis.getValueForDisplay(nX);
            double y = yAxis.getValueForDisplay(nY);
            dataSet.set(getIndex(), x, y);
            xValue = x;
            yValue = y;

            // update();
        }

        @Override
        public String toString() {
            return "selected index=" + getIndex();
        }
    }

    private void addPoint(final double x, final double y) {
        if (!(getChart() instanceof XYChart)) {
            return;
        }
        final XYChart xyChart = (XYChart) getChart();

        // TODO: tidy up code and make it compatible with multiple renderer

        // find data set closes to screen coordinate x & y
        Pane pane = getChart().getCanvasForeground();
        Bounds bounds = pane.getBoundsInLocal();
        Bounds screenBounds = pane.localToScreen(bounds);
        int x0 = (int) screenBounds.getMinX();
        int y0 = (int) screenBounds.getMinY();
        DataPoint dataPoint = findNearestDataPoint(getChart(), new Point2D(x - x0, y - y0));

        if (dataPoint != null && (dataPoint.getDataSet() instanceof EditableDataSet)) {
            final Axis xAxis = xyChart.getFirstAxis(Orientation.HORIZONTAL);
            final Axis yAxis = xyChart.getFirstAxis(Orientation.VERTICAL);
            int index = dataPoint.getIndex();
            final double newValX = xAxis.getValueForDisplay(x - x0);
            final double newValY = yAxis.getValueForDisplay(y - y0);
            EditableDataSet ds = (EditableDataSet) (dataPoint.getDataSet());
            final double oldValX = ds.getX(index);
            if (oldValX <= newValX) {
                ds.add(index, newValX, newValY);
            } else {
                ds.add(index - 1, newValX, newValY);
            }
        }

        updateMarker();
    }

    private DataPoint findNearestDataPoint(final Chart chart, final Point2D mouseLocation) {
        DataPoint nearestDataPoint = null;
        if (!(chart instanceof XYChart)) {
            return null;
        }
        final XYChart xyChart = (XYChart) chart;
        // TODO: iterate through all axes, renderer and datasets
        final double xValue = xyChart.getXAxis().getValueForDisplay(mouseLocation.getX());

        for (final DataPoint dataPoint : findNeighborPoints(xyChart, xValue)) {
            if (getChart().getFirstAxis(Orientation.HORIZONTAL) instanceof Axis) {
                final double x = xyChart.getXAxis().getDisplayPosition(dataPoint.x);
                final double y = xyChart.getYAxis().getDisplayPosition(dataPoint.y);
                final Point2D displayPoint = new Point2D(x, y);
                dataPoint.distanceFromMouse = displayPoint.distance(mouseLocation);
                if ((nearestDataPoint == null
                        || dataPoint.getDistanceFromMouse() < nearestDataPoint.getDistanceFromMouse())) {
                    nearestDataPoint = dataPoint;
                }
            }
        }
        return nearestDataPoint;
    }

    private List<DataPoint> findNeighborPoints(final XYChart chart, final double searchedX) {
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
     * Handles series that have data sorted or not sorted with respect to X coordinate.
     * @param dataSet data set
     * @param searchedX X coordinates
     * @return pair of neighbouring data points
     */
    private Pair<DataPoint, DataPoint> findNeighborPoints(final DataSet dataSet, final double searchedX) {
        int prevIndex = -1;
        int nextIndex = -1;
        double prevX = Double.MIN_VALUE;
        double nextX = Double.MAX_VALUE;

        for (int i = 0, size = dataSet.getDataCount(); i < size; i++) {
            final double currentX = dataSet.getX(i);

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
                : new DataPoint(getChart(), dataSet, prevIndex, dataSet.getX(prevIndex), dataSet.getY(prevIndex),
                        dataSet.getDataLabel(prevIndex));
        final DataPoint nextPoint = nextIndex == -1 || nextIndex == prevIndex ? null
                : new DataPoint(getChart(), dataSet, nextIndex, dataSet.getX(nextIndex), dataSet.getY(nextIndex),
                        dataSet.getDataLabel(nextIndex));

        return new Pair<>(prevPoint, nextPoint);
    }

    private void deleteAllMarkedPoints() {
        for (EditableDataSet dataSet : markedPoints.keySet()) {
            ConcurrentHashMap<Integer, SelectedDataPoint> dataPoints = markedPoints.get(dataSet);
            for (Integer dataPointIndex : dataPoints.keySet()) {
                SelectedDataPoint dataPoint = dataPoints.get(dataPointIndex);

                if (dataPoint.delete()) {
                    dataPoints.remove(dataPointIndex);
                }
            }
        }
        updateMarker();
    }

    protected class PointEditionPopup extends Popup {

        private Button addPoint = new Button("add");
        private Button deletePoint = new Button("delete");
        private Button deletePoints = new Button("delete all");

        public PointEditionPopup() {
            super();
            setAutoFix(true);
            setAutoHide(true);
            setHideOnEscape(true);
            setAutoHide(true);
            getContent().add(initContent());

            addPoint.setOnAction(evt -> {
                final double x = PointEditionPopup.this.getX();
                final double y = PointEditionPopup.this.getY();
                addPoint(x, y);
            });
            deletePoints.setOnAction(evt -> deleteAllMarkedPoints());
        }

        private VBox initContent() {
            VBox pane = new VBox();
            pane.getChildren().add(new Label("popup"));
            pane.getChildren().add(addPoint);
            pane.getChildren().add(deletePoint);
            pane.getChildren().add(deletePoints);

            return pane;
        }

        public void showPopup(final MouseEvent event, final SelectedDataPoint selectedPoint) {
            // System.err.println("show popup = " + selectedPoint);
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

    public class DataPoint {

        private final Chart chart;
        private final double x;
        private final double y;
        private final String label;
        double distanceFromMouse;
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

        public int getIndex() {
            return index;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public String getLabel() {
            return label;
        }

        public double getDistanceFromMouse() {
            return distanceFromMouse;
        }

        @Override
        public String toString() {
            return "DataSet= '" + dataSet.getName() + "' index=" + index;
        }

    }
}
