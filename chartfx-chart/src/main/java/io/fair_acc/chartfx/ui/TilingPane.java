package io.fair_acc.chartfx.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.kordamp.ikonli.javafx.FontIcon;

/**
 * TilingPane that to mimics HBox-, VBox-, or TilePane layout while consistently maximising it's children and
 * following layout constraints from its parent
 *
 * @author rstein
 */
public class TilingPane extends GridPane {
    protected static final String FONT_AWESOME = "FontAwesome";
    protected static final int FONT_SIZE = 20;
    private final ObjectProperty<Layout> layout = new SimpleObjectProperty<>(this, "layout", Layout.GRID) {
        @Override
        public void set(final Layout newLayout) {
            if (newLayout == null) {
                throw new IllegalArgumentException("layout must not be null");
            }
            super.set(newLayout);
        }
    };

    public TilingPane() {
        this(Layout.GRID);
    }

    public TilingPane(final Layout layout) {
        this(layout, (Node[]) null);
    }

    public TilingPane(final Layout layout, Node... nodes) {
        super();
        this.layout.set(layout);
        VBox.setVgrow(this, Priority.ALWAYS);
        getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                layoutNormal();
            }
        });

        this.layout.addListener((ch, o, n) -> layoutNormal());

        if (nodes != null) {
            getChildren().addAll(nodes);
        }
    }

    public Layout getLayout() {
        return layoutProperty().get();
    }

    public ObjectProperty<Layout> layoutProperty() {
        return layout;
    }

    public void setLayout(final Layout value) {
        layoutProperty().set(value);
    }

    @Override
    public String toString() {
        return TilingPane.class.getSimpleName() + "('" + getLayout() + "0')";
    }

    protected int getColumnsCount() {
        final int childCount = getChildren().size();
        if (childCount == 0) {
            return 1;
        }
        switch (getLayout()) {
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

            return (int) Math.ceil(Math.sqrt(childCount)); // n-columns
        }
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
                colConstraints.setFillWidth(true);
                colConstraintList.add(colConstraints);
            }
            getColumnConstraints().setAll(colConstraintList);
        }

        int rowIndex = 0;
        int colIndex = 0;
        int childCount = 0;
        final int nChildren = getChildren().size();
        int nColSpan = Math.max(1, colsCount / (nChildren - childCount));
        for (final Node child : getChildren()) {
            GridPane.setFillWidth(child, true);
            GridPane.setFillHeight(child, true);
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

    public enum Layout {
        HBOX("HBox", "fas-arrows-alt-h:" + FONT_SIZE),
        VBOX("VBox", "fas-arrows-alt-v:" + FONT_SIZE),
        GRID("Grid", "fas-th:" + FONT_SIZE),
        MAXIMISE("Maximise", "fas-window-maximize:" + FONT_SIZE);

        private final String name;
        private final String iconCode;

        Layout(final String name, final String iconCode) {
            this.name = name;
            this.iconCode = iconCode;
        }

        public Node getIcon() {
            return new FontIcon(iconCode);
        }

        public String getName() {
            return name;
        }
    }
}
