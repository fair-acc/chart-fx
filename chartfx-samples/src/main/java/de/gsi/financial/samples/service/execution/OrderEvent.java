package de.gsi.financial.samples.service.execution;

import java.util.EventObject;

import de.gsi.financial.samples.dos.Order;

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
