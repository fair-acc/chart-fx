package io.fair_acc.chartfx.ui.utils;

import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * Utility class for registering a post-layout action
 * for one run. It is intended to be registered inside
 * the layoutChildren() phase and will only be run once.
 *
 * Drawing inside layoutChildren is problematic because
 * the layout may be done multiple times and can result
 * in many unnecessary drawing operations.
 *
 * Continuously keeping a post-layout action is also problematic
 * because it (as far as I understand) forces a pulse to happen
 * even if nothing needs to be drawn.
 *
 * @author ennerf
 */
public class PostLayoutHook {

    public PostLayoutHook(Node node, Runnable action) {
        this.node = node;
        this.action = action;
    }

    public void runPostLayout() {
        if (registeredScene != null && registeredScene != node.getScene()) {
            unregister();
        }
        if (registeredScene == null && node.getScene() != null) {
            registeredScene = node.getScene();
            registeredScene.addPostLayoutPulseListener(pulseListener);
        }
    }

    // Executes immediately (for testing)
    @Deprecated
    public void runNow() {
        action.run();
    }

    private void runListener() {
        action.run();
        unregister();
    }

    private void unregister() {
        registeredScene.removePostLayoutPulseListener(pulseListener);
        registeredScene = null;
    }

    final Node node;
    final Runnable action;

    final Runnable pulseListener = this::runListener;
    Scene registeredScene = null;

}
