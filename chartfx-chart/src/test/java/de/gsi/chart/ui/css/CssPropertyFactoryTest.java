package de.gsi.chart.ui.css;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.api.FxAssert.verifyThat;

import java.util.List;

import javafx.css.*;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.ThrowingSupplier;
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
        assertDoesNotThrow((ThrowingSupplier<?>) CssPropertyFactory::new);
        verifyThat(TestStyleable.CSS.getCssMetaData(), notNullValue());
        verifyThat(styleable.getCssMetaData(), notNullValue());
        verifyThat(TestStyleable.CSS.getCssMetaDataModifyable(), notNullValue());

        verifyThat(styleable.getMinHeight(), equalTo(-1.0)); // parent property
        verifyThat(styleable.getTestDouble(), equalTo(4.2));
        verifyThat(styleable.getTestFloat(), equalTo(1.337f));
        verifyThat(styleable.getTestInt(), equalTo(42));
        verifyThat(styleable.getTestLong(), equalTo(123456789123456789L));
        verifyThat(styleable.getTestString(), equalTo("test"));
        verifyThat(styleable.getTestBool(), equalTo(false));
        verifyThat(styleable.getTestObject(), equalTo(Color.PINK));
        verifyThat(styleable.getTestEnum(), equalTo(TestEnum.ENABLED));
        verifyThat(styleable.getTestEnumP(), equalTo(TestEnum.DISABLED));
        verifyThat(styleable.getPseudoClassStates(), contains(PseudoClass.getPseudoClass("disabled")));
        verifyThat(styleable.getPseudoClassStates(), not(contains(PseudoClass.getPseudoClass("enabled"))));
        fxRobot.interact(() -> styleable.setStyle("-fx-test-double: 2.3; -fx-test-float: 1.1; -fx-test-int: 23; -fx-test-long: 24; -fx-test-string: styletest;-fx-test-bool: true;-fx-test-object: green;-fx-test-enum: disabled;-fx-test-enum-p: enabled; -fx-min-height: 13;"));
        verifyThat(styleable.getMinHeight(), equalTo(13.0)); // parent property
        verifyThat(styleable.getTestDouble(), equalTo(2.3));
        verifyThat(styleable.getTestFloat(), equalTo(1.1f));
        verifyThat(styleable.getTestObject(), equalTo(Color.GREEN));
        verifyThat(styleable.getTestInt(), equalTo(23));
        verifyThat(styleable.getTestLong(), equalTo(24L));
        verifyThat(styleable.getTestString(), equalTo("styletest"));
        verifyThat(styleable.getTestEnum(), equalTo(TestEnum.DISABLED));
        verifyThat(styleable.getTestEnumP(), equalTo(TestEnum.ENABLED));
        verifyThat(styleable.getTestBool(), equalTo(true));
        verifyThat(styleable.getPseudoClassStates(), not(contains(PseudoClass.getPseudoClass("disabled"))));
        verifyThat(styleable.getPseudoClassStates(), contains(PseudoClass.getPseudoClass("enabled")));

        // test error cases
        // double - erroneous (propertyName-variable-field mismatch)
        assertThrows(IllegalArgumentException.class, () -> styleable.testDouble2 = TestStyleable.CSS.createDoubleProperty(styleable, "testDouble", 4.2, true, null));
        assertThrows(IllegalArgumentException.class, () -> styleable.testDouble3 = TestStyleable.CSS.createDoubleProperty(styleable, "testDouble1", 4.2, true, null));
        assertThrows(IllegalArgumentException.class, () -> styleable.testDouble3 = TestStyleable.CSS.createDoubleProperty(null, "testDouble1", 4.2, true, null));

        assertEquals("-fx-test-property-name", TestStyleable.CSS.getCssPropertyName("testPropertyName"));
        assertEquals("-fx-test-property-name2", TestStyleable.CSS.getCssPropertyName("testPropertyName2"));

        // applying via css sheet
        // test with multiple instances
    }

    public static class TestStyleable extends Region {
        private static final CssPropertyFactory<TestStyleable> CSS = new CssPropertyFactory<>(Region.getClassCssMetaData());
        // double
        private final StyleableDoubleProperty testDouble = CSS.createDoubleProperty(this, "testDouble", 4.2, true, null);
        // float
        private final StyleableFloatProperty testFloat = CSS.createFloatProperty(this, "testFloat", 1.337f, true, null);
        // int
        private final StyleableIntegerProperty testInt = CSS.createIntegerProperty(this, "testInt", 42, true, null);
        // long
        private final StyleableLongProperty testLong = CSS.createLongProperty(this, "testLong", 123456789123456789L, true, null);
        // string
        private final StyleableStringProperty testString = CSS.createStringProperty(this, "testString", "test", true, null);
        // boolean
        private final StyleableBooleanProperty testBool = CSS.createBooleanProperty(this, "testBool", false, true, null);
        // object
        private final StyleableObjectProperty<Paint> testObject = CSS.createObjectProperty(this, "testObject", Color.PINK, true, StyleConverter.getPaintConverter(), null);
        // enum
        private final StyleableObjectProperty<TestEnum> testEnum = CSS.createObjectProperty(this, "testEnum", TestEnum.ENABLED, true, StyleConverter.getEnumConverter(TestEnum.class), null);
        // enum with pseudoclass
        private final StyleableObjectProperty<TestEnum> testEnumP = CSS.createEnumPropertyWithPseudoclasses(this, "testEnumP", TestEnum.DISABLED, true, TestEnum.class, null);
        // double - erroneous (propertyName-variable-field mismatch)
        protected StyleableDoubleProperty testDouble2;
        protected StyleableDoubleProperty testDouble3;

        public double getTestDouble() {
            return testDouble.getValue();
        }
        public float getTestFloat() {
            return testFloat.getValue();
        }
        public int getTestInt() {
            return testInt.getValue();
        }
        public long getTestLong() {
            return testLong.getValue();
        }
        public String getTestString() {
            return testString.getValue();
        }
        public boolean getTestBool() {
            return testBool.getValue();
        }
        public Paint getTestObject() {
            return testObject.getValue();
        }
        public TestEnum getTestEnum() {
            return testEnum.getValue();
        }
        public TestEnum getTestEnumP() {
            return testEnumP.getValue();
        }
        @Override
        public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
            return CSS.getCssMetaData();
        }
    }

    public enum TestEnum {
        ENABLED,
        DISABLED
    }
}
