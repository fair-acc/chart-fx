package de.gsi.chart.samples.financial.service.execution;

import java.util.EventObject;

import de.gsi.chart.samples.financial.dos.Order;

public class OrderEvent extends EventObject {
    private static final long serialVersionUID = 3995883467037156877L;

    private final Order order;

    public OrderEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}
