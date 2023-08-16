package io.fair_acc.chartfx.renderer.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.canvas.Canvas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.datareduction.DefaultDataReducer;
import io.fair_acc.chartfx.renderer.datareduction.MaxDataReducer;
import io.fair_acc.dataset.DataSet;

/**
 * Basic getter/setter tests for {@link io.fair_acc.chartfx.renderer.spi.AbstractErrorDataSetRendererParameter}
 * @author rstein
 */
public class AbstractErrorDataSetRendererParameterTests {
    @Test
    public void basicGetterSetterTests() {
        assertDoesNotThrow(TestErrorDataSetRendererParameter::new);

        final TestErrorDataSetRendererParameter renderer = new TestErrorDataSetRendererParameter();

        renderer.setAllowNaNs(true);
        assertTrue(renderer.isallowNaNs());
        renderer.setAllowNaNs(false);
        assertFalse(renderer.isallowNaNs());

        renderer.setBarWidth(13);
        assertEquals(13, renderer.getBarWidth());

        renderer.setBarWidthPercentage(42);
        assertEquals(42, renderer.getBarWidthPercentage());

        renderer.setDashSize(7);
        assertEquals(7, renderer.getDashSize());

        renderer.setDrawBars(true);
        assertTrue(renderer.isDrawBars());
        renderer.setDrawBars(false);
        assertFalse(renderer.isDrawBars());

        renderer.setDrawBubbles(true);
        assertTrue(renderer.isDrawBubbles());
        renderer.setDrawBubbles(false);
        assertFalse(renderer.isDrawBubbles());

        renderer.setDrawChartDataSets(true);
        assertTrue(renderer.isDrawChartDataSets());
        renderer.setDrawChartDataSets(false);
        assertFalse(renderer.isDrawChartDataSets());

        renderer.setDrawMarker(true);
        assertTrue(renderer.isDrawMarker());
        renderer.setDrawMarker(false);
        assertFalse(renderer.isDrawMarker());

        renderer.setDynamicBarWidth(true);
        assertTrue(renderer.isDynamicBarWidth());
        renderer.setDynamicBarWidth(false);
        assertFalse(renderer.isDynamicBarWidth());

        for (ErrorStyle eStyle : ErrorStyle.values()) {
            renderer.setErrorType(eStyle);
            assertEquals(eStyle, renderer.getErrorType());
        }
        renderer.setIntensityFading(0.85);
        assertEquals(0.85, renderer.getIntensityFading());

        renderer.setMarkerSize(4);
        assertEquals(4, renderer.getMarkerSize());

        for (LineStyle eStyle : LineStyle.values()) {
            renderer.setPolyLineStyle(eStyle);
            assertEquals(eStyle, renderer.getPolyLineStyle());
        }

        assertEquals(DefaultDataReducer.class, renderer.getRendererDataReducer().getClass());
        renderer.setRendererDataReducer(new MaxDataReducer());
        assertEquals(MaxDataReducer.class, renderer.getRendererDataReducer().getClass());
        renderer.setRendererDataReducer(null);
        assertEquals(DefaultDataReducer.class, renderer.getRendererDataReducer().getClass());

        renderer.setShiftBar(true);
        assertTrue(renderer.isShiftBar());
        renderer.setShiftBar(false);
        assertFalse(renderer.isShiftBar());

        renderer.setshiftBarOffset(5);
        assertEquals(5, renderer.getShiftBarOffset());
    }

    @Test
    public void testBindings() {
        final TestErrorDataSetRendererParameter renderer1 = new TestErrorDataSetRendererParameter();
        final TestErrorDataSetRendererParameter renderer2 = new TestErrorDataSetRendererParameter();

        Assertions.assertDoesNotThrow(() -> renderer1.bind(renderer2));

        DefaultNumericAxis test = new DefaultNumericAxis("test axis");
        renderer2.getAxes().add(test);
        assertEquals(test, renderer2.getAxes().get(0));

        assertDoesNotThrow(renderer2::unbind);
    }

    /**
     * basic test class, only supports limited getter/setter/property functions
     */
    public static class TestErrorDataSetRendererParameter extends AbstractErrorDataSetRendererParameter<TestErrorDataSetRendererParameter> {
        @Override
        public Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void render() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected TestErrorDataSetRendererParameter getThis() {
            return this;
        }
    }
}
