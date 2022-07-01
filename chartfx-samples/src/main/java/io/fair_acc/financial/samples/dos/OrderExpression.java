package io.fair_acc.financial.samples.dos;

import java.util.Date;

public class OrderExpression {
    public enum OrderBuySell {
        BUY,
        SELL
    }

    public enum OrderType {
        MARKET,
        LIMIT,
        STOP,
        MIT,
        STOP_LIMIT,
        MOC,
        MOO,
        OCO_LIMIT_STOP,
        OCO_BUY_STOP_SELL_STOP
    }

    public enum TimeInForce {
        DAY,
        GTC,
        GTDT,
        NEXT_BAR
    }

    /**
     * MARKET PRICE ENTRY
     * OPEN_PRICE - Enter on open price of actual bar
     * CLOSE_PRICE - Enter on close price of actual bar
     * TEST_PRICE - Enter on specific price of actual bar (for backtest only!),
     * the price has to be specified and included in actual bar, if the price is not included, the close price is taken.
     */
    public enum MarketOnPrice {
        OPEN_PRICE,
        CLOSE_PRICE,
        TEST_PRICE
    }

    public static final TIF TIF_NEXT_BAR = new TIF(TimeInForce.NEXT_BAR);

    private final OrderBuySell buySell;
    private final OrderType orderType;
    private final int orderQuantity;
    private Double price; // LIMIT, STOP price
    private Double price2; // STOP_LIMIT second price
    private MarketOnPrice marketOnPrice = MarketOnPrice.CLOSE_PRICE;
    private TIF tif = new TIF();

    public OrderExpression(OrderBuySell buySell, OrderType orderType, int orderQuantity) {
        this.buySell = buySell;
        this.orderType = orderType;
        this.orderQuantity = orderQuantity;
    }

    public OrderExpression(OrderBuySell buySell, OrderType orderType, int orderQuantity, Double price) {
        this.buySell = buySell;
        this.orderType = orderType;
        this.orderQuantity = orderQuantity;
        this.price = price;
    }

    public OrderExpression(OrderBuySell buySell, OrderType orderType, int orderQuantity, Double price, TIF tif) {
        this.buySell = buySell;
        this.orderType = orderType;
        this.orderQuantity = orderQuantity;
        this.price = price;
        this.tif = tif;
    }

    public OrderExpression(OrderBuySell buySell, OrderType orderType, int orderQuantity, Double price, TIF tif, MarketOnPrice marketOnPrice) {
        this.buySell = buySell;
        this.orderType = orderType;
        this.orderQuantity = orderQuantity;
        this.price = price;
        if (tif != null) {
            this.tif = tif;
        }
        this.marketOnPrice = marketOnPrice;
    }

    public OrderExpression(OrderBuySell buySell, OrderType orderType, int orderQuantity, Double price, Double price2) {
        this.buySell = buySell;
        this.orderType = orderType;
        this.orderQuantity = orderQuantity;
        this.price = price;
        this.price2 = price2;
    }

    public OrderExpression(OrderBuySell buySell, OrderType orderType, int orderQuantity, Double price, Double price2, TIF tif) {
        this.buySell = buySell;
        this.orderType = orderType;
        this.orderQuantity = orderQuantity;
        this.price = price;
        this.price2 = price2;
        this.tif = tif;
    }

