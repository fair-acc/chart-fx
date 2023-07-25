package io.fair_acc.chartfx.ui.utils;

import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * Utility class for registering pre-layout and post-layout hooks
 * that get executed once. Each JavaFX tick is executed in phases:
 * <p>
 * 1) animations/timers, e.g., Platform.runLater()
 * 2) CSS styling pass (styling etc. gets updated)
 * 3) pre-layout hook
 * 4) layout pass (layoutChildren)
 * 5) post-layout hook
 * 6) update bounds
 * 7) copy dirty node changes to the rendering thread
 * <p>
 * Drawing inside layout children is problematic because
 * the layout may be recursive and can result in many
 * unnecessary drawing operations.
 * <p>
 * However, constantly keeping a layout hook is also not ideal because
 * (as far as I understand) that will always trigger a JavaFX tick even
 * when it is unnecessary.
 * <p>
 * This class registers actions for just one tick and then automatically
 * unregisters itself. Only the first invocation per tick has an effect.
 * <p>
 * Note that both hooks get added and removed together to avoid issues
 * where users could add a pre-layout hook in the layoutChildren method
 * and end up with actions split across ticks.
 *
 * @author ennerf
 */
public class LayoutHook {

    public static LayoutHook newPreAndPostHook(Node node, Runnable preLayoutAction, Runnable postLayoutAction) {
        return new LayoutHook(node, preLayoutAction, postLayoutAction);
    }

    private LayoutHook(Node node, Runnable preLayoutAction, Runnable postLayoutAction) {
        this.node = node;
        this.preLayoutAction = preLayoutAction;
        this.postLayoutAction = postLayoutAction;
    }

    public void registerOnce() {
        // Scene has changed -> remove the old one first
        if (registeredScene != null && registeredScene != node.getScene()) {
            unregister();
        }
        // Register only if we haven't already registered
        if (registeredScene == null && node.getScene() != null) {
            registeredScene = node.getScene();
            registeredScene.addPreLayoutPulseListener(preLayoutAction);
            registeredScene.addPostLayoutPulseListener(postLayoutAndRemove);
        }
    }

    private void unregister() {
        registeredScene.removePreLayoutPulseListener(preLayoutAction);
        registeredScene.removePostLayoutPulseListener(postLayoutAndRemove);
        registeredScene = null;
    }

    private void runPostlayoutAndRemove() {
        postLayoutAction.run();
        unregister();
    }

    final Node node;
    final Runnable preLayoutAction;
    final Runnable postLayoutAction;

    final Runnable postLayoutAndRemove = this::runPostlayoutAndRemove;
    Scene registeredScene = null;

}
