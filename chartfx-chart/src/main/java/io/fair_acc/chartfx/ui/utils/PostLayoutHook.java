package io.fair_acc.chartfx.ui.utils;

import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * @author Florian Enner
 * @since 23 Jun 2023
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
        if (registeredScene == null) {
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
