package io.fair_acc.sample.financial;

import static io.fair_acc.sample.financial.service.StandardTradePlanAttributes.POSITIONS;

import java.text.ParseException;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.renderer.spi.financial.CandleStickRenderer;
import io.fair_acc.chartfx.renderer.spi.financial.PositionFinancialRendererPaintAfterEP;
import io.fair_acc.sample.financial.dos.Position;
import io.fair_acc.sample.financial.dos.Position.PositionStatus;
import io.fair_acc.sample.financial.dos.PositionContainer;
import io.fair_acc.sample.financial.service.CalendarUtils;
import io.fair_acc.sample.financial.service.order.PositionFinancialDataSet;
import io.fair_acc.dataset.spi.DefaultDataSet;
import io.fair_acc.dataset.spi.financial.OhlcvDataSet;
import io.fair_acc.dataset.spi.financial.api.attrs.AttributeModel;

/**
 * Financial Position Sample
 *
 * @author afischer
 */
public class FinancialPositionSample extends AbstractBasicFinancialApplication {
    protected void prepareRenderers(XYChart chart, OhlcvDataSet ohlcvDataSet, DefaultDataSet indiSet) {
        // define context
        AttributeModel context = new AttributeModel()
                                         .setAttribute(POSITIONS, new PositionContainer());

        // direct define closed positions
        addClosedPosition(context, 0, "2020/09/01 16:00", 3516.75, "2020/09/04 16:00", 3407.25, 1, 1, resource);
        addClosedPosition(context, 1, "2020/09/10 16:00", 3330.00, "2020/09/18 16:00", 3316.25, 1, -1, resource);
        addClosedPosition(context, 2, "2020/09/28 16:00", 3291.00, "2020/10/05 16:00", 3393.00, 1, 1, resource);
        addClosedPosition(context, 3, "2020/09/28 16:00", 3291.00, "2020/10/19 16:00", 3422.75, 1, 1, resource);

        // position/order visualization
        PositionFinancialDataSet positionFinancialDataSet = new PositionFinancialDataSet(
                resource, ohlcvDataSet, context);

        // create and apply renderers
        CandleStickRenderer candleStickRenderer = new CandleStickRenderer();
        candleStickRenderer.getDatasets().addAll(ohlcvDataSet);
        candleStickRenderer.addPaintAfterEp(new PositionFinancialRendererPaintAfterEP(
                positionFinancialDataSet, chart));

        ErrorDataSetRenderer avgRenderer = new ErrorDataSetRenderer();
        avgRenderer.setDrawMarker(false);
        avgRenderer.setErrorStyle(ErrorStyle.NONE);
        avgRenderer.getDatasets().addAll(indiSet);

        chart.getRenderers().clear();
        chart.getRenderers().add(candleStickRenderer);
        chart.getRenderers().add(avgRenderer);
    }

    // helpers methods --------------------------------------------------------
    private void addClosedPosition(AttributeModel context, int id, String entryTimePattern, double entryPrice, String exitTimePattern, double exitPrice,
            int quantity, int longShort, String symbol) {
        try {
            context.getRequiredAttribute(POSITIONS).addPosition(getClosedPosition(id, entryTimePattern, entryPrice, exitTimePattern, exitPrice, quantity, longShort, symbol));
        } catch (ParseException e) {
            throw new IllegalArgumentException("The time pattern is not correctly configured! e=" + e.getMessage(), e);
        }
    }

    // Create opened position by basic attributes
    private Position getClosedPosition(int id, String entryTimePattern, double entryPrice, String exitTimePattern, double exitPrice,
            int quantity, int longShort, String symbol) throws ParseException {
        return closePosition(getOpenedPosition(id, entryTimePattern, entryPrice, quantity, longShort, symbol), exitTimePattern, exitPrice);
    }

    // Create opened position by basic attributes
    private Position getOpenedPosition(int id, String entryTimePattern, double entryPrice,
            int quantity, int longShort, String symbol) throws ParseException {
        return new Position(id, null, "strategy1",
                CalendarUtils.createByDateTime(entryTimePattern).getTime(), longShort, symbol,
                "account1", entryPrice, quantity);
    }

    // Close the position, new domain object is created.
    private Position closePosition(Position position, String exitTimePattern, double exitPrice) throws ParseException {
        Position positionClosed = position.copyDeep();
        positionClosed.setExitTime(CalendarUtils.createByDateTime(exitTimePattern).getTime());
        positionClosed.setPositionExitIndex(positionClosed.getExitTime().getTime()); // indices are driven by time for this example
        positionClosed.setExitPrice(exitPrice);
        positionClosed.setPositionStatus(PositionStatus.CLOSED);

        return positionClosed;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        launch(args);
    }
}
