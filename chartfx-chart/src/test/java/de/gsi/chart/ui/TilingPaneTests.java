package de.gsi.chart.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.ui.TilingPane.Layout;
import de.gsi.chart.utils.FXUtils;

/**
 * Tests {@link de.gsi.chart.ui.TilingPane }
 *
 * @author rstein
 */
@ExtendWith(ApplicationExtension.class)
public class TilingPaneTests {
    private final List<Label> testLabels = new ArrayList<>(4);
    private TilingPane field;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(() -> new TilingPane());
        assertThrows(IllegalArgumentException.class, () -> new TilingPane(null));

        for (int i = 0; i < 4; i++) {
            testLabels.add(new Label("label" + i));
        }
        assertDoesNotThrow(() -> new TilingPane(Layout.GRID, testLabels.toArray(new Node[0])));
        field = new TilingPane(Layout.GRID, testLabels.toArray(new Node[0]));

        stage.setScene(new Scene(field, 100, 100));
        stage.show();
    }

    @Test
    public void testSetterGetter() throws InterruptedException, ExecutionException {
        assertEquals(Layout.GRID, field.getLayout(), "getLayout()");
        assertNotNull(field.toString(), "toString()");
        assertEquals(4, field.getChildren().size());
        testChildFillAndColumnSpanValues();

        FXUtils.runAndWait(() -> field.setLayout(Layout.VBOX));
        assertEquals(Layout.VBOX, field.getLayout(), "getLayout() - VBOX");
        assertEquals(4, field.getChildren().size());
        assertEquals(1, field.getColumnsCount());

        // check for children layout
        testChildFillAndColumnSpanValues();
        assertEquals(0, GridPane.getColumnIndex(testLabels.get(0)), "child0 - colIndex");
        assertEquals(0, GridPane.getRowIndex(testLabels.get(0)), "child0 - rowIndex");
        assertEquals(0, GridPane.getColumnIndex(testLabels.get(1)), "child1 - colIndex");
        assertEquals(1, GridPane.getRowIndex(testLabels.get(1)), "child1 - rowIndex");
        assertEquals(0, GridPane.getColumnIndex(testLabels.get(2)), "child2 - colIndex");
        assertEquals(2, GridPane.getRowIndex(testLabels.get(2)), "child2 - rowIndex");
        assertEquals(0, GridPane.getColumnIndex(testLabels.get(3)), "child3 - colIndex");
        assertEquals(3, GridPane.getRowIndex(testLabels.get(3)), "child3 - rowIndex");

        FXUtils.runAndWait(() -> field.setLayout(Layout.HBOX));
        assertEquals(Layout.HBOX, field.getLayout(), "getLayout() - HBOX");
        assertEquals(4, field.getChildren().size());
        assertEquals(4, field.getColumnsCount());

        // check for children layout
        testChildFillAndColumnSpanValues();
        assertEquals(0, GridPane.getColumnIndex(testLabels.get(0)), "child0 - colIndex");
        assertEquals(0, GridPane.getRowIndex(testLabels.get(0)), "child0 - rowIndex");
        assertEquals(1, GridPane.getColumnIndex(testLabels.get(1)), "child1 - colIndex");
        assertEquals(0, GridPane.getRowIndex(testLabels.get(1)), "child1 - rowIndex");
        assertEquals(2, GridPane.getColumnIndex(testLabels.get(2)), "child2 - colIndex");
        assertEquals(0, GridPane.getRowIndex(testLabels.get(2)), "child2 - rowIndex");
        assertEquals(3, GridPane.getColumnIndex(testLabels.get(3)), "child3 - colIndex");
        assertEquals(0, GridPane.getRowIndex(testLabels.get(3)), "child3 - rowIndex");

        FXUtils.runAndWait(() -> field.setLayout(Layout.GRID));
        assertEquals(Layout.GRID, field.getLayout(), "getLayout() - GRID");
        assertEquals(4, field.getChildren().size());
        assertEquals(2, field.getColumnsCount());

        // check for children layout
        testChildFillAndColumnSpanValues();
        assertEquals(0, GridPane.getColumnIndex(testLabels.get(0)), "child0 - colIndex");
        assertEquals(0, GridPane.getRowIndex(testLabels.get(0)), "child0 - rowIndex");
        assertEquals(1, GridPane.getColumnIndex(testLabels.get(1)), "child1 - colIndex");
        assertEquals(0, GridPane.getRowIndex(testLabels.get(1)), "child1 - rowIndex");
        assertEquals(0, GridPane.getColumnIndex(testLabels.get(2)), "child2 - colIndex");
        assertEquals(1, GridPane.getRowIndex(testLabels.get(2)), "child2 - rowIndex");
        assertEquals(1, GridPane.getColumnIndex(testLabels.get(3)), "child3 - colIndex");
        assertEquals(1, GridPane.getRowIndex(testLabels.get(3)), "child3 - rowIndex");

        final Label newLabel = new Label("label4");
        FXUtils.runAndWait(() -> field.getChildren().add(newLabel));
        assertEquals(Layout.GRID, field.getLayout());
        assertEquals(5, field.getChildren().size());
        assertEquals(3, field.getColumnsCount());

        // check for children layout
        testChildFillAndColumnSpanValues();
        assertEquals(0, GridPane.getColumnIndex(testLabels.get(0)), "child0 - colIndex");
        assertEquals(0, GridPane.getRowIndex(testLabels.get(0)), "child0 - rowIndex");
        assertEquals(1, GridPane.getColumnIndex(testLabels.get(1)), "child1 - colIndex");
        assertEquals(0, GridPane.getRowIndex(testLabels.get(1)), "child1 - rowIndex");
        assertEquals(2, GridPane.getColumnIndex(testLabels.get(2)), "child2 - colIndex");
        assertEquals(0, GridPane.getRowIndex(testLabels.get(2)), "child2 - rowIndex");
        assertEquals(0, GridPane.getColumnIndex(testLabels.get(3)), "child3 - colIndex");
        assertEquals(1, GridPane.getRowIndex(testLabels.get(3)), "child3 - rowIndex");
        // newly added child
        assertEquals(1, GridPane.getColumnIndex(newLabel), "child4 - colIndex");
        assertEquals(1, GridPane.getRowIndex(newLabel), "child4 - rowIndex");
        assertEquals(2, GridPane.getColumnSpan(newLabel), "child4 - columnSpan");

        FXUtils.runAndWait(() -> field.getChildren().clear());
        assertEquals(Layout.GRID, field.getLayout());
        assertEquals(0, field.getChildren().size());
        assertEquals(1, field.getColumnsCount());
    }

    private void testChildFillAndColumnSpanValues() {
        // only check for the first four children,  the fifth has a larger span
        for (int i = 0; i < 4; i++) {
            Label child = testLabels.get(i);
            assertTrue(GridPane.isFillWidth(child), "child" + i + " - isFillWidth");
            assertTrue(GridPane.isFillHeight(child), "child" + i + " - isFillHeight");
            assertEquals(1, GridPane.getColumnSpan(child), "child" + i + " - getColumnSpan()");
        }
    }

    @Test
    public void testLayoutEnum() {
        Node icon;
        for (Layout layout : Layout.values()) {
            assertNotNull(layout.getName());
            icon = layout.getIcon();
            assertNotNull(icon);
            // check that icon is a duplicate
            assertFalse(icon.equals(layout.getIcon()));
        }
    }
}
