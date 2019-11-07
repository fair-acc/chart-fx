package de.gsi.chart.viewer;

import java.util.ArrayList;
import java.util.List;

import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * DataViewTilingPane that to mimics HBox-, VBox-, or TilePane layout while consistently maximising it's children and
 * following layout constraints from its parent
 *
 * @author rstein
 */
public class DataViewTilingPane extends GridPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataViewTilingPane.class);
    protected static final String FONT_AWESOME = "FontAwesome";
    protected static final int FONT_SIZE = 20;
    public final Layout layout;

    public DataViewTilingPane(final Layout layout) {
        super();
        this.layout = layout;
        VBox.setVgrow(this, Priority.ALWAYS);
        getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                layoutNormal();
            }
        });
    }

    protected int getColumnsCount() {
        final int childCount = getChildren().size();
        if (childCount == 0) {
            return 1;
        }
        switch (layout) {
        case HBOX:
            return childCount;
        case MAXIMISE:
        case VBOX:
            return 1;
        case GRID:
        default:
            if (childCount < 4) {
                return 2;
            }
            int ncols = (int) Math.ceil(Math.sqrt(childCount));
            if (ncols == 0) {
                ncols = 1;
            }
            return ncols;
        }
    }

    public Layout getLayout() {
        return layout;
    }

    protected void layoutNormal() {
        if (getChildren().isEmpty()) {
            return;
        }
        final int colsCount = getColumnsCount();

        if (getColumnConstraints().size() != colsCount) {
            final List<ColumnConstraints> colConstraintList = new ArrayList<>();
            for (int i = 0; i < colsCount; i++) {
                final ColumnConstraints colConstraints = new ColumnConstraints(); // NOPMD
                colConstraints.setPercentWidth(100.0 / colsCount);
                colConstraints.setHgrow(Priority.ALWAYS); // allow column to grow
                colConstraints.setFillWidth(true);
                colConstraintList.add(colConstraints);
            }
            getColumnConstraints().setAll(colConstraintList);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("update column constraints");
            }
        }

        int rowIndex = 0;
        int colIndex = 0;
        int childCount = 0;
        final int nChildren = getChildren().size();
        int nColSpan = Math.max(1, colsCount / (nChildren - childCount));
        for (final Node child : getChildren()) {
            GridPane.setFillWidth(child, true);
            GridPane.setFillHeight(child, true);
            GridPane.setHgrow(child, Priority.ALWAYS);
            GridPane.setVgrow(child, Priority.ALWAYS);
            GridPane.setColumnIndex(child, colIndex);
            GridPane.setRowIndex(child, rowIndex);

            if ((colIndex == 0) && ((nChildren - childCount) < colsCount)) {
                nColSpan = Math.max(1, colsCount / (nChildren - childCount));
            }
            // last window fills up row
            if (((nChildren - childCount) == 1) && (colIndex < colsCount)) {
                nColSpan = colsCount - colIndex;
            }

            GridPane.setColumnSpan(child, nColSpan);

            colIndex += nColSpan;
            if (colIndex >= colsCount) {
                colIndex = 0;
                rowIndex++;
            }
            childCount++;
        }
    }

    @Override
    public String toString() {
        return DataViewTilingPane.class.getSimpleName() + "('" + layout + "0')";
    }

    public enum Layout {
        HBOX("HBox", new Glyph(FONT_AWESOME, "\uf07e").size(FONT_SIZE)), // TODO: change to more appropriate icon
        VBOX("VBox", new Glyph(FONT_AWESOME, "\uf07d").size(FONT_SIZE)), // TODO: change to more appropriate icon
        GRID("Grid", new Glyph(FONT_AWESOME, "\uf009").size(FONT_SIZE)), // TODO: change to more appropriate icon
        MAXIMISE("Maximise", new Glyph(FONT_AWESOME, "\uf2d0").size(FONT_SIZE));

        private final String name;
        private final Glyph glyph;

        Layout(final String name, final Glyph glyph) {
            this.name = name;
            this.glyph = glyph;
        }

        public Node getIcon() {
            return glyph.duplicate();
        }

        public String getName() {
            return name;
        }
    }
}
