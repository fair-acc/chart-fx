/**
 * LGPL-3.0, 2020/21, GSI-CS-CO/Chart-fx, BTA HF OpenSource Java-FX Branch, Financial Charts
 */
package de.gsi.chart.samples.financial.service.order;

import static de.gsi.chart.samples.financial.service.StandardTradePlanAttributes.POSITIONS;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.renderer.spi.financial.PositionFinancialRendererPaintAfterEP.PositionRendered;
import de.gsi.chart.renderer.spi.financial.PositionFinancialRendererPaintAfterEP.PositionRenderedAware;
import de.gsi.chart.samples.financial.dos.Order;
import de.gsi.chart.samples.financial.dos.Position;
import de.gsi.chart.samples.financial.dos.Position.PositionStatus;
import de.gsi.chart.samples.financial.dos.PositionContainer;
import de.gsi.chart.samples.financial.service.execution.ExecutionPlatformListener;
import de.gsi.chart.samples.financial.service.execution.OrderEvent;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.spi.AbstractDataSet;
import de.gsi.dataset.spi.financial.OhlcvDataSet;
import de.gsi.dataset.spi.financial.api.attrs.AttributeModel;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;

/**
 *<p>
 * Example of Trading Position DataSet. Depended on the Trading Platform Position / Order Domain Objects.
 *<p>
 * The example includes:
 * <ul>
 * <li> merging positions for same entry or exit time, and
 * <li> mapping position domain object to renderer required domain object PositionRendered, and
 * <li> full example of the position listener mechanism.
 * </ul>
 *
 * @author afischer
 */
