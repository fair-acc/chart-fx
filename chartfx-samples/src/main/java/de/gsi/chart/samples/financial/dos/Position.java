/**
 * LGPL-3.0, 2020/21, GSI-CS-CO/Chart-fx, BTA HF OpenSource Java-FX Branch, Financial Charts
 */
package de.gsi.chart.samples.financial.dos;

import de.gsi.chart.samples.financial.service.ConcurrentDateFormatAccess;
import de.gsi.chart.samples.financial.service.period.Period;
import de.gsi.dataset.spi.financial.api.attrs.AttributeModel;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * @author afischer
 */
public class Position implements Comparable<Position>, Serializable {
    private static final long serialVersionUID = -7967285725003509765L;

    public static final ConcurrentDateFormatAccess dateFormat = new ConcurrentDateFormatAccess("MM/dd/yyyy HH:mm:ss");

    public enum PositionStatus {
        OPENED,
        CLOSED
    }

    private final int positionId;
    private Integer timePosId;
    private final Date entryTime;
    private Date exitTime;
    private final int positionType; // -1 = short, 1 = long
    private PositionStatus positionStatus = PositionStatus.OPENED;
    private final String symbol;
    private String strategy;
    private final String entryUserName;
    private final String accountId;
    private int positionQuantity;
    private final Double entryPrice;
    private Double exitPrice;
    private Double mae; // Maximum Adverse Excursion
    private Double mfe;
    private Double risk; // Risk for Position Trade
    private boolean isLive = false; // the trade positions was performed live
    private Double pl; // dollar profit/loss available if the entry/exit prices are not present
    private Period period; // timeframe which is used for strategy approach
    transient private Order entryOrder;
    transient private Order exitOrder;
    transient private AttributeModel addons; // append transient data about position for calculations

    public Position(int positionId, String entryUserName, Date entryTime, int positionType,
            String symbol, String accountId, Double entryPrice, int positionQuantity) {
        this.positionId = positionId;
        this.entryUserName = entryUserName;
        this.entryTime = entryTime;
        this.positionType = positionType;
        this.symbol = symbol;
        this.accountId = accountId;
        this.entryPrice = entryPrice;
        this.positionQuantity = positionQuantity;
    }

    public Position(int positionId, String entryUserName, String strategy, Date entryTime, int positionType,
            String symbol, String accountId, Double entryPrice, int positionQuantity) {
        this(positionId, entryUserName, entryTime, positionType, symbol, accountId, entryPrice, positionQuantity);
        this.strategy = strategy;
    }

    public Position copyDeep() {
        return SerializationUtils.clone(this);
    }

    public Order getEntryOrder() {
        return entryOrder;
    }

    public void setEntryOrder(Order entryOrder) {
        this.entryOrder = entryOrder;
    }

    public Order getExitOrder() {
        return exitOrder;
    }

    public AttributeModel getAddons() {
        if (addons == null)
            addons = new AttributeModel();
        return addons;
    }

    public void setExitOrder(Order exitOrder) {
        this.exitOrder = exitOrder;
    }

    public Double getMfe() {
        return mfe;
    }

    public void setMfe(Double mfe) {
        this.mfe = mfe;
    }

    public int getPositionQuantity() {
        return positionQuantity;
    }

