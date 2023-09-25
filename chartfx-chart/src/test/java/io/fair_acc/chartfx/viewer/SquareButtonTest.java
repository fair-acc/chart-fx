package io.fair_acc.chartfx.viewer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ExecutionException;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.skin.ToolBarSkin;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import com.sun.javafx.scene.control.ContextMenuContent;

import io.fair_acc.chartfx.ui.TilingPane.Layout;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils.JavaFxInterceptor;
import io.fair_acc.chartfx.utils.FXUtils;

/**
 * Tests {@link io.fair_acc.chartfx.viewer.SquareButton }
 *
 * @author rstein
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(JavaFxInterceptor.class)
public class SquareButtonTest {
    private Node icon;
    private SquareButton field;
    private StackPane root;
    private Group group;
    private Window sceneWindow;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(() -> new SquareButton(""));

        icon = Layout.GRID.getIcon();
        field = new SquareButton(null, icon);
        field.setStyle("-fx-padding: 1.69px");

        root = new StackPane(field);
        root.setStyle("-fx-border-color: black; -fx-border-width: 1.33px");
        Scene scene = new Scene(root, 120, 200);
        stage.setScene(scene);
        stage.show();
        sceneWindow = scene.getWindow();
    }

    @BeforeEach
    public void beforeEach() {
        ensureParentIsNotNull();
    }

    @Test
    public void testSetterGetter() throws InterruptedException, ExecutionException {
        FXUtils.assertJavaFxThread();

        assertNull(field.getText(), "getText()");
        assertNotNull(field.toString(), "toString()");
        // could not check for identity since ButtonBase slightly modifies the Graphics (css-related)
        assertEquals(icon, field.getGraphic());
        assertNotNull(field.getGraphic(), "getGraphic()");

        field.setMinWidth(50);
        assertEquals(field.getHeight(), field.getWidth(), "getHeight() == getWidth()");

        field.setMinWidth(60);
        assertEquals(field.getHeight(), field.getWidth(), "getHeight() == getWidth()");

        root.getChildren().remove(field);
        field.setMinHeight(Region.USE_COMPUTED_SIZE);

        assertDoesNotThrow(() -> group = new Group(field));
        Assertions.assertDoesNotThrow(() -> group.getChildren().remove(field));
    }

    @Test
    public void heightChangeListener_scalingWithSnapToPixel_snappedPreferredHeightWithPaddingsIsCloseToSnappedParentHeightWithoutInsets() {
        FXUtils.assertJavaFxThread();
        final boolean originalSnapToPixel = field.isSnapToPixel();
        final double originalRenderScaleY = sceneWindow.getRenderScaleY();
        final Region parent = (Region) field.getParent();
        final double originalParentHeight = parent.getHeight();
        final double scale = 1.17;
        final double delta = 1 / scale;
        try {
            field.setSnapToPixel(true);
            sceneWindow.setRenderScaleY(scale);
            final double parentHeight = 50;
            final double snappedParentHeight = parent.snapSizeY(parentHeight);
            assertThat(snappedParentHeight).isGreaterThan(parentHeight);
            final double childMaximumHeight = snappedParentHeight - parent.getInsets().getTop() - parent.getInsets().getBottom();
            forceChangingOfButtonHeightViaParent(parent, snappedParentHeight);
            assertThat(parent.getHeight()).isEqualTo(snappedParentHeight, within(delta));
            assertThat(field.snapSizeY(field.getPrefHeight()))
                    .isLessThan(childMaximumHeight)
                    .isCloseTo(childMaximumHeight, within(delta));
        } finally {
            field.setSnapToPixel(originalSnapToPixel);
            sceneWindow.setRenderScaleY(originalRenderScaleY);
            parent.resize(parent.getWidth(), originalParentHeight);
        }
    }

    @Test
    public void heightChangeListener_scalingWithoutSnapToPixel_snappedPreferredHeightWithPaddingsIsCloseToSnappedParentHeightWithoutInsets() {
        FXUtils.assertJavaFxThread();
        final boolean originalSnapToPixel = field.isSnapToPixel();
        final double originalRenderScaleY = sceneWindow.getRenderScaleY();
        final Region parent = (Region) field.getParent();
        final double originalParentHeight = parent.getHeight();
        final double scale = 1.17;
        final double delta = 1 / originalRenderScaleY;
        try {
            field.setSnapToPixel(false);
            sceneWindow.setRenderScaleY(scale);
            final double parentHeight = 50;
            final double snappedParentHeight = parent.snapSizeY(parentHeight);
            assertThat(snappedParentHeight).isGreaterThan(parentHeight);
            final double childMaximumHeight = snappedParentHeight - parent.getInsets().getTop() - parent.getInsets().getBottom();
            forceChangingOfButtonHeightViaParent(parent, snappedParentHeight);
            assertThat(parent.getHeight()).isEqualTo(snappedParentHeight, within(delta));
            assertThat(field.snapSizeY(field.getPrefHeight())).isCloseTo(childMaximumHeight, within(delta));
        } finally {
            field.setSnapToPixel(originalSnapToPixel);
            sceneWindow.setRenderScaleY(originalRenderScaleY);
            parent.resize(parent.getWidth(), originalParentHeight);
        }
    }

    @Test
    public void heightChangeListener_parentAvailableHeightIsZero_preferredHeightIsMaxButtonSize() {
        FXUtils.assertJavaFxThread();
        final CustomMenuItem menuItem = new CustomMenuItem(field);
        final ContextMenu contextMenu = new ContextMenu(menuItem);
        try {
            contextMenu.show(sceneWindow);
            final double expected = field.snapSizeY(
                    SquareButton.MAX_BUTTON_SIZE + field.getPadding().getTop() + field.getPadding().getBottom());
            final double delta = 1 / field.getScene().getWindow().getRenderScaleY();
            assertThat(field.getPrefHeight()).isCloseTo(expected, within(delta));
            contextMenu.hide();
        } finally {
            menuItem.setContent(null);
            contextMenu.getItems().remove(menuItem);
        }
    }

    private void forceChangingOfButtonHeightViaParent(Region parent, double targetHeight) {
        parent.resize(parent.getWidth(), targetHeight);
    }

    private void ensureParentIsNotNull() {
        if (!root.getChildren().contains(field)) {
            root.getChildren().add(field);
        }
    }
}
