package de.gsi.chart.plugins;

import static impl.org.controlsfx.i18n.Localization.asKey;
import static impl.org.controlsfx.i18n.Localization.localize;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.controlsfx.control.spreadsheet.GridBase;
import org.controlsfx.control.spreadsheet.SpreadsheetCell;
import org.controlsfx.control.spreadsheet.SpreadsheetCellType;
import org.controlsfx.control.spreadsheet.SpreadsheetCellType.DoubleType;
import org.controlsfx.control.spreadsheet.SpreadsheetView;
import org.controlsfx.control.spreadsheet.StringConverterWithFormat;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

import de.gsi.chart.Chart;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.event.EventListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TablePosition;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.converter.DoubleStringConverter;

/**
 * Displays the all visible data sets inside a table on demand. Implements copy-paste functionality into system
 * clip-board to allow further processing in other applications. Presently the display and max clip-board export is
 * limited to 10k data rows for performance reasons.
 * 
 * @author rstein
 */
public class TableViewer extends ChartPlugin {

    private static final int MAX_ROW_EXPORT_LIMIT = 10000;
    protected static final String FONT_AWESOME = "FontAwesome";
    protected static final int FONT_SIZE = 20;
    private final Glyph tableView = new Glyph(FONT_AWESOME, "\uf0ce").size(FONT_SIZE);
    private final Glyph graphView = new Glyph(FONT_AWESOME, "\uf201").size(FONT_SIZE);
    private final Glyph saveIcon = new Glyph(FONT_AWESOME, "\uf0c7").size(FONT_SIZE);
    // not a great icon \uf02e for clip board but another one wasn't available
    // (\uf328)
    private final Glyph clipBoardIcon = new Glyph(FONT_AWESOME, FontAwesome.Glyph.CLIPBOARD).size(FONT_SIZE);
    private final ListChangeListener<Renderer> rendererChangeListener = this::rendererChanged;
    private final ListChangeListener<DataSet> datasetChangeListener = this::datasetsChanged;
    private final EventListener dataSetDataUpdateListener = obs -> FXUtils.runFX(this::refreshTable);
    private final HBox interactorButtons = getInteractorBar();
    // private Pane table = new Pane();
    private final MySpreadsheetView table = new MySpreadsheetView();

    /**
     * Creates a new instance of DataSetTableViewer class.
     */
    public TableViewer() {
        super();

        table.setStyle("-fx-background-color: -fx-focus-color, -fx-background; -fx-opacity = 0.5");
        table.setStyle("-fx-background-color: #AAAAAAD0");

        chartProperty().addListener((change, o, n) -> {
            if (o != null) {
                o.getToolBar().getChildren().remove(interactorButtons);
                o.getPlotForeground().getChildren().remove(table);
                o.getPlotArea().setBottom(null);
                table.prefWidthProperty().unbind();
                table.prefHeightProperty().unbind();

                // de-register data set listener
                o.getDatasets().removeListener(datasetChangeListener);
                o.getRenderers().removeListener(rendererChangeListener);

            }
            if (n != null) {
                if (isAddButtonsToToolBar()) {
                    n.getToolBar().getChildren().add(interactorButtons);
                }
                n.getPlotForeground().getChildren().add(table);
                table.toFront();
                table.setVisible(false);
                table.prefWidthProperty().bind(n.getPlotForeground().widthProperty());
                table.prefHeightProperty().bind(n.getPlotForeground().heightProperty());

                // register data set listener
                n.getDatasets().addListener(datasetChangeListener);
                n.getDatasets().forEach(dataSet -> dataSet.addListener(dataSetDataUpdateListener));
                n.getRenderers().addListener(rendererChangeListener);
                n.getRenderers().forEach(renderer -> renderer.getDatasets().addListener(datasetChangeListener));
            }
        });

        addButtonsToToolBarProperty().addListener((ch, o, n) -> {
            final Chart chartLocal = getChart();
            if (chartLocal == null || o.equals(n)) {
                return;
            }
            if (n) {
                chartLocal.getToolBar().getChildren().add(interactorButtons);
            } else {
                chartLocal.getToolBar().getChildren().remove(interactorButtons);
            }
        });
    }

