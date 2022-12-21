package io.fair_acc.sample.financial.dos;

import java.util.Date;

/**
 * @author afischer
 */
public class Order {
    public enum OrderStatus {
        OPENED,
        FILLED,
        CANCELLED,
        ERROR
    }

    private final int internalOrderId;
    private final long orderIndex;
    private final String userName;
    private final Date entryTime;
    private final String symbol;
    private final OrderExpression orderExpression;
    private final String accountId;

    private String serviceOrderId;
    private Date lastActivityTime;
    private OrderStatus status = OrderStatus.OPENED;
    private double averageFillPrice; // filled price by exchange
    private OHLCVItem ohlcvItem;

    private boolean isExitOrder = false;
    private Position entryOfPosition;
    private Position exitOfPosition;

    /**
     * Create exchange order
     *
     * @param internalOrderId String
     * @param orderIndex      Long
     * @param userName        String
     * @param entryTime       Date
     * @param symbol          String
     * @param orderExpression OrderExpression
     * @param accountId       String
     */
    public Order(int internalOrderId, Long orderIndex, String userName, Date entryTime, String symbol,
            OrderExpression orderExpression, String accountId) {
        this.internalOrderId = internalOrderId;
        this.orderIndex = orderIndex == null ? entryTime.getTime() : orderIndex;
        this.userName = userName;
        this.entryTime = entryTime;
        this.symbol = symbol;
        this.orderExpression = orderExpression;
        this.accountId = accountId;
        setLastActivityTime(entryTime);
    }

    public void setLastActivityTime(Date lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setAverageFillPrice(double averageFillPrice) {
        this.averageFillPrice = averageFillPrice;
    }

    public int getInternalOrderId() {
        return internalOrderId;
    }

    public long getOrderIndex() {
        return orderIndex;
    }

    public String getUserName() {
        return userName;
    }

    public void setServiceOrderId(String serviceOrderId) {
        this.serviceOrderId = serviceOrderId;
    }

    public String getServiceOrderId() {
        return serviceOrderId;
    }

    public Position getEntryOfPosition() {
        return entryOfPosition;
    }

    public void setEntryOfPosition(Position entryOfPosition) {
        this.entryOfPosition = entryOfPosition;
    }

    public Position getExitOfPosition() {
        return exitOfPosition;
    }

    public void setExitOfPosition(Position exitOfPosition) {
        this.exitOfPosition = exitOfPosition;
    }

    public void setExitOrder(boolean isExitOrder) {
        this.isExitOrder = isExitOrder;
    }

    // order was used for closing of position
    public boolean isExitOrder() {
        return isExitOrder;
    }

    public Date getEntryTime() {
        return entryTime;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderExpression getOrderExpression() {
        return orderExpression;
    }

    public String getAccountId() {
        return accountId;
    }

    public Date getLastActivityTime() {
        return lastActivityTime;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public double getAverageFillPrice() {
        return averageFillPrice;
    }

    public OHLCVItem getOhlcvItem() {
        return ohlcvItem;
    }

    public void setOhlcvItem(OHLCVItem ohlcvItem) {
        this.ohlcvItem = ohlcvItem;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + internalOrderId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Order other = (Order) obj;
        return internalOrderId == other.internalOrderId;
    }

    @Override
    public String toString() {
        return "Order [internalOrderId=" + internalOrderId + ", orderIndex=" + orderIndex + ", userName=" + userName + ", serviceOrderId=" + serviceOrderId + ", entryTime=" + entryTime + ", symbol=" + symbol
      + ", orderExpression=" + orderExpression + ", accountId=" + accountId + ", lastActivityTime=" + lastActivityTime + ", status=" + status
      + ", averageFillPrice=" + averageFillPrice + "]";
    }
}
