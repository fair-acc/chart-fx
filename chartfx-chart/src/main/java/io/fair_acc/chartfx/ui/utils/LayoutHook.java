package io.fair_acc.chartfx.ui.utils;

import io.fair_acc.dataset.utils.AssertUtils;
import javafx.beans.value.ChangeListener;
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
        node.sceneProperty().addListener((observable, oldValue, newValue) -> {
            // Make sure we leave the old scene in a consistent state
            if (isRegistered()) {
                if (hasRunPreLayout) {
                   postLayoutAction.run();
                }
                unregister();
            }

            // Register when the scene changes. Note that the scene reference gets
            // set in the CSS phase, so by the time we can register it would
            // already be too late. Waiting for the layout phase wouldn't
            // let us change the scene graph, so the best option we have is
            // run the pre layout hook manually during CSS.
            if (newValue != null) {
                runPreLayoutAndAdd();
            }

            // Also register scene size triggers to catch any manual resizing before
            // the pulse begins.
            if (oldValue != null) {
                oldValue.widthProperty().removeListener(windowResizeListener);
                oldValue.heightProperty().removeListener(windowResizeListener);
            }
            if (newValue != null) {
                newValue.widthProperty().addListener(windowResizeListener);
                newValue.heightProperty().addListener(windowResizeListener);
            }
        });
    }

    /**
     * @return true if the pre layout hook was executed this cycle. Meant to be called during the layout phase.
     */
    public boolean hasRunPreLayout() {
        return hasRunPreLayout;
    }

    public boolean isRegistered() {
        return registeredScene != null;
    }

    public LayoutHook registerOnce() {
        // Already registered or null, so nothing to do
        var scene = node.getScene();
        if (scene == registeredScene) {
            return this;
        }

        // Scene has changed -> remove the old one first
        if (isRegistered()) {
            unregister();
        }

        // Register only if the scene is valid
        if (scene != null) {
            scene.addPreLayoutPulseListener(preLayoutAndAdd);
            registeredScene = scene;
        }
        return this;
    }

    /**
     * Registers the post-layout hook and executes the pre-layout hook if
     * it has not already been executed during this pulse.
     * <p>
     * Generally this should be called by the pulse by registering
     * registerOnce(), but when registering during the CSS or layout
     * this method may be called manually to get it executed within
     * the same pulse.
     */
    private void runPreLayoutAndAdd() {
        // Already ran
        if (hasRunPreLayout()) {
            return;
        }

        // Needs a registration
        if (!isRegistered()) {
            if (node.getScene() == null) {
                return;
            }
            registerOnce();
        }

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
        if (!isRegistered()) {
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
    final ChangeListener<Number> windowResizeListener = (obs, old, size) -> registerOnce();
    Scene registeredScene = null;

}
