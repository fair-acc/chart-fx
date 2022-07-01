package io.fair_acc.chartfx.plugins;

import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;

import javafx.scene.input.MouseEvent;

/**
 * Utility methods for operating on {@link MouseEvent}s. (Unfortunately, the original by G.Kruk is package scoped)
 *
 * @author Grzegorz Kruk
 * @author braeun
 */
public class MouseEventsHelper {

    public static boolean isOnlyCtrlModifierDown(final MouseEvent event) {
        return event.isControlDown() && !event.isAltDown() && !event.isMetaDown() && !event.isShiftDown();
    }

    public static boolean isOnlyMiddleButtonDown(final MouseEvent event) {
        return event.isMiddleButtonDown() && !event.isPrimaryButtonDown() && !event.isSecondaryButtonDown();
    }

    public static boolean isOnlyPrimaryButtonDown(final MouseEvent event) {
        return event.getButton() == PRIMARY && !event.isMiddleButtonDown() && !event.isSecondaryButtonDown();
    }

    public static boolean isOnlySecondaryButtonDown(final MouseEvent event) {
        return event.getButton() == SECONDARY && !event.isPrimaryButtonDown() && !event.isMiddleButtonDown();
    }

    public static boolean modifierKeysUp(final MouseEvent event) {
        return !event.isAltDown() && !event.isControlDown() && !event.isMetaDown() && !event.isShiftDown();
    }

}
