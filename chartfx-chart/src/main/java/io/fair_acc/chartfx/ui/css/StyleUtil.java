package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.chartfx.utils.PropUtil;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

import java.util.List;

/**
 * Utility class for styleable nodes
 *
 * @author ennerf
 */
public class StyleUtil {

    private StyleUtil() {
    }

    public interface ChangeCounter {
        default long getChangeCounter() {
            return changeCounterProperty().get();
        }

        ReadOnlyLongProperty changeCounterProperty();
    }

    public static <NODE extends Node> NODE hiddenStyleNode(NODE node, String... styles) {
        styleNode(node, styles);
        node.setVisible(false); // don't let CSS modify visibility
        return node;
    }

    public static <NODE extends Node> NODE styleNode(NODE node, String... styles) {
        // Note: we can't modify visibility, otherwise it can't be set via CSS anymore
        addStyles(node, styles);
        node.setManaged(false);
        return node;
    }

    public static <NODE extends Node> NODE addStyles(NODE node, String... styles) {
        node.getStyleClass().addAll(styles);
        return node;
    }

    public static <NODE extends Node> NODE hide(NODE node) {
        node.setVisible(false);
        node.setManaged(false);
        return node;
    }

    public static void applyPseudoClass(Node node, PseudoClass clazz, ObservableBooleanValue condition) {
        node.pseudoClassStateChanged(clazz, condition.get());
        PropUtil.runOnChange(() ->  {
            // We immediately apply style changes caused by updating the
            // pseudo class to avoid triggering another tick.
            node.pseudoClassStateChanged(clazz, condition.get());
            node.applyCss();
        }, condition);
    }

    static void copyNodeStyle(Node style, GraphicsContext gc) {
        // https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#node
        // rotate, translate, etc. would mess up the coordinate frame
        gc.setGlobalAlpha(style.getOpacity());
    }

    static void registerNodeListener(Node style, ChangeListener<Object> listener) {
        style.opacityProperty().addListener(listener);
        style.rotateProperty().addListener(listener);
        style.visibleProperty().addListener(listener);
    }

    static void copyShapeStyle(Shape style, GraphicsContext gc) {
        // https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#shape
        gc.setFill(style.getFill());
        // style.isSmooth(); // no equivalent
        gc.setStroke(style.getStroke());
        // style.getStrokeType(); // no equivalent
        gc.setLineDashes(toLineDashArray(style.getStrokeDashArray()));
        gc.setLineDashOffset(style.getStrokeDashOffset());
        gc.setLineCap(style.getStrokeLineCap());
        gc.setLineJoin(style.getStrokeLineJoin());
        gc.setMiterLimit(style.getStrokeMiterLimit());
        gc.setLineWidth(style.getStrokeWidth());
        copyNodeStyle(style, gc);
    }

    static void registerShapeListener(Shape style, ChangeListener<Object> listener) {
        style.fillProperty().addListener(listener);
        style.strokeProperty().addListener(listener);
        Bindings.size(style.getStrokeDashArray()).addListener(listener);
        style.strokeDashOffsetProperty().addListener(listener);
        style.strokeLineCapProperty().addListener(listener);
        style.strokeLineJoinProperty().addListener(listener);
        style.strokeMiterLimitProperty().addListener(listener);
        style.strokeWidthProperty().addListener(listener);
        registerNodeListener(style, listener);
    }

    static void copyTextStyle(Text style, GraphicsContext gc) {
        // https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#text
        gc.setFont(style.getFont());
        gc.setFontSmoothingType(style.getFontSmoothingType());
        // style.isStrikethrough(); // no equivalent
        gc.setTextAlign(style.getTextAlignment());
        gc.setTextBaseline(style.getTextOrigin());
        // style.isUnderline(); // no equivalent
        copyShapeStyle(style, gc);
    }

    static void registerTextListener(Text style, ChangeListener<Object> listener) {
        style.fontProperty().addListener(listener);
        style.fontSmoothingTypeProperty().addListener(listener);
        style.textAlignmentProperty().addListener(listener);
        style.textOriginProperty().addListener(listener);
        registerShapeListener(style, listener);
    }

    static ChangeListener<Object> incrementOnChange(LongProperty counter) {
        return (obs, old, value) -> counter.set(counter.get() + 1);
    }

    private static double[] toLineDashArray(List<Double> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return null;
        }
        FXUtils.assertJavaFxThread();
        double[] array = cachedDashArray;
        if (array.length != numbers.size()) {
            array = new double[numbers.size()];
        }
        int i = 0;
        for (Double number : numbers) {
            array[i++] = number.doubleValue();
        }
        return cachedDashArray = array;
    }
    // small and only called from JavaFX thread, so we can cache statically
    private static double[] cachedDashArray = new double[2];

}
