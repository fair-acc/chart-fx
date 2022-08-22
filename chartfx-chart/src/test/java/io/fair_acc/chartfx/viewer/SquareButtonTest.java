package io.fair_acc.chartfx.viewer;

import io.fair_acc.chartfx.ui.TilingPane.Layout;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils.JavaFxInterceptor;
import io.fair_acc.chartfx.utils.FXUtils;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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

        root = new StackPane(field);
        Scene scene = new Scene(root, 120, 200);
        stage.setScene(scene);
        stage.show();
        sceneWindow = scene.getWindow();
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
    public void heightChangeListener_fractionAppearsInParentHeightDueToScalingAndSnapToPixelFlag_theFractionIsIgnored() {
        FXUtils.assertJavaFxThread();
        ensureParentIsNotNull();
        double originalRenderScaleY = sceneWindow.getRenderScaleY();
        Region parent = (Region) field.getParent();
        double originalParentHeight = parent.getHeight();
        try {
            sceneWindow.setRenderScaleY(1.17);
            double targetHeight = parent.snapSizeY(50);
            double targetHeightWithoutFraction = Math.floor(targetHeight);
            assertThat(targetHeight).isNotEqualTo(targetHeightWithoutFraction);
            forceChangingOfButtonHeightViaParent(parent, targetHeight);
            assertThat(parent.getHeight()).isEqualTo(targetHeight);
            assertThat(field.getPrefHeight()).isEqualTo(targetHeightWithoutFraction);
        }
        finally {
            sceneWindow.setRenderScaleY(originalRenderScaleY);
            parent.resize(parent.getWidth(), originalParentHeight);
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