public class PositionFinancialDataSet extends AbstractDataSet<PositionFinancialDataSet> implements ExecutionPlatformListener, PositionRenderedAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(PositionFinancialDataSet.class);

    private Map<Long, PositionRendered> renderedPositionsBased;
    private List<PositionRendered> renderedPositionsList;
    private String instrument;
    private OhlcvDataSet ohlcvDataSet;

    public PositionFinancialDataSet(String instrument, OhlcvDataSet ohlcvDataSet, AttributeModel context) {
        super(instrument + "-positions", 2);
        this.instrument = instrument;
        this.ohlcvDataSet = ohlcvDataSet;
        fillDataSet(context.getRequiredAttribute(POSITIONS));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(PositionFinancialDataSet.class.getSimpleName()).log("started '{}'");
        }
    }

    private void fillDataSet(PositionContainer positions) {
        renderedPositionsBased = new TreeMap<>();
        renderedPositionsList = new ArrayList<>();
        Set<Position> positionSet = positions.getPositionByMarketSymbol(instrument);
        for (Position position : positionSet) {
            includePosition(position, true);
        }
        renderedPositionsList.addAll(renderedPositionsBased.values());
    }

    private void includePosition(Position position, boolean updateRequired) {
        if (position != null) {
            includeRenderedPosition(createPositionRendered(position, 1), updateRequired);
            if (position.getPositionStatus() == PositionStatus.CLOSED) {
                includeRenderedPosition(createPositionRendered(position, 2), updateRequired);
            }
        }
    }

    private void includeRenderedPosition(PositionRendered positionRendered, boolean updateRequired) {
        PositionRendered positionRenderedMain = renderedPositionsBased.get(positionRendered.index);
        if (positionRenderedMain != null) { // there exists some position for this index, merge it
            if (positionRenderedMain.positionId == positionRendered.positionId || !updateRequired) {
                return; // already included or update forbidden
            }
            if (positionRendered.entryExit == positionRenderedMain.entryExit) {
                positionRenderedMain.quantity += positionRendered.quantity;
            } else {
                positionRenderedMain.quantity -= positionRendered.quantity;
            }
            positionRenderedMain.price = (positionRenderedMain.price + positionRendered.price) / 2.0;
            positionRenderedMain.joinedEntries.addAll(positionRendered.joinedEntries);

        } else { // new rendered position, add them
            renderedPositionsBased.put(positionRendered.index, positionRendered);
            if (!updateRequired) { // add new order with new positions entry and exit parts
                renderedPositionsList.add(positionRendered);
            }
        }
    }

    private PositionRendered createPositionRendered(Position position, int entryExit) {
        PositionRendered positionRendered = new PositionRendered();
        positionRendered.positionId = position.getPositionId();
        positionRendered.entryExit = entryExit;
        positionRendered.posType = position.getPositionType();
        positionRendered.quantity = position.getPositionQuantity();
        positionRendered.price = entryExit == 1 ? position.getEntryPrice() : position.getExitPrice();
        positionRendered.closed = position.getPositionStatus() == PositionStatus.CLOSED;

        if (entryExit != 1) {
            // estimate entry index
            appendEntryLinkage(position, positionRendered);
        }

        // find correct OHLC item and attach position index to painted OHLC item timestamp
        long orderTime = entryExit == 1 ? position.getPositionEntryIndex() : position.getPositionExitIndex();
        positionRendered.index = estimateOhlcBasedTimestamp(orderTime);

        return positionRendered;
    }

    // necessary for linkage lines between exit and entry orders which are binding to same trade position
    private void appendEntryLinkage(Position position, PositionRendered positionRendered) {
        List<Double> entryRow = new ArrayList<>();
        entryRow.add((double) estimateOhlcBasedTimestamp(position.getPositionEntryIndex())); // x
        entryRow.add(position.getEntryPrice()); // y
        entryRow.add((position.getPositionType() == -1 && position.getEntryPrice() < position.getExitPrice())
                                     || (position.getPositionType() == 1 && position.getEntryPrice() > position.getExitPrice())
                             ? -1.0
                             : 1.0); // loss/profit
        positionRendered.joinedEntries.add(entryRow);
    }

    // estimate OHLC which shows the trades, find the timestamp of the bar for trade.
    private long estimateOhlcBasedTimestamp(long orderTime) {
        if (ohlcvDataSet != null) {
            int idx = ohlcvDataSet.getIndex(DIM_X, orderTime / 1000.0);
            IOhlcvItem ohlcvItem = ohlcvDataSet.getItem(idx);
            long ohlcItemTime = ohlcvItem.getTimeStamp().getTime();
            long ohlcItemTimeNext = -1;
            if (idx < ohlcvDataSet.getDataCount() - 1) {
                ohlcItemTimeNext = ohlcvDataSet.getItem(idx + 1).getTimeStamp().getTime();
            }
            if (orderTime <= ohlcItemTime) {
                orderTime = Math.round(ohlcItemTime / 1000.0);
            } else if (orderTime > ohlcItemTimeNext) {
                orderTime = Math.round(ohlcItemTimeNext / 1000.0);
            }
        }
        return orderTime;
    }

    public PositionRendered getPositionByTime(long corr) {
        return renderedPositionsBased.get(corr);
    }

    @Override
    public double get(int dimIndex, int index) {
        PositionRendered positionRendered = renderedPositionsList.get(index);
        if (dimIndex == DIM_X) { // return coordination by index
            return positionRendered == null ? -1.0 : positionRendered.index;
        }
        return positionRendered == null ? -1.0 : positionRendered.price;
    }

    @Override
    public int getDataCount() {
        return renderedPositionsList.size();
    }

    @Override
    public DataSet set(DataSet other, boolean copy) {
        this.renderedPositionsBased = new TreeMap<>(((PositionFinancialDataSet) other).renderedPositionsBased);
        this.renderedPositionsList = new ArrayList<>(((PositionFinancialDataSet) other).renderedPositionsList);
        this.instrument = ((PositionFinancialDataSet) other).instrument;
        this.ohlcvDataSet = ((PositionFinancialDataSet) other).ohlcvDataSet;

        return this;
    }

    @Override
    public void orderFilled(OrderEvent event) {
        // update internal memories
        Order order = event.getOrder();
        includePosition(order.isExitOrder() ? order.getExitOfPosition() : order.getEntryOfPosition(), false);
        fireInvalidated(new AddedDataEvent(PositionFinancialDataSet.this, "filled-order"));
    }

    @Override
    public void orderCancelled(OrderEvent event) {
        // nothing to do
    }
}
