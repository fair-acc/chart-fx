package io.fair_acc.chartfx;

import static org.junit.jupiter.api.Assertions.*;

import io.fair_acc.chartfx.utils.FXUtils;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import io.fair_acc.chartfx.ui.utils.TestFx;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
@SuppressWarnings({ "PMD.UncommentedEmptyMethodBody" })
public class ChartTest {
    private TestChart chart;

    @BeforeEach
    public void setup() throws Exception {
        chart = FXUtils.runAndWait(TestChart::new);
    }

    @TestFx
    public void setTitlePaint() {
        chart.getTitleLabel().setTextFill(Color.BLUE);
        assertEquals(Color.BLUE, chart.getTitleLabel().getTextFill());
    }

    @TestFx
    public void setTitleSide() {
        chart.getTitleLabel().setSide(Side.RIGHT);
        assertEquals(Side.RIGHT, chart.getTitleLabel().getSide());
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

    }
}