    public HBox getInteractorBar() {
        final Separator separator = new Separator();
        separator.setOrientation(Orientation.VERTICAL);
        final HBox buttonBar = new HBox();
        buttonBar.setPadding(new Insets(1, 1, 1, 1));
        final Button switchTableView = new Button(null, tableView);
        switchTableView.setPadding(new Insets(3, 3, 3, 3));
        switchTableView.setTooltip(new Tooltip("switches between graph and table view"));

        final Button copyToClipBoard = new Button(null, clipBoardIcon);
        copyToClipBoard.setPadding(new Insets(3, 3, 3, 3));
        copyToClipBoard.setTooltip(new Tooltip("copy actively shown content top system clipboard"));
        copyToClipBoard.setOnAction(e -> table.copyAllToClipboard());

        final Button saveTableView = new Button(null, saveIcon);
        saveTableView.setPadding(new Insets(3, 3, 3, 3));
        saveTableView.setTooltip(new Tooltip("store actively shown content as .csv file"));
        saveTableView.setOnAction(e -> this.exportGridToCSV());

        switchTableView.setOnAction(evt -> {
            switchTableView.setGraphic(table.isVisible() ? tableView : graphView);
            table.setVisible(!table.isVisible());
            table.setMouseTransparent(!table.isVisible());
            getChart().getPlotForeground().setMouseTransparent(!table.isVisible());
            table.setZoomFactor(1.0);
            refreshTable();
        });

        buttonBar.getChildren().addAll(separator, switchTableView, copyToClipBoard, saveTableView);
        return buttonBar;
    }

    protected void datasetsChanged(final ListChangeListener.Change<? extends DataSet> change) {
        boolean dataSetChanges = false;

        final List<DataSet> newDataSets = new ArrayList<>();
        final List<DataSet> oldDataSets = new ArrayList<>();

        while (change.next()) {
            oldDataSets.addAll(change.getRemoved());
            for (final DataSet set : change.getRemoved()) {
                set.removeListener(dataSetDataUpdateListener);
                dataSetChanges = true;
            }

            newDataSets.addAll(change.getAddedSubList());
            for (final DataSet set : change.getAddedSubList()) {
                set.addListener(dataSetDataUpdateListener);
                dataSetChanges = true;
            }
        }

        if (dataSetChanges) {
            this.refreshTable();
        }
    }

    protected void rendererChanged(final ListChangeListener.Change<? extends Renderer> change) {
        boolean dataSetChanges = false;
        while (change.next()) {
            // handle added renderer
            change.getAddedSubList().forEach(renderer -> renderer.getDatasets().addListener(datasetChangeListener));
            if (!change.getAddedSubList().isEmpty()) {
                dataSetChanges = true;
            }

            // handle removed renderer
            change.getRemoved().forEach(renderer -> renderer.getDatasets().removeListener(datasetChangeListener));
            if (!change.getRemoved().isEmpty()) {
                dataSetChanges = true;
            }
        }

        if (dataSetChanges) {
            this.refreshTable();
        }
    }

    public SpreadsheetView getTable() {
        return table;
    }

    private void refreshTable() {
        if (getChart() == null || !table.isVisible()) {
            return;
        }
        repopulateTable();
    }

    private void repopulateTable() {
        ObservableList<DataSet> dataSets = getChart().getAllDatasets();

        int nRowCount = 0;
        int nColumnCount = 0;
        for (DataSet ds : dataSets) {
            nRowCount = Math.min(Math.max(nRowCount, ds.getDataCount()), MAX_ROW_EXPORT_LIMIT);
            nColumnCount += 2;

            if (ds instanceof DataSetError) {
                DataSetError eDs = (DataSetError) ds;

                switch (eDs.getErrorType()) {
                case NO_ERROR:
                    break;
                case X:
                case X_ASYMMETRIC:
                case Y:
                case Y_ASYMMETRIC:
                    nColumnCount += 2;
                    break;
                case XY:
                case XY_ASYMMETRIC:
                default:
                    nColumnCount += 2;
                    break;
                }
            }
        }

        table.setGrid(new MyGrid(nRowCount, nColumnCount, dataSets));
    }

    public List<String> getGridStringRepresentation() {
        ArrayList<String> stringRows = new ArrayList<>();
        ObservableList<ObservableList<SpreadsheetCell>> rows = table.getGrid().getRows();
        int countRow = 0;
        for (ObservableList<SpreadsheetCell> row : rows) {
            StringBuilder sb = new StringBuilder();
            if (countRow < 2) {
                sb.append("# ");
            }
            for (SpreadsheetCell cell : row) {
                sb.append(cell.getText());
                sb.append(',');
            }
            sb.append('\n');
            stringRows.add(sb.toString());
            countRow++;
        }
        return stringRows;
    }

