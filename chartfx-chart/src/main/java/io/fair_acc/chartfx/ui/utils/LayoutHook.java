package io.fair_acc.chartfx.ui.utils;

import io.fair_acc.dataset.utils.AssertUtils;
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
        this.preLayoutAction = AssertUtils.notNull("preLayoutAction", preLayoutAction);
        this.postLayoutAction = AssertUtils.notNull("preLayoutAction", postLayoutAction);
    }

    /**
     * @return true if the pre layout hook was executed this cycle. Meant to be called during the layout phase.
     */
    public boolean hasRunPreLayout() {
        return hasRunPreLayout;
    }

    public LayoutHook registerOnce() {
        // Potentially called before proper initialization
        if (preLayoutAction == null || postLayoutAndRemove == null) {
            return this;
        }

        // Already registered or null, so nothing to do
        var scene = node.getScene();
        if (scene == registeredScene) {
            return this;
        }

        // Scene has changed -> remove the old one first
        if (registeredScene != null) {
            unregister();
        }

        // Register only if the scene is valid
        if (scene != null) {
            scene.addPreLayoutPulseListener(preLayoutAndAdd);
            registeredScene = scene;
        }
        return this;
    }

    private void runPreLayoutAndAdd() {
        // We don't want to be in a position where the post layout listener
        // runs by itself, so we don't register until we made sure that the
        // pre-layout action ran before.
        hasRunPreLayout = true;
        preLayoutAction.run();
        registeredScene.addPostLayoutPulseListener(postLayoutAndRemove);
    }

    private void runPostLayoutAndRemove() {
        postLayoutAction.run();
        unregister();
    }

    private void unregister() {
        if (registeredScene == null) {
            return;
        }
        registeredScene.removePreLayoutPulseListener(preLayoutAndAdd);
        registeredScene.removePostLayoutPulseListener(postLayoutAndRemove);
        registeredScene = null;
        hasRunPreLayout = false;
    }

    final Node node;
    final Runnable preLayoutAction;
    final Runnable postLayoutAction;
    boolean hasRunPreLayout = false;

    final Runnable preLayoutAndAdd = this::runPreLayoutAndAdd;
    final Runnable postLayoutAndRemove = this::runPostLayoutAndRemove;
    Scene registeredScene = null;

}