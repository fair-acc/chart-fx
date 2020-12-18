/**
 * LGPL-3.0, 2020/21, GSI-CS-CO/Chart-fx, BTA HF OpenSource Java-FX Branch, Financial Charts
 */
package de.gsi.chart.samples.financial.service.execution;

import de.gsi.chart.samples.financial.dos.*;
import de.gsi.chart.samples.financial.service.StandardTradePlanAttributes;
import de.gsi.chart.samples.financial.service.order.InternalOrderIdGenerator;
import de.gsi.dataset.spi.financial.api.attrs.AttributeModel;

import java.util.Date;
import java.util.Set;

/**
 * Basic example of order execution processing
 *
 * @author afischer
 */
public class BasicOrderExecutionService {

    private AttributeModel context;
    private OrderContainer orderContainer;
    private PositionContainer positionContainer;
    private String accountId;
    private ExecutionPlatform executionPlatform;

    public BasicOrderExecutionService(AttributeModel context, ExecutionPlatform executionPlatform) {
        this.context = context;
        this.executionPlatform = executionPlatform;
        afterPropertiesSet();
    }

    private void afterPropertiesSet() {
        orderContainer = context.getRequiredAttribute(StandardTradePlanAttributes.ORDERS);
        positionContainer = context.getRequiredAttribute(StandardTradePlanAttributes.POSITIONS);
        accountId = context.getAttribute(StandardTradePlanAttributes.ACCOUNT_ID, "account");
    }

    public Order createOrder(String name, Date entryTime, String asset, OrderExpression orderExpression) {
        Integer orderId = InternalOrderIdGenerator.generateId();
        return new Order(orderId, name, entryTime, asset, orderExpression, accountId);
    }

    public ExecutionResult performOrder(String name, Date entryTime, String asset, OrderExpression orderExpression) {
        return performOrder(createOrder(name, entryTime, asset, orderExpression));
    }

    public ExecutionResult performOrder(Date entryTime, String asset, OrderExpression orderExpression) {
        return performOrder(createOrder(null, entryTime, asset, orderExpression));
    }

    public ExecutionResult performOrder(Order order) {
        return executionPlatform.performOrder(order);
    }

    public ExecutionResult cancelOrder(int orderId) {
        return executionPlatform.cancelOrder(orderId);
    }

    public ExecutionResult cancelOrder(Order order) {
        return executionPlatform.cancelOrder(order);
    }

    public void flatPositions(String asset) {
        Set<Position> openedPositions = positionContainer.getFastOpenedPositionByMarketSymbol(asset);
        for (Position position : openedPositions) {
            if (position.getPositionType() == 1) { // Long
                performOrder(position.getEntryTime(), position.getSymbol(), OrderExpression.sellMarket(position.getPositionQuantity()));

            } else { // Short
                performOrder(position.getEntryTime(), position.getSymbol(), OrderExpression.buyMarket(position.getPositionQuantity()));
            }
        }
    }
}