    public void exportGridToCSV() {
        if (!table.isVisible()) {
            repopulateTable();
        }
        FileChooser chooser = new FileChooser();
        File save = chooser.showSaveDialog(getChart().getScene().getWindow());
        if (save == null) {
            return;
        }
        List<String> stringRows = getGridStringRepresentation();
        
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(save.getPath() + ".csv"), StandardCharsets.UTF_8)) {
            for (String str : stringRows) {
                writer.write(str);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private class MyGrid extends GridBase {

        public MyGrid(final int rowCount, final int columnCount, final List<DataSet> dataSets) {
            super(rowCount, columnCount);

            initRowData(dataSets);
        }

        private void initRowData(List<DataSet> dataSets) {
            final ObservableList<ObservableList<SpreadsheetCell>> rows = FXCollections.observableArrayList();

            final ObservableList<SpreadsheetCell> dataSetNames = FXCollections.observableArrayList();
            for (int c = 0; c < getColumnCount(); c++) {
                dataSetNames.add(SpreadsheetCellType.STRING.createCell(0, c, 1, 1, ""));
            }
            rows.add(dataSetNames);

            // DoubleType doubleCellConverter = SpreadsheetCellType.DOUBLE;
            DoubleType doubleCellConverter = new DoubleType(
                    new StringConverterWithFormat<Double>(new DoubleStringConverter()) {

                        @Override
                        public String toString(Double item) {
                            return toStringFormat(item, "");
                        }

                        @Override
                        public Double fromString(String str) {
                            if (str == null || str.isEmpty() || "NaN".equals(str)) {
                                return Double.NaN;
                            } else {
                                return myConverter.fromString(str);
                            }
                        }

                        @Override
                        public String toStringFormat(Double item, String format) {
                            try {
                                if (item == null || Double.isNaN(item)) {
                                    return "";
                                } else {
                                    return String.format("%f", item);
                                }
                            } catch (Exception ex) {
                                return myConverter.toString(item);
                            }
                        }
                    });

            final ObservableList<SpreadsheetCell> dataSetVariables = FXCollections.observableArrayList();
            rows.add(dataSetVariables);

            int columnCountDataSetName = 0;
            for (int row = 0; row < this.getRowCount(); row++) {
                int column = 0;
                final ObservableList<SpreadsheetCell> rowData = FXCollections.observableArrayList();
                rows.add(rowData);
                for (DataSet ds : dataSets) {
                    int columnCount = 2;

                    Double valueX = row < ds.getDataCount() ? ds.getX(row) : Double.NaN;
                    Double valueY = row < ds.getDataCount() ? ds.getY(row) : Double.NaN;
                    rowData.add(doubleCellConverter.createCell(row, column++, 1, 1, valueX));
                    rowData.add(doubleCellConverter.createCell(row, column++, 1, 1, valueY));
                    if (row == 0) {
                        dataSetVariables.add(SpreadsheetCellType.STRING.createCell(row, column - 2, 1, 1, "X"));
                        dataSetVariables.add(SpreadsheetCellType.STRING.createCell(row, column - 1, 1, 1, "Y"));
                    }

                    if (ds instanceof DataSetError) {
                        DataSetError eDs = (DataSetError) ds;
                        Double errorXN = row < ds.getDataCount() ? eDs.getXErrorNegative(row) : Double.NaN;
                        Double errorXP = row < ds.getDataCount() ? eDs.getXErrorPositive(row) : Double.NaN;
                        Double errorYN = row < ds.getDataCount() ? eDs.getYErrorNegative(row) : Double.NaN;
                        Double errorYP = row < ds.getDataCount() ? eDs.getYErrorPositive(row) : Double.NaN;

                        switch (eDs.getErrorType()) {
                        case NO_ERROR:
                            break;
                        case X:
                        case X_ASYMMETRIC:
                            rowData.add(doubleCellConverter.createCell(row, column++, 1, 1, errorXN));
                            rowData.add(doubleCellConverter.createCell(row, column++, 1, 1, errorXP));
                            if (row == 0) {
                                dataSetVariables
                                        .add(SpreadsheetCellType.STRING.createCell(row, column - 2, 1, 1, "+ex"));
                                dataSetVariables
                                        .add(SpreadsheetCellType.STRING.createCell(row, column - 1, 1, 1, "-ex"));
                            }
                            columnCount = 4;
                            break;
                        case Y:
                        case Y_ASYMMETRIC:
                            rowData.add(doubleCellConverter.createCell(row, column++, 1, 1, errorYN));
                            rowData.add(doubleCellConverter.createCell(row, column++, 1, 1, errorYP));
                            if (row == 0) {
                                dataSetVariables
                                        .add(SpreadsheetCellType.STRING.createCell(row, column - 2, 1, 1, "+ey"));
                                dataSetVariables
                                        .add(SpreadsheetCellType.STRING.createCell(row, column - 1, 1, 1, "-ey"));
                            }
                            columnCount = 4;
                            break;
                        case XY:
                        case XY_ASYMMETRIC:
                        default:
                            rowData.add(doubleCellConverter.createCell(row, column++, 1, 1, errorXN));
                            rowData.add(doubleCellConverter.createCell(row, column++, 1, 1, errorXP));
                            rowData.add(doubleCellConverter.createCell(row, column++, 1, 1, errorYN));
                            rowData.add(doubleCellConverter.createCell(row, column++, 1, 1, errorYP));
                            if (row == 0) {
                                dataSetVariables
                                        .add(SpreadsheetCellType.STRING.createCell(row, column - 4, 1, 1, "+ex"));
                                dataSetVariables
                                        .add(SpreadsheetCellType.STRING.createCell(row, column - 3, 1, 1, "-ex"));
                                dataSetVariables
                                        .add(SpreadsheetCellType.STRING.createCell(row, column - 2, 1, 1, "+ey"));
                                dataSetVariables
                                        .add(SpreadsheetCellType.STRING.createCell(row, column - 1, 1, 1, "-ey"));
                            }
                            columnCount = 6;
                            break;
                        }
                    }

                    if (row == 0) {
                        dataSetNames.set(columnCountDataSetName, SpreadsheetCellType.STRING.createCell(0,
                                columnCountDataSetName, 1, columnCount, ds.getName()));
                        columnCountDataSetName++;
                    }
                }
            }

            this.setRows(rows);
        }

        @Override
        public ObservableList<String> getRowHeaders() {
            final ObservableList<String> rowHeaders = FXCollections.observableArrayList();
            for (int i = 0; i < getRowCount(); i++) {
                rowHeaders.add(String.valueOf(i));
            }
            return rowHeaders;
        }

        @Override
        public ObservableList<String> getColumnHeaders() {
            final ObservableList<String> columnHeaders = FXCollections.observableArrayList();
            for (int i = 0; i < getRowCount(); i++) {
                columnHeaders.add(String.valueOf(i));
            }
            return columnHeaders;
        }

    }

    private class MySpreadsheetView extends SpreadsheetView {

        MySpreadsheetView() {
            super();
        }

        public void copyAllToClipboard() {
            if (!table.isVisible()) {
                repopulateTable();
            }
            List<String> rows = TableViewer.this.getGridStringRepresentation();
            StringBuilder sb = new StringBuilder();
            rows.forEach(s -> sb.append(s));

            final ClipboardContent content = new ClipboardContent();
            content.putString(sb.toString());
            Clipboard.getSystemClipboard().setContent(content);
        }

        /**
         * Put the current selection into the ClipBoard. This can be overridden by developers for custom behavior.
         */
        @Override
        public void copyClipboard() {
            super.copyClipboard();

            @SuppressWarnings("rawtypes")
            final ObservableList<TablePosition> posList = getSelectionModel().getSelectedCells();

            int minRow = Integer.MAX_VALUE;
            int maxRow = -1;
            int minCol = Integer.MAX_VALUE;
            int maxCol = -1;
            for (final TablePosition<?, ?> p : posList) {
                final int row = p.getRow();
                final int col = p.getColumn();
                minRow = Math.min(minRow, row);
                maxRow = Math.max(maxRow, row);
                minCol = Math.min(minCol, col);
                maxCol = Math.max(maxCol, col);
            }

            if (maxCol < 0 || maxRow < 0 || minCol == Integer.MAX_VALUE || minRow == Integer.MAX_VALUE) {
                // unsuported and/or no selection export whole table
                this.copyAllToClipboard();
                return;
            }

            // specific case of few selected fields
            StringBuilder sb = new StringBuilder();
            final int nRows = maxRow - minRow;
            final int nCols = maxCol - minCol;
            for (int row = minRow; row <= maxRow; row++) {
                for (int col = minCol; col <= maxCol; col++) {
                    SpreadsheetCell cell = getGrid().getRows().get(getModelRow(row)).get(getModelColumn(col));
                    String cellString = cell.getItem().toString();
                    sb.append(cellString);

                    if (nCols > 1) {
                        sb.append(',');
                        // N.B. add trailing comma only if there is more than
                        // one field in the line
                    }
                }
                if (nRows > 1) {
                    sb.append('\n');
                }
            }
            final ClipboardContent content = new ClipboardContent();
            content.putString(sb.toString());
            Clipboard.getSystemClipboard().setContent(content);
        }

        /**
         * Create a menu on rightClick with two options: Copy/Paste This can be overridden by developers for custom
         * behavior.
         * 
         * @return the ContextMenu to use.
         */
        @Override
        public ContextMenu getSpreadsheetViewContextMenu() {
            final ContextMenu contextMenu = super.getSpreadsheetViewContextMenu();

            final MenuItem copyAllItem = new MenuItem(localize(asKey("spreadsheet.view.menu.copy")) + " all");
            copyAllItem.setGraphic(
                    new ImageView(new Image(SpreadsheetView.class.getResourceAsStream("copySpreadsheetView.png"))));
            copyAllItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN));
            copyAllItem.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent e) {
                    copyAllToClipboard();
                }
            });

            contextMenu.getItems().add(0, copyAllItem);

            return contextMenu;
        }

    }

}
