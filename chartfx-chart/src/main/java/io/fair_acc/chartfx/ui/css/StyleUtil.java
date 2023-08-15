package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.chartfx.utils.PropUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class for styleable nodes
 *
 * @author ennerf
 */
public class StyleUtil {

    private StyleUtil() {
    }

    /**
     * A node that lets users change styles via CSS. The
     * actual drawing is done in a canvas to reduce the
     * number of nodes on the SceneGraph. Each change
     * increments a counter, so that it is easy to
     * invalidate outdated renderings.
     */
    public interface StyleNode {

        /**
         * Copies all style parameters except for rotate
         * @param gc target context
         */
        default void copyStyleTo(GraphicsContext gc) {
            copyStyle((Node)this, gc);
        }

        default long getChangeCounter() {
            return changeCounterProperty().get();
        }

        /**
         * @return a counter with the total number of style changes
         */
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

    public static void forEachStyleProp(Node node, Consumer<ObservableValue<?>> action) {
        // https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#node
        action.accept(node.visibleProperty());
        action.accept(node.rotateProperty());
        action.accept(node.opacityProperty());

        // https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#shape
        if (node instanceof Shape) {
            Shape shape = (Shape) node;
            action.accept(shape.fillProperty());
            action.accept(shape.strokeProperty());
            action.accept(Bindings.size(shape.getStrokeDashArray()));
            action.accept(shape.strokeDashOffsetProperty());
            action.accept(shape.strokeLineCapProperty());
            action.accept(shape.strokeLineJoinProperty());
            action.accept(shape.strokeMiterLimitProperty());
            action.accept(shape.strokeWidthProperty());

            // https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#text
            if (node instanceof Text) {
                Text text = (Text) node;
                action.accept(text.fontProperty());
                action.accept(text.fontSmoothingTypeProperty());
                action.accept(text.textAlignmentProperty());
                action.accept(text.textOriginProperty());
            }
        }
    }

    public static void copyStyle(Node style, GraphicsContext gc) {
        // rotate, translate, etc. would mess up the coordinate frame
        gc.setGlobalAlpha(style.getOpacity());

        if (style instanceof Shape) {
            Shape shape = (Shape) style;
            gc.setFill(shape.getFill());
            // style.isSmooth(); // no equivalent
            gc.setStroke(shape.getStroke());
            // style.getStrokeType(); // no equivalent
            gc.setLineDashes(toLineDashArray(shape.getStrokeDashArray()));
            gc.setLineDashOffset(shape.getStrokeDashOffset());
            gc.setLineCap(shape.getStrokeLineCap());
            gc.setLineJoin(shape.getStrokeLineJoin());
            gc.setMiterLimit(shape.getStrokeMiterLimit());
            gc.setLineWidth(shape.getStrokeWidth());

            if (style instanceof Text) {
                Text text = (Text) style;
                gc.setFont(text.getFont());
                gc.setFontSmoothingType(text.getFontSmoothingType());
                // style.isStrikethrough(); // no equivalent
                gc.setTextAlign(text.getTextAlignment());
                gc.setTextBaseline(text.getTextOrigin());
                // style.isUnderline(); // no equivalent
            }
        }
    }

    public static String toStyleString(Node style) {
        StringBuilder builder = new StringBuilder();
        for (String styleClass : style.getStyleClass()) {
            builder.append(".").append(styleClass).append(", ");
        }
        removeEndIf(builder, ", ");
        builder.append(" {");
        forEachStyleProp(style, obs -> {
            if (!(obs instanceof StyleableProperty<?>)) {
                return;
            }
            var prop = (StyleableProperty<?>) obs;
            builder.append("\n  ").append(prop.getCssMetaData().getProperty())
                    .append(": ").append(prop.getValue()).append(";");
        });
        builder.append("\n}");
        return builder.toString();
    }

    private static boolean removeEndIf(StringBuilder builder, String end) {
        if (builder.length() < end.length()) {
            return false;
        }
        for (int i = 0; i < end.length(); i++) {
            char a = end.charAt(end.length() - 1 - i);
            char b = builder.charAt(builder.length() - 1 - i);
            if (a != b) {
                return false;
            }
        }
        builder.setLength(builder.length() - end.length());
        return true;
    }

    static Consumer<ObservableValue<?>> incrementOnChange(LongProperty counter) {
        ChangeListener<Object> listener = (obs, old, value) -> counter.set(counter.get() + 1);
        return prop -> prop.addListener(listener);
    }

    public static void copyLineDashes(final GraphicsContext gc, Shape style) {
        gc.setLineDashes(toLineDashArray(style.getStrokeDashArray()));
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
