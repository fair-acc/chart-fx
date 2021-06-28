package de.gsi.financial.samples.service.order;

import de.gsi.financial.samples.dos.OHLCVItem;
import de.gsi.financial.samples.dos.Order;
import de.gsi.financial.samples.dos.Order.OrderStatus;
import de.gsi.financial.samples.dos.OrderExpression;
import de.gsi.financial.samples.dos.OrderExpression.OrderBuySell;

/**
 * @author afischer
 */
public class ResolveOrderService {
    /**
     * Resolving the status of order by ohlcv item
     *
     * @param item  ohlcv
     * @param order domain object for resolving
     */
    public static void resolveOrder(OHLCVItem item, Order order) {
        resolveOrder(item, null, order);
    }

    /**
     * Resolving the status of order by ohlcv item
     *
     * @param item      ohlcv
     * @param dailyItem item ohlcv
     * @param order     the order for execution by market
     */
    public static void resolveOrder(OHLCVItem item, OHLCVItem dailyItem, Order order) {
        OrderExpression orderExpression = order.getOrderExpression();
        switch (orderExpression.getOrderType()) {
        case MARKET:
            switch (orderExpression.getMarketOnPrice()) {
            case OPEN_PRICE:
                order.setAverageFillPrice(item.getOpen());
                break;
            case CLOSE_PRICE:
                order.setAverageFillPrice(item.getClose());
                break;
            case TEST_PRICE:
                double price = order.getOrderExpression().getPrice();
                if (isPriceIncluded(item, price)) {
                    order.setAverageFillPrice(price);
                } else { // not included, use close price
                    order.setAverageFillPrice(item.getClose());
                }
                break;
            }
            fillOrderByOhlcvItem(order, item);
            break;

        case LIMIT:
        case MIT:
            if (OrderBuySell.BUY.equals(order.getOrderExpression().getBuySell())) {
                if (item.getLow() <= order.getOrderExpression().getPrice()) {
                    if (item.getOpen() <= order.getOrderExpression().getPrice()) {
                        order.setAverageFillPrice(item.getOpen());
                    } else {
                        order.setAverageFillPrice(order.getOrderExpression().getPrice());
                    }
                    fillOrderByOhlcvItem(order, item);
                }
            } else { // SELL
                if (item.getHigh() >= order.getOrderExpression().getPrice()) {
                    if (item.getOpen() >= order.getOrderExpression().getPrice()) {
                        order.setAverageFillPrice(item.getOpen());
                    } else {
                        order.setAverageFillPrice(order.getOrderExpression().getPrice());
                    }
                    fillOrderByOhlcvItem(order, item);
                }
            }
            break;

        case STOP:
            if (OrderBuySell.BUY.equals(order.getOrderExpression().getBuySell())) {
                if (item.getHigh() >= order.getOrderExpression().getPrice()) {
                    if (item.getOpen() >= order.getOrderExpression().getPrice()) {
                        order.setAverageFillPrice(item.getOpen());
                    } else {
                        order.setAverageFillPrice(order.getOrderExpression().getPrice());
                    }
                    fillOrderByOhlcvItem(order, item);
                }
            } else { // SELL
                if (item.getLow() <= order.getOrderExpression().getPrice()) {
                    if (item.getOpen() <= order.getOrderExpression().getPrice()) {
                        order.setAverageFillPrice(item.getOpen());
                    } else {
                        order.setAverageFillPrice(order.getOrderExpression().getPrice());
                    }
                    fillOrderByOhlcvItem(order, item);
                }
            }
            break;

        case STOP_LIMIT:
            if (OrderBuySell.BUY.equals(order.getOrderExpression().getBuySell())) {
                if (item.getHigh() >= order.getOrderExpression().getPrice()) {
                    if (item.getOpen() <= order.getOrderExpression().getPrice()) {
                        order.setAverageFillPrice(order.getOrderExpression().getPrice());
                        fillOrderByOhlcvItem(order, item);
                    } else {
                        if (item.getOpen() > order.getOrderExpression().getPrice2() && item.getLow() <= order.getOrderExpression().getPrice2()) {
                            order.setAverageFillPrice(order.getOrderExpression().getPrice2());
                            fillOrderByOhlcvItem(order, item);
                        }
                        if (item.getOpen() <= order.getOrderExpression().getPrice2()) {
                            order.setAverageFillPrice(item.getOpen());
                            fillOrderByOhlcvItem(order, item);
                        }
                    }
                }
            } else { // SELL
                if (item.getLow() <= order.getOrderExpression().getPrice()) {
                    if (item.getOpen() >= order.getOrderExpression().getPrice()) {
                        order.setAverageFillPrice(order.getOrderExpression().getPrice());
                        fillOrderByOhlcvItem(order, item);
                    } else {
                        if (item.getOpen() < order.getOrderExpression().getPrice2() && item.getHigh() >= order.getOrderExpression().getPrice2()) {
                            order.setAverageFillPrice(order.getOrderExpression().getPrice2());
                            fillOrderByOhlcvItem(order, item);
                        }
                        if (item.getOpen() >= order.getOrderExpression().getPrice2()) {
                            order.setAverageFillPrice(item.getOpen());
                            fillOrderByOhlcvItem(order, item);
                        }
                    }
                }
            }
            break;

        case MOO:
            order.setAverageFillPrice(dailyItem != null ? dailyItem.getOpen() : item.getOpen());
            fillOrderByOhlcvItem(order, item);
            break;

        case MOC:
            order.setAverageFillPrice(dailyItem != null ? dailyItem.getClose() : item.getClose());
            fillOrderByOhlcvItem(order, item);
            break;
        }
    }

    /**
     * Checks if the input price is included in the inserted OHLCV bar
     *
     * @param item  the ohlcv bar
     * @param price the price for checking
     * @return true = the price is included in the bar
     */
    public static boolean isPriceIncluded(OHLCVItem item, double price) {
        return price <= item.getHigh() && price >= item.getLow();
    }

    private static void fillOrderByOhlcvItem(Order order, OHLCVItem item) {
        order.setStatus(OrderStatus.FILLED);
        order.setLastActivityTime(item.getTimeStamp());
        order.setOhlcvItem(item);
    }

    private ResolveOrderService() {
    }
}
