/**
 * LGPL-3.0, 2020/21, GSI-CS-CO/Chart-fx, BTA HF OpenSource Java-FX Branch, Financial Charts
 */
package de.gsi.chart.samples.financial.dos;

import de.gsi.chart.samples.financial.dos.Order.OrderStatus;

import java.util.*;

/**
 * @author afischer
 */
public class OrderContainer {
    private final Map<Integer, Order> orders = new LinkedHashMap<Integer, Order>();
    private final HashSet<Order> openedOrders = new LinkedHashSet<Order>();

    public void addOrder(Order order) {
        openedOrders.add(order);
        orders.put(order.getInternalOrderId(), order);
    }

    public Order removeOrder(Order order) {
        openedOrders.remove(order);
        return orders.remove(order.getInternalOrderId());
    }

    public boolean removeOpenedOrder(Order order) {
        return openedOrders.remove(order);
    }

    public Collection<Order> getOrders() {
        return orders.values();
    }

    @SuppressWarnings("unchecked")
    public Collection<Order> getOpenedOrders() {
        return (LinkedHashSet<Order>) openedOrders.clone();
    }

    public Set<Order> getOpenedAndFilledOrders() {
        Set<Order> openOrders = new LinkedHashSet<Order>();
        for (Order order : getOrders()) {
            if (OrderStatus.OPENED == order.getStatus() || OrderStatus.FILLED == order.getStatus()) {
                openOrders.add(order);
            }
        }
        return openOrders;
    }

    public Set<Order> getOpenedAndFilledOrdersByMarket(String symbol) {
        Set<Order> marketOrders = new LinkedHashSet<Order>();
        if (symbol != null) {
            for (Order order : getOpenedAndFilledOrders()) {
                if (order.getSymbol().equals(symbol)) {
                    marketOrders.add(order);
                }
            }
        }
        return marketOrders;
    }

    public Order getOrderById(int internalOrderId) {
        return orders.get(internalOrderId);
    }

    public void clear() {
        orders.clear();
    }

    public boolean contains(Order order) {
        return orders.containsKey(order.getInternalOrderId());
    }

    @Override
    public String toString() {
        return "OrderContainer [orders=" + orders + "]";
    }
}
