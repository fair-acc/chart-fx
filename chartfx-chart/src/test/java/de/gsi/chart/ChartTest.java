package de.gsi.chart;

import static org.junit.jupiter.api.Assertions.*;

import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
@SuppressWarnings({ "PMD.UncommentedEmptyMethodBody" })
public class ChartTest {
    private TestChart chart;

    @BeforeEach
    public void setup() {
        chart = new TestChart();
    }

    @TestFx
    public void setTitlePaint() {
        chart.setTitlePaint(Color.BLUE);
        assertEquals(Color.BLUE, chart.getTitlePaint().getTextFill());
    }

    @TestFx
    public void setTitleSide() {
        chart.setTitleSide(Side.RIGHT);
        assertEquals(Side.RIGHT, chart.getTitleSide());
    }

    private static class TestChart extends Chart {
        @Override
        public void updateAxisRange() {
        }

        @Override
        protected void axesChanged(ListChangeListener.Change<? extends Axis> change) {
        }

        @Override
        protected void redrawCanvas() {
        }

        public Label getTitlePaint() {
            return titleLabel;
        }
    }
}
