package de.gsi.financial.samples.service.execution;

import static de.gsi.financial.samples.service.StandardTradePlanAttributes.ORDERS;
import static de.gsi.financial.samples.service.StandardTradePlanAttributes.POSITIONS;

import java.util.LinkedHashSet;
import java.util.Set;

import de.gsi.financial.samples.dos.Order;
import de.gsi.financial.samples.dos.OrderContainer;
import de.gsi.financial.samples.dos.PositionContainer;
import de.gsi.financial.samples.service.execution.ExecutionResult.ExecutionResultEnum;
import de.gsi.dataset.spi.financial.api.attrs.AttributeModel;

/**
 * @author afischer
 */
public abstract class AbstractExecutionPlatform implements ExecutionPlatform {
    protected AttributeModel context;
    protected OrderContainer orders = null;
    protected PositionContainer positions = null;

    private final Set<ExecutionPlatformListener> listeners = new LinkedHashSet<>();

    public void setContext(AttributeModel context) {
        this.context = context;
    }

    @Override
    public void addExecutionPlatformListener(ExecutionPlatformListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeExecutionPlatformListener(ExecutionPlatformListener listener) {
        listeners.remove(listener);
    }

    protected void fireOrderFilled(Order order) {
        OrderEvent event = new OrderEvent(this, order);
        for (ExecutionPlatformListener listener : listeners) {
            listener.orderFilled(event);
        }
    }

    protected void fireOrderCancelled(Order order) {
        OrderEvent event = new OrderEvent(this, order);
        for (ExecutionPlatformListener listener : listeners) {
            listener.orderCancelled(event);
        }
    }

    @Override
    public ExecutionResult performOrder(Order order) {
        // 1. set required attributes of order
        ExecutionResult result = setRequiredOrderAttributes(order);
        if (ExecutionResultEnum.ERROR.equals(result.getResult())) {
            return result;
        }
        // 2. store order to the container
        storeOrder(order);
        // 3. process order
        return processOrder(order);
    }

    @Override
    public ExecutionResult cancelOrder(int orderId) {
        Order order = orders.getOrderById(orderId);
        if (order == null) {
            ExecutionResult result = new ExecutionResult(order);
            result.setResult(ExecutionResultEnum.ERROR);
            result.setErrorMessage("The order " + orderId + " doesn't exist.");

            return result;
        }
        return cancelOrder(order);
    }

    @Override
    public ExecutionResult cancelOrder(Order order) {
        return executeOrderCancellation(order);
    }

    protected ExecutionResult setRequiredOrderAttributes(Order order) {
        // gateway - ensures service position id, etc.
        return ensureRequiredOrderAttributes(order);
    }

    protected void storeOrder(Order order) {
        // ensure order container
        if (orders == null) {
            orders = context.getAttribute(ORDERS);
            if (orders == null) {
                orders = new OrderContainer();
                context.setAttribute(ORDERS, orders);
            }
            positions = context.getAttribute(POSITIONS);
            if (positions == null) {
                positions = new PositionContainer();
                context.setAttribute(POSITIONS, positions);
            }
        }
        orders.addOrder(order);
    }

    /**
     * Processing of order
     * @param order Order
     * @return result
     */
    protected ExecutionResult processOrder(Order order) {
        return executeOrder(order);
    }

    /**
     * Ensure the required attributes for order
     * @param order Order
     */
    protected abstract ExecutionResult ensureRequiredOrderAttributes(Order order);

    /**
     * Execute order by gateway
     * @param order Order
     * @return result
     */
    protected abstract ExecutionResult executeOrder(Order order);

    /**
     * Execute order cancellation by gateway
     * @param order Order
     * @return result
     */
    protected abstract ExecutionResult executeOrderCancellation(Order order);
}