    public String getEntryUserName() {
        return entryUserName;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public void setPositionQuantity(int positionQuantity) {
        this.positionQuantity = positionQuantity;
    }

    public Date getExitTime() {
        return exitTime;
    }

    public void setExitTime(Date exitTime) {
        this.exitTime = exitTime;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    /**
     * @return Maximum Adverse Excursion
     */
    public Double getMae() {
        return mae;
    }

    public void setMae(Double mae) {
        this.mae = mae;
    }

    /**
     * @return Necessary risk for trade this position (before open position)
     */
    public Double getRisk() {
        return risk;
    }

    public void setRisk(Double risk) {
        this.risk = risk;
    }

    public PositionStatus getPositionStatus() {
        return positionStatus;
    }

    public void setPositionStatus(PositionStatus positionStatus) {
        this.positionStatus = positionStatus;
    }

    public Double getExitPrice() {
        return exitPrice;
    }

    public void setExitPrice(Double exitPrice) {
        this.exitPrice = exitPrice;
    }

    public int getPositionId() {
        return positionId;
    }

    public Date getEntryTime() {
        return entryTime;
    }

    // -1 = short, 1 = long
    public int getPositionType() {
        return positionType;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getAccountId() {
        return accountId;
    }

    public Double getEntryPrice() {
        return entryPrice;
    }

    public Double getPl() {
        return pl;
    }

    public void setPl(Double pl) {
        this.pl = pl;
    }

    public Integer getTimePosId() {
        return timePosId;
    }

    public void setTimePosId(Integer timePosId) {
        this.timePosId = timePosId;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    @Override
    public int compareTo(Position o) {
        return positionId - o.positionId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + positionId;
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
        Position other = (Position) obj;
        return positionId == other.positionId;
    }

    public boolean comparePosition(Position position) {
        if (this == position)
            return true;
        if (position == null)
            return false;
        if (getClass() != position.getClass())
            return false;
        Position other = position;
        if (accountId == null) {
            if (other.accountId != null)
                return false;
        } else if (!accountId.equals(other.accountId))
            return false;
        if (entryPrice == null) {
            if (other.entryPrice != null)
                return false;
        } else if (!entryPrice.equals(other.entryPrice))
            return false;
        if (entryTime == null) {
            if (other.entryTime != null)
                return false;
        } else if (!DateUtils.truncatedEquals(entryTime, other.entryTime, Calendar.MINUTE))
            return false;
        if (entryUserName == null) {
            if (other.entryUserName != null)
                return false;
        } else if (!entryUserName.equals(other.entryUserName))
            return false;
        if (strategy == null) {
            if (other.strategy != null)
                return false;
        } else if (!strategy.equals(other.strategy))
            return false;
        if (exitPrice == null) {
            if (other.exitPrice != null)
                return false;
        } else if (!exitPrice.equals(other.exitPrice))
            return false;
        if (exitTime == null) {
            if (other.exitTime != null)
                return false;
        } else if (!DateUtils.truncatedEquals(exitTime, other.exitTime, Calendar.MINUTE))
            return false;
        if (mae == null) {
            if (other.mae != null)
                return false;
        } else if (!mae.equals(other.mae))
            return false;
        if (risk == null) {
            if (other.risk != null)
                return false;
        } else if (!risk.equals(other.risk))
            return false;
        if (mfe == null) {
            if (other.mfe != null)
                return false;
        } else if (!mfe.equals(other.mfe))
            return false;
        if (isLive != other.isLive)
            return false;
        if (pl == null) {
            if (other.pl != null)
                return false;
        } else if (!pl.equals(other.pl))
            return false;
        if (timePosId == null) {
            if (other.timePosId != null)
                return false;
        } else if (!timePosId.equals(other.timePosId))
            return false;
        if (period == null) {
            if (other.period != null)
                return false;
        } else if (!period.equals(other.period))
            return false;
        if (positionId != other.positionId)
            return false;
        if (positionQuantity != other.positionQuantity)
            return false;
        if (positionStatus != other.positionStatus)
            return false;
        if (positionType != other.positionType)
            return false;
        if (symbol == null) {
            if (other.symbol != null)
                return false;
        } else if (!symbol.equals(other.symbol))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Position [positionId=" + positionId + ", timePosId=" + timePosId + ", entryUserName=" + entryUserName + ", strategy=" + strategy
                + ", entryTime=" + (entryTime != null ? dateFormat.format(entryTime) : "NO ENTRY") + ", exitTime=" + (exitTime != null ? dateFormat.format(exitTime) : "NO EXIT") + ", positionType="
                + positionType + ", positionStatus=" + positionStatus + ", symbol=" + symbol + ", accountId=" + accountId
                + ", positionQuantity=" + positionQuantity + ", entryPrice=" + entryPrice + ", exitPrice=" + exitPrice
                + ", MaxPositionProfit=" + mfe + ", MAE=" + mae + ", risk=" + risk
                + ", period=" + period + ", pl=" + pl + ", isLive=" + isLive + "]";
    }
}
