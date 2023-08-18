package io.fair_acc.chartfx.renderer.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import javafx.scene.canvas.Canvas;

import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.Test;

import io.fair_acc.chartfx.renderer.ContourType;
import io.fair_acc.chartfx.renderer.datareduction.ReductionType;
import io.fair_acc.chartfx.renderer.spi.utils.ColorGradient;
import io.fair_acc.dataset.DataSet;

/**
 * Basic getter/setter tests for {@link io.fair_acc.chartfx.renderer.spi.AbstractContourDataSetRendererParameter}
 * @author rstein
 */
public class AbstractContourDataSetRendererParameterTests {
    @Test
    public void basicGetterSetterTests() {
        assertDoesNotThrow(TestContourDataSetRendererParameter::new);

        final TestContourDataSetRendererParameter renderer = new TestContourDataSetRendererParameter();

        renderer.setAltImplementation(true);
        assertTrue(renderer.isAltImplementation());
        renderer.setAltImplementation(false);
        assertFalse(renderer.isAltImplementation());

        renderer.setColorGradient(ColorGradient.BLUERED);
        assertEquals(ColorGradient.BLUERED, renderer.getColorGradient());

        renderer.setComputeLocalRange(true);
        assertTrue(renderer.computeLocalRange());
        renderer.setComputeLocalRange(false);
        assertFalse(renderer.computeLocalRange());

        renderer.setContourType(ContourType.CONTOUR_HEXAGON);
        assertEquals(ContourType.CONTOUR_HEXAGON, renderer.getContourType());

        renderer.setMaxContourSegments(42);
        assertEquals(42, renderer.getMaxContourSegments());

        renderer.setMinHexTileSizeProperty(101);
        assertEquals(101, renderer.getMinHexTileSizeProperty());

        renderer.setNumberQuantisationLevels(3);
        assertEquals(3, renderer.getNumberQuantisationLevels());

        renderer.setReductionFactorX(4);
        assertEquals(4, renderer.getReductionFactorX());
        renderer.setReductionFactorY(3);
        assertEquals(3, renderer.getReductionFactorY());

        renderer.setReductionType(ReductionType.AVERAGE);
        assertEquals(ReductionType.AVERAGE, renderer.getReductionType());

        renderer.setSmooth(true);
        assertTrue(renderer.isSmooth());
        renderer.setSmooth(false);
        assertFalse(renderer.isSmooth());
    }

    /**
     * basic test class, only supports limited getter/setter/property functions
     */
    public static class TestContourDataSetRendererParameter extends AbstractContourDataSetRendererParameter<TestContourDataSetRendererParameter> {

        @Override
        public boolean drawLegendSymbol(DataSetNode dataSet, Canvas canvas) {
            return super.drawLegendSymbol(dataSet, canvas);
        }

        @Override
        protected void render(GraphicsContext gc, DataSet dataSet, DataSetNode style) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected TestContourDataSetRendererParameter getThis() {
            return this;
        }
    }
}
