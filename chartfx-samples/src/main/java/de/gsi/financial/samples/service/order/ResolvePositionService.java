package de.gsi.financial.samples.service.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.gsi.financial.samples.dos.Order;
import de.gsi.financial.samples.dos.Order.OrderStatus;
import de.gsi.financial.samples.dos.OrderExpression.OrderBuySell;
import de.gsi.financial.samples.dos.Position;
import de.gsi.financial.samples.dos.Position.PositionStatus;
import de.gsi.financial.samples.dos.PositionContainer;

/**
 * @author afischer
 */
public class ResolvePositionService {
    private static final Log logger = LogFactory.getLog(ResolvePositionService.class);

    /**
     * Main resolving of trading positions by its order commands.
     *
     * @param order             Order domain object
     * @param positionContainer with all positions of the system
     */
    public static void resolvePositions(Order order, PositionContainer positionContainer) {
        if (!OrderStatus.FILLED.equals(order.getStatus())) {
            throw new IllegalArgumentException("The order has to be filled for position processing. Order id: " + order.getInternalOrderId());
        }
        int restQuantity = order.getOrderExpression().getOrderQuantity();
        List<Position> newCreatedPositions = new ArrayList<>();

        // try to close opened positions
        Set<Position> openedPositions = positionContainer.getFastOpenedPositionByMarketSymbol(order.getSymbol());
        int positionTypeReverse = -1 * (order.getOrderExpression().getBuySell().equals(OrderBuySell.BUY) ? 1 : -1);

        Position[] positionArray = openedPositions.toArray(new Position[0]);
        for (int i = positionArray.length - 1; i >= 0; i--) {
            Position position = positionArray[i];
            if (position.getPositionType() == positionTypeReverse) {
                // rest quantity calculation
                restQuantity -= position.getPositionQuantity();
                // close whole position and
                // ends the method
                if (restQuantity == 0) {
                    closePositionOperation(positionContainer, position, order);
                    break;

                    // the open position is not close whole -> the part of position is still opened
                } else if (restQuantity < 0) {
                    Position openedPosition = duplicatePosition(position);
                    openedPosition.setPositionQuantity(Math.abs(restQuantity));
                    newCreatedPositions.add(openedPosition);

                    position.setPositionQuantity(position.getPositionQuantity() + restQuantity);
                    closePositionOperation(positionContainer, position, order);
                    break;

                    // the position is whole closed, but there is rest quantity which create new position
                } else { // restQuantity > 0
                    closePositionOperation(positionContainer, position, order);
                    // continue to next
                }
            }
        }
        // rest quantity has to create the position by same direction as order
        if (restQuantity > 0) {
            Position position = createPositionByOrder(order);
            position.setPositionQuantity(restQuantity);
            newCreatedPositions.add(position);
        }
        // operations with opened positions
        for (Position position : newCreatedPositions) {
            // add new created positions to the container
            positionContainer.addPosition(position);
        }
    }

    private static void closePositionOperation(PositionContainer positionContainer, Position position, Order order) {
        // close position operations
        position.setPositionExitIndex(order.getLastActivityTime().getTime());
        position.setExitTime(order.getLastActivityTime());
        position.setExitPrice(order.getAverageFillPrice());
        position.setExitOrder(order);
        position.setPositionStatus(PositionStatus.CLOSED);

        order.setExitOrder(true); // order was used for closing of position
        order.setExitOfPosition(position); // cross linkage

        positionContainer.notifyPositionClosed(position);
    }

    /**
     * Calculate P/L of the position
     *
     * @param position       for computation process
     * @param fullpointvalue of the market with this position
     * @return P/L amount double
     */
    public static double calculatePositionProfitLoss(Position position, double fullpointvalue) {
        return (position.getExitPrice() - position.getEntryPrice()) * position.getPositionType() * position.getPositionQuantity() * fullpointvalue;
    }

    /**
     * Create prototype of position by order
     *
     * @param order Order
     * @return position prototype
     */
    public static Position createPositionByOrder(Order order) {
        if (!OrderStatus.FILLED.equals(order.getStatus())) {
            String message = "The position cannot be created, because order " + order.getInternalOrderId() + "/" + order.getServiceOrderId() + " is not filled.";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        // position type 1=LONG and -1=SHORT
        int positionType = order.getOrderExpression().getBuySell().equals(OrderBuySell.BUY) ? 1 : -1;
        // create position instance
        Position position = new Position(InternalPositionIdGenerator.generateId(), order.getOrderIndex(), order.getUserName(), order.getLastActivityTime(),
                positionType, order.getSymbol(), order.getAccountId(), order.getAverageFillPrice(), order.getOrderExpression().getOrderQuantity());
        // crosslinkage can be helpful with fast resolving of exit orders vs entry orders
        position.setEntryOrder(order);
        order.setEntryOfPosition(position); // cross linkage

        return position;
    }

    /**
     * Duplicate inserted position (new unique ID is generated)
     *
     * @param position for cloning
     * @return cloned position
     */
    public static Position duplicatePosition(Position position) {
        // create position instance
        Position position2 = new Position(InternalPositionIdGenerator.generateId(), position.getPositionEntryIndex(), position.getEntryUserName(), position.getEntryTime(),
                position.getPositionType(), position.getSymbol(), position.getAccountId(), position.getEntryPrice(), position.getPositionQuantity());
        position2.setPositionStatus(position.getPositionStatus());
        position2.setPositionExitIndex(position.getExitTime() != null ? position.getExitTime().getTime() : -1L);
        position2.setExitTime(position.getExitTime());
        position2.setExitPrice(position.getExitPrice());
        // cross linkages
        position2.setEntryOrder(position.getEntryOrder());
        position2.setExitOrder(position.getExitOrder());

        return position2;
    }

    private ResolvePositionService() {
    }
}
