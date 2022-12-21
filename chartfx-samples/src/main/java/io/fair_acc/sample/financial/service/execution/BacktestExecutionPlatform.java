package io.fair_acc.sample.financial.service.execution;

import java.util.Date;

import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import io.fair_acc.sample.financial.dos.OHLCVItem;
import io.fair_acc.sample.financial.dos.Order;
import io.fair_acc.sample.financial.dos.Order.OrderStatus;
import io.fair_acc.sample.financial.dos.OrderExpression.OrderType;
import io.fair_acc.sample.financial.service.OhlcvChangeListener;
import io.fair_acc.sample.financial.service.execution.ExecutionResult.ExecutionResultEnum;
import io.fair_acc.sample.financial.service.order.ResolveOrderService;
import io.fair_acc.sample.financial.service.order.ResolvePositionService;

/**
 * Example of Simple Backtest Execution Platform with Market Order Implementation
 *
 * @author afischer
 */
public class BacktestExecutionPlatform extends AbstractExecutionPlatform implements OhlcvChangeListener {
    private OHLCVItem ohlcvItem;

    @Override
    protected ExecutionResult ensureRequiredOrderAttributes(Order order) {
        // ensure the service order id - not used
        return new ExecutionResult(order);
    }

    @Override
    protected ExecutionResult executeOrderCancellation(Order order) {
        // always ok
        ExecutionResult result = new ExecutionResult(order);

        if (OrderStatus.OPENED.equals(order.getStatus())) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setLastActivityTime(getActualTime());
            // backtest execution platform can remove the order from the container (not for real system)
            orders.removeOrder(order);
            // notify listeners about cancelled position
            fireOrderCancelled(order);

        } else {
            result.setResult(ExecutionResultEnum.ERROR);
            result.setErrorMessage("The order " + order.getInternalOrderId() + " is not possible to cancel. The order is not opened.");
        }

        return result;
    }

    @Override
    protected ExecutionResult executeOrder(Order order) {
        // MARKET order - direct resolving the order on actual bar
        // all next orders are solved in next tick
        // this behaviour is for backtests only - real platform can perform the order directly
        // backtest can process the prices of actual bar from history, real platform not
        if (OrderType.MARKET.equals(order.getOrderExpression().getOrderType())) {
            resolveOrder(order);
        }

        return new ExecutionResult(order); // OK status always
    }

    /**
     * Resolve order for actual OHLCV item
     * @param order Order domain object
     */
    protected void resolveOrder(Order order) {
        // try to filled the order according to ohlcv item
        ResolveOrderService.resolveOrder(getActualOhlcvItem(), null, order);
        // resolve the trading positions
        if (OrderStatus.FILLED.equals(order.getStatus())) {
            // remove opened orders
            orders.removeOpenedOrder(order);
            // resolves positions process
            ResolvePositionService.resolvePositions(order, positions);
            // notify listeners about filled position
            fireOrderFilled(order);
        }
    }

    //------------------------ data provider section --------------------------

    protected Date getActualTime() {
        return ohlcvItem.getTimeStamp();
    }

    protected OHLCVItem getActualOhlcvItem() {
        return ohlcvItem;
    }

    @Override
    public void tickEvent(IOhlcvItem ohlcvItem) {
        this.ohlcvItem = (OHLCVItem) ohlcvItem;
    }
}