    /**
     * BUY MARKET (on close price)
     */
    public static OrderExpression buyMarket(int quantity) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.MARKET, quantity);
    }

    /**
     * BUY MARKET (on test price)
     */
    public static OrderExpression buyMarketOnTestPrice(int quantity, double price) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.MARKET, quantity, price, new TIF(), MarketOnPrice.TEST_PRICE);
    }

    /**
     * BUY MARKET (on close price) + TIF
     */
    public static OrderExpression buyMarket(int quantity, TIF tif) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.MARKET, quantity, null, tif, MarketOnPrice.CLOSE_PRICE);
    }

    /**
     * SELL MARKET (on close price)
     */
    public static OrderExpression sellMarket(int quantity) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.MARKET, quantity);
    }

    /**
     * SELL MARKET (on test price)
     */
    public static OrderExpression sellMarketOnTestPrice(int quantity, double price) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.MARKET, quantity, price, new TIF(), MarketOnPrice.TEST_PRICE);
    }

    /**
     * SELL MARKET (on close price) + TIF
     */
    public static OrderExpression sellMarket(int quantity, TIF tif) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.MARKET, quantity, null, tif, MarketOnPrice.CLOSE_PRICE);
    }

    /**
     * BUY MARKET on open price of actual bar
     */
    public static OrderExpression buyMarketOnOpenPrice(int quantity) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.MARKET, quantity, null, null, MarketOnPrice.OPEN_PRICE);
    }

    /**
     * BUY MARKET on open price of actual bar
     */
    public static OrderExpression buyMarketOnOpenPrice(int quantity, TIF tif) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.MARKET, quantity, null, tif, MarketOnPrice.OPEN_PRICE);
    }

    /**
     * SELL MARKET on open price of actual bar
     */
    public static OrderExpression sellMarketOnOpenPrice(int quantity) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.MARKET, quantity, null, null, MarketOnPrice.OPEN_PRICE);
    }

    /**
     * SELL MARKET on open price of actual bar + TIF
     */
    public static OrderExpression sellMarketOnOpenPrice(int quantity, TIF tif) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.MARKET, quantity, null, tif, MarketOnPrice.OPEN_PRICE);
    }

    /**
     * BUY MARKET on next bar
     */
    public static OrderExpression buyMarketNextBar(int quantity) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.MARKET, quantity, null, TIF_NEXT_BAR);
    }

    /**
     * BUY MARKET on next bar on open price
     */
    public static OrderExpression buyMarketNextBarOnOpenPrice(int quantity) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.MARKET, quantity, null, TIF_NEXT_BAR, MarketOnPrice.OPEN_PRICE);
    }

    /**
     * SELL MARKET on next bar
     */
    public static OrderExpression sellMarketNextBar(int quantity) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.MARKET, quantity, null, TIF_NEXT_BAR);
    }

    /**
     * BUY MOC
     */
    public static OrderExpression buyMarketOnClose(int quantity) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.MOC, quantity);
    }

    /**
     * SELL MOC
     */
    public static OrderExpression sellMarketOnClose(int quantity) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.MOC, quantity);
    }

    /**
     * BUY MOO
     */
    public static OrderExpression buyMarketOnOpen(int quantity) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.MOO, quantity);
    }

    /**
     * SELL MOO
     */
    public static OrderExpression sellMarketOnOpen(int quantity) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.MOO, quantity);
    }

    /**
     * BUY MIT
     */
    public static OrderExpression buyMarketInTouch(double price, int quantity) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.MIT, quantity, price);
    }

    /**
     * SELL MIT
     */
    public static OrderExpression sellMarketInTouch(double price, int quantity) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.MIT, quantity, price);
    }

    /**
     * BUY MIT + TIF
     */
    public static OrderExpression buyMarketInTouch(double price, int quantity, TIF tif) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.MIT, quantity, price, tif);
    }

    /**
     * SELL MIT + TIF
     */
    public static OrderExpression sellMarketInTouch(double price, int quantity, TIF tif) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.MIT, quantity, price, tif);
    }

    /**
     * BUY LIMIT
     */
    public static OrderExpression buyLimit(double price, int quantity) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.LIMIT, quantity, price);
    }

    /**
     * SELL LIMIT
     */
    public static OrderExpression sellLimit(double price, int quantity) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.LIMIT, quantity, price);
    }

    /**
     * BUY LIMIT + TIF
     */
    public static OrderExpression buyLimit(double price, int quantity, TIF tif) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.LIMIT, quantity, price, tif);
    }

    /**
     * SELL LIMIT + TIF
     */
    public static OrderExpression sellLimit(double price, int quantity, TIF tif) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.LIMIT, quantity, price, tif);
    }

    /**
     * BUY STOP
     */
    public static OrderExpression buyStop(double price, int quantity) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.STOP, quantity, price);
    }

    /**
     * SELL STOP
     */
    public static OrderExpression sellStop(double price, int quantity) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.STOP, quantity, price);
    }

    /**
     * BUY STOP + TIF
     */
    public static OrderExpression buyStop(double price, int quantity, TIF tif) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.STOP, quantity, price, tif);
    }

    /**
     * SELL STOP + TIF
     */
    public static OrderExpression sellStop(double price, int quantity, TIF tif) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.STOP, quantity, price, tif);
    }

    /**
     * BUY STOP LIMIT
     *
     * @param price  double for STOP order
     * @param price2 double for LIMIT order
     */
    public static OrderExpression buyStopLimit(double price, double price2, int quantity) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.STOP_LIMIT, quantity, price, price2);
    }

    /**
     * SELL STOP LIMIT
     *
     * @param price  double for STOP order
     * @param price2 double for LIMIT order
     */
    public static OrderExpression sellStopLimit(double price, double price2, int quantity) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.STOP_LIMIT, quantity, price, price2);
    }

    /**
     * BUY STOP LIMIT + TIF
     *
     * @param price  double for STOP order
     * @param price2 double for LIMIT order
     */
    public static OrderExpression buyStopLimit(double price, double price2, int quantity, TIF tif) {
        return new OrderExpression(OrderBuySell.BUY, OrderType.STOP_LIMIT, quantity, price, price2, tif);
    }

    /**
     * SELL STOP LIMIT + TIF
     *
     * @param price  double for STOP order
     * @param price2 double for LIMIT order
     */
    public static OrderExpression sellStopLimit(double price, double price2, int quantity, TIF tif) {
        return new OrderExpression(OrderBuySell.SELL, OrderType.STOP_LIMIT, quantity, price, price2, tif);
    }

    public OrderBuySell getBuySell() {
        return buySell;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public int getOrderQuantity() {
        return orderQuantity;
    }

    public Double getPrice() {
        return price;
    }

    public Double getPrice2() {
        return price2;
    }

    public MarketOnPrice getMarketOnPrice() {
        return marketOnPrice;
    }

    public TIF getTif() {
        return tif;
    }

    @Override
    public String toString() {
        return buySell + " " + orderType + (price != null ? " " + price : "") + (price2 != null ? " " + price2 : "") + " Q" + orderQuantity + ", " + tif;
    }

    public static class TIF {
        private TimeInForce tif = TimeInForce.DAY;
        private Date goodTillDateTimeFrom;
        private Date goodTillDateTimeTo;

        public TIF() {
        }

        public TIF(TimeInForce tif) {
            this.tif = tif;
        }

        public TIF(Date goodTillDateTimeFrom, Date goodTillDateTimeTo) {
            this.tif = TimeInForce.GTDT;
            this.goodTillDateTimeFrom = goodTillDateTimeFrom;
            this.goodTillDateTimeTo = goodTillDateTimeTo;
        }

        public TIF(Date goodTillDateTimeFrom) {
            this.tif = TimeInForce.GTDT;
            this.goodTillDateTimeFrom = goodTillDateTimeFrom;
            this.goodTillDateTimeTo = null;
        }

        public TimeInForce getTif() {
            return tif;
        }

        public Date getGoodTillDateTimeFrom() {
            return goodTillDateTimeFrom;
        }

        public Date getGoodTillDateTimeTo() {
            return goodTillDateTimeTo;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((goodTillDateTimeFrom == null) ? 0 : goodTillDateTimeFrom.hashCode());
            result = prime * result + ((goodTillDateTimeTo == null) ? 0 : goodTillDateTimeTo.hashCode());
            result = prime * result + ((tif == null) ? 0 : tif.hashCode());
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
            TIF other = (TIF) obj;
            if (goodTillDateTimeFrom == null) {
                if (other.goodTillDateTimeFrom != null)
                    return false;
            } else if (!goodTillDateTimeFrom.equals(other.goodTillDateTimeFrom))
                return false;
            if (goodTillDateTimeTo == null) {
                if (other.goodTillDateTimeTo != null)
                    return false;
            } else if (!goodTillDateTimeTo.equals(other.goodTillDateTimeTo))
                return false;
            if (tif != other.tif)
                return false;
            return true;
        }

        @Override
        public String toString() {
            if (TimeInForce.GTDT.equals(tif)) {
                return "TIF [TIF=" + tif + ", GTD-From=" + goodTillDateTimeFrom + ", GTD-To=" + goodTillDateTimeTo + "]";
            }
            return tif.toString();
        }
    }
}
