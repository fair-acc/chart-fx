package de.gsi.chart.samples.financial.service.execution;

import de.gsi.chart.samples.financial.dos.Order;

import java.util.EventObject;

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
