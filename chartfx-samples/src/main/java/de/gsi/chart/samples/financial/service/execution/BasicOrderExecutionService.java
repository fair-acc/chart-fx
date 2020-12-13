package de.gsi.chart.samples.financial.service.execution;

import de.gsi.chart.samples.financial.dos.*;
import de.gsi.chart.samples.financial.service.StandardTradePlanAttributes;
import de.gsi.chart.samples.financial.service.order.InternalOrderIdGenerator;
import de.gsi.dataset.spi.financial.api.attrs.AttributeModel;

import java.util.Date;
import java.util.Set;

public class BasicOrderExecutionService {

    private AttributeModel context;
    private OrderContainer orderContainer;
    private PositionContainer positionContainer;
    private String assetName;
    private String accountId;
    private ExecutionPlatform executionPlatform;

    public void afterPropertiesSet() throws Exception {
        orderContainer = context.getAttribute(StandardTradePlanAttributes.ORDERS);
        positionContainer = context.getAttribute(StandardTradePlanAttributes.POSITIONS);
        assetName = context.getAttribute(StandardTradePlanAttributes.ASSET_NAME);
        accountId = context.getAttribute(StandardTradePlanAttributes.ACCOUNT_ID);
        if (orderContainer == null || positionContainer == null) {
            throw new IllegalArgumentException("The orders or positions containers are not prepared for common order execution service!");
        }
    }

    public void setContext(AttributeModel context) {
        this.context = context;
    }

    public Order createOrder(String name, Date entryTime, String symbol, OrderExpression orderExpression) {
        Integer orderId = InternalOrderIdGenerator.generateId();
        return new Order(orderId, name, entryTime, symbol, orderExpression, accountId);
    }

    public ExecutionResult performOrder(String name, Date entryTime, String symbol, OrderExpression orderExpression) {
        return performOrder(createOrder(name, entryTime, symbol, orderExpression));
    }

    public ExecutionResult performOrder(Date entryTime, String symbol, OrderExpression orderExpression) {
        return performOrder(createOrder(null, entryTime, symbol, orderExpression));
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

    public void flatPositions() {
        Set<Position> openedPositions = positionContainer.getFastOpenedPositionByMarketSymbol(assetName);
        for (Position position : openedPositions) {
            if (position.getPositionType() == 1) { // Long
                performOrder(position.getEntryTime(), position.getSymbol(), OrderExpression.sellMarket(position.getPositionQuantity()));

            } else { // Short
                performOrder(position.getEntryTime(), position.getSymbol(), OrderExpression.buyMarket(position.getPositionQuantity()));
            }
        }
    }

    //--------------------------- injections -------------------------------

    public void setExecutionPlatform(ExecutionPlatform executionPlatform) {
        this.executionPlatform = executionPlatform;
    }

}
