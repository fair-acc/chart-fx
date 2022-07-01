package io.fair_acc.financial.samples.service;

import io.fair_acc.dataset.spi.financial.api.attrs.AttributeKey;
import io.fair_acc.financial.samples.dos.OrderContainer;
import io.fair_acc.financial.samples.dos.PositionContainer;

public class StandardTradePlanAttributes {
    /**
     * The performed trading orders
     */
    public static final AttributeKey<OrderContainer> ORDERS = AttributeKey.create(OrderContainer.class, "ORDERS");

    /**
     * The opened/closed trading positions
     */
    public static final AttributeKey<PositionContainer> POSITIONS = AttributeKey.create(PositionContainer.class, "POSITIONS");

    /** The trading asset identification - this symbol will be traded by execution platform
     * The more providers are supported, this one is main for trading. */
    public static final AttributeKey<String> ASSET_NAME = AttributeKey.create(String.class, "ASSET_NAME");

    /** The account ID  */
    public static final AttributeKey<String> ACCOUNT_ID = AttributeKey.create(String.class, "ACCOUNT_ID");

    private StandardTradePlanAttributes() {
    }
}
