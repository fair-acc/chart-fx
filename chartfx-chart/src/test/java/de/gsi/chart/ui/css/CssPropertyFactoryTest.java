package de.gsi.chart.ui.css;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.testfx.api.FxAssert.verifyThat;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableFloatProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableLongProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleableStringProperty;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;

/**
 * Tests {@link de.gsi.chart.ui.css.CssPropertyFactory }
 *
 * @author akrimm
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class CssPropertyFactoryTest {
    private final TestStyleable styleable = new TestStyleable();

    @Start
    public void start(Stage stage) {
        // we need to add the styleable to a scene and display it or the css will not be evsaluated
        final VBox root = new VBox(styleable);
        final Scene scene = new Scene(root, 100, 120);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testApplyingStyle(FxRobot fxRobot) {
        verifyThat(styleable.getMinHeight(), equalTo(-1.0)); // parent property
        verifyThat(styleable.getTestDouble().getValue(), equalTo(4.2));
        verifyThat(styleable.getTestInt().getValue(), equalTo(42));
        verifyThat(styleable.getTestString().getValue(), equalTo("test"));
        verifyThat(styleable.getTestBool().getValue(), equalTo(false));
        verifyThat(styleable.getTestObject().getValue(), equalTo(Color.PINK));
        verifyThat(styleable.getTestEnum().getValue(), equalTo(TestEnum.ENABLED));
        verifyThat(styleable.getTestEnumP().getValue(), equalTo(TestEnum.DISABLED));
        verifyThat(styleable.getPseudoClassStates(), Matchers.contains(PseudoClass.getPseudoClass("disabled")));
        verifyThat(styleable.getPseudoClassStates(), not(contains(PseudoClass.getPseudoClass("enabled"))));
        fxRobot.interact(() -> styleable.setStyle("-fx-test-double: 2.3; -fx-test-int: 23; -fx-test-string: styletest;-fx-test-bool: true;-fx-test-object: green;-fx-test-enum: disabled;-fx-test-enum-p: enabled; -fx-min-height: 13;"));
        verifyThat(styleable.getMinHeight(), equalTo(13.0)); // parent property
        verifyThat(styleable.getTestDouble().getValue(), equalTo(2.3));
        verifyThat(styleable.getTestObject().getValue(), equalTo(Color.GREEN));
        verifyThat(styleable.getTestInt().getValue(), equalTo(23));
        verifyThat(styleable.getTestString().getValue(), equalTo("styletest"));
        verifyThat(styleable.getTestEnum().getValue(), equalTo(TestEnum.DISABLED));
        verifyThat(styleable.getTestEnumP().getValue(), equalTo(TestEnum.ENABLED));
        verifyThat(styleable.getTestBool().getValue(), equalTo(true));
        verifyThat(styleable.getPseudoClassStates(), not(contains(PseudoClass.getPseudoClass("disabled"))));
        verifyThat(styleable.getPseudoClassStates(), contains(PseudoClass.getPseudoClass("enabled")));

        // test applying via css sheet
        // test with multiple instances
    }

    public static class TestStyleable extends Region {
        private static final CssPropertyFactory<TestStyleable> CSS = new CssPropertyFactory<>(Region.getClassCssMetaData());

        @Override
        public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
            return CSS.getCssMetaData();
        }

        // double
        private final DoubleProperty testDouble = CSS.createDoubleProperty(this, "testDouble", "-fx-test-double",
                s -> (StyleableDoubleProperty) s.getTestDouble(), 4.2, true, null);

        public DoubleProperty getTestDouble() {
            return testDouble;
        }

        // float
        private final FloatProperty testFloat = CSS.createFloatProperty(this, "testFloat", "-fx-test-float",
                s -> (StyleableFloatProperty) s.getTestFloat(), 1.337f, true, null);

        public FloatProperty getTestFloat() {
            return testFloat;
        }

        // int
        private final IntegerProperty testInt = CSS.createIntegerProperty(this, "testInt", "-fx-test-int", s -> (StyleableIntegerProperty) s.getTestInt(), 42, true, null);

        public IntegerProperty getTestInt() {
            return testInt;
        }

        // long
        private final LongProperty testLong = CSS.createLongProperty(this, "testLong", "-fx-test-long", s -> (StyleableLongProperty) s.getTestLong(), 123456789123456789l, true, null);

        public LongProperty getTestLong() {
            return testLong;
        }

        // string
        private final StringProperty testString = CSS.createStringProperty(this, "testString", "-fx-test-string",
                s -> (StyleableStringProperty) s.getTestString(), "test", true, null);

        public StringProperty getTestString() {
            return testString;
        }

        // boolean
        private final BooleanProperty testBool = CSS.createBooleanProperty(this, "testBool", "-fx-test-bool", s -> (StyleableBooleanProperty) s.getTestBool(), false, true, null);

        public BooleanProperty getTestBool() {
            return testBool;
        }

        // object
        private final ObjectProperty<Paint> testObject = CSS.createObjectProperty(this, "testObject", "-fx-test-object",
                s -> (StyleableProperty<Paint>) s.getTestObject(), Color.PINK, true, StyleConverter.getPaintConverter(), null);

        public ObjectProperty<Paint> getTestObject() {
            return testObject;
        }

        // enum
        private final ObjectProperty<TestEnum> testEnum = CSS.createObjectProperty(this, "testEnum", "-fx-test-enum",
                s -> (StyleableProperty<TestEnum>) s.getTestEnum(), TestEnum.ENABLED, true, StyleConverter.getEnumConverter(TestEnum.class), null);

        public ObjectProperty<TestEnum> getTestEnum() {
            return testEnum;
        }

        // enum with pseudoclass
        private final ObjectProperty<TestEnum> testEnumP = CSS.createEnumPropertyWithPseudoclasses(this, "testEnumP", "-fx-test-enum-p",
                s -> (StyleableProperty<TestEnum>) s.getTestEnumP(), TestEnum.DISABLED, true, TestEnum.class, null);

        public ObjectProperty<TestEnum> getTestEnumP() {
            return testEnumP;
        }
    }

    public static enum TestEnum {
        ENABLED,
        DISABLED;
    }
}
