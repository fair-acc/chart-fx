package de.gsi.financial.samples.dos;

import java.io.Serializable;
import java.util.*;

import de.gsi.financial.samples.dos.Position.PositionStatus;

public class PositionContainer implements Serializable {
    private static final long serialVersionUID = -6964168549256831250L;

    // positions by its ID
    private Map<Integer, Position> positionIdMap = new HashMap<>();

    // all positions per symbol
    private Map<String, Set<Position>> positionMap = new HashMap<>();

    // open positions per symbol only - performance step
    private Map<String, Set<Position>> openPositionMap = new HashMap<>();

    // all positions per strategy
    private Map<String, Set<Position>> strategyPositionMap = new HashMap<>();

    // open positions per strategy only - performance step
    private Map<String, Set<Position>> strategyOpenPositionMap = new HashMap<>();

    public void addPosition(Position position) {
        positionIdMap.put(position.getPositionId(), position);

        Set<Position> positionSet = positionMap.computeIfAbsent(
                position.getSymbol(), k -> new LinkedHashSet<>());
        addAndReplaceSet(positionSet, position);

        // strategy scope
        String strategy = position.getStrategy();
        if (strategy != null) {
            positionSet = strategyPositionMap.computeIfAbsent(
                    strategy, k -> new LinkedHashSet<>());
            addAndReplaceSet(positionSet, position);
        }

        // fast map for opened positions
        if (position.getPositionStatus() == PositionStatus.OPENED) {
            positionSet = openPositionMap.computeIfAbsent(
                    position.getSymbol(), k -> new LinkedHashSet<>());
            addAndReplaceSet(positionSet, position);

            // strategy scope
            if (strategy != null) {
                positionSet = strategyOpenPositionMap.computeIfAbsent(
                        strategy, k -> new LinkedHashSet<>());
                addAndReplaceSet(positionSet, position);
            }
        } else { // position closed - remove it from fast maps
            notifyPositionClosed(position);
        }
    }

    private void addAndReplaceSet(Set<Position> positionSet, Position position) {
        if (!positionSet.add(position)) {
            positionSet.remove(position);
            positionSet.add(position);
        }
    }

    public boolean removePosition(Position position) {
        positionIdMap.remove(position.getPositionId());
        String strategy = position.getStrategy();
        if (position.getPositionStatus() == PositionStatus.OPENED) {
            Set<Position> positionSet = openPositionMap.get(position.getSymbol());
            if (positionSet != null) {
                positionSet.remove(position);
            }
            if (strategy != null) {
                positionSet = strategyOpenPositionMap.get(strategy);
                if (positionSet != null) {
                    positionSet.remove(position);
                }
            }
        }
        if (strategy != null) {
            Set<Position> positionSet = positionMap.get(strategy);
            if (positionSet != null) {
                positionSet.remove(position);
            }
        }
        Set<Position> positionSet = positionMap.get(position.getSymbol());
        return positionSet != null && positionSet.remove(position);
    }

    public void notifyPositionClosed(Position position) {
        Set<Position> positionSet = openPositionMap.get(position.getSymbol());
        if (positionSet != null) {
            positionSet.remove(position);
        }
        if (position.getStrategy() != null) {
            positionSet = strategyOpenPositionMap.get(position.getStrategy());
            if (positionSet != null) {
                positionSet.remove(position);
            }
        }
    }

    public int size() {
        int size = 0;
        for (Set<Position> positions : positionMap.values()) {
            size += positions.size();
        }
        return size;
    }

    public int size(String symbol) {
        Set<Position> positions = positionMap.get(symbol);
        return positions == null ? 0 : positions.size();
    }

    public int sizeByStrategy(String strategy) {
        Set<Position> positions = strategyPositionMap.get(strategy);
        return positions == null ? 0 : positions.size();
    }

    public void clear() {
        positionIdMap = new HashMap<>();
        positionMap = new HashMap<>();
        openPositionMap = new HashMap<>();
        strategyPositionMap = new HashMap<>();
        strategyOpenPositionMap = new HashMap<>();
    }

    public Position getPositionById(int positionId) {
        return positionIdMap.get(positionId);
    }

    public Set<Position> getPositionByMarketSymbol(String symbol) {
        Set<Position> positions = null;
        if (symbol != null) {
            positions = positionMap.get(symbol);
        }
        if (positions == null) {
            positions = new LinkedHashSet<>();
        }
        return positions;
    }

    public Set<Position> getPositionByStrategy(String strategy) {
        Set<Position> positions = null;
        if (strategy != null) {
            positions = strategyPositionMap.get(strategy);
        }
        if (positions == null) {
            positions = new LinkedHashSet<>();
        }
        return positions;
    }

    public List<Position> getAllPositionList() {
        ArrayList<Position> positionList = new ArrayList<>();
        for (Set<Position> positions : positionMap.values()) {
            positionList.addAll(positions);
        }
        return positionList;
    }

    public List<Position> getAllPositionListTimeOrdered() {
        List<Position> positionList = getAllPositionList();
        positionList.sort(Comparator.comparing(Position::getExitTime));
        return positionList;
    }

    public List<Position> getPositionListByMarketSymbol(String symbol) {
        return new ArrayList<>(getPositionByMarketSymbol(symbol));
    }

    public List<Position> getPositionListByStrategy(String strategy) {
        return new ArrayList<>(getPositionByStrategy(strategy));
    }

    // optimized solution for opened positions
    public Set<Position> getFastOpenedPositionByMarketSymbol(String symbol) {
        Set<Position> openedPositionSet = openPositionMap.get(symbol);
        if (openedPositionSet == null) {
            return new LinkedHashSet<>();
        }
        return openedPositionSet;
    }

    // optimized solution for opened positions
    public Set<Position> getFastOpenedPositionByStrategy(String strategy) {
        Set<Position> openedPositionSet = strategyOpenPositionMap.get(strategy);
        if (openedPositionSet == null) {
            return new LinkedHashSet<>();
        }
        return openedPositionSet;
    }

    public List<Position> getOpenedPositionsByMarketSymbol(String symbol) {
        return getPositionStatusByMarketSymbol(symbol, PositionStatus.OPENED);
    }

    public List<Position> getClosedPositionsByMarketSymbol(String symbol) {
        return getPositionStatusByMarketSymbol(symbol, PositionStatus.CLOSED);
    }

    public List<Position> getOpenedPositionsByStrategy(String strategy) {
        return getPositionStatusByStrategy(strategy, PositionStatus.OPENED);
    }

    public List<Position> getClosedPositionsByStrategy(String strategy) {
        return getPositionStatusByStrategy(strategy, PositionStatus.CLOSED);
    }

    public List<Position> getClosedPositions() {
        ArrayList<Position> positions = new ArrayList<>();
        for (String symbol : getTradedMarketSymbols()) {
            positions.addAll(getPositionStatusByMarketSymbol(symbol, PositionStatus.CLOSED));
        }
        return positions;
    }

    private List<Position> getPositionStatusByMarketSymbol(String symbol, PositionStatus positionStatus) {
        List<Position> positions = new ArrayList<>();
        for (Position position : getPositionByMarketSymbol(symbol)) {
            if (positionStatus.equals(position.getPositionStatus())) {
                positions.add(position);
            }
        }
        return positions;
    }

    private List<Position> getPositionStatusByStrategy(String strategy, PositionStatus positionStatus) {
        List<Position> positions = new ArrayList<>();
        for (Position position : getPositionByStrategy(strategy)) {
            if (positionStatus.equals(position.getPositionStatus())) {
                positions.add(position);
            }
        }
        return positions;
    }

    public Set<String> getTradedMarketSymbols() {
        return positionMap.keySet();
    }

    public Set<String> getTradedStrategies() {
        return strategyPositionMap.keySet();
    }

    public boolean contains(Position position) {
        Set<Position> positionSet = getPositionByMarketSymbol(position.getSymbol());
        return positionSet.contains(position);
    }

    public void compare(PositionContainer positionContainer) throws IllegalStateException {
        for (String market : positionMap.keySet()) {
            Set<Position> positions = getPositionByMarketSymbol(market);
            Set<Position> otherPositions = positionContainer.getPositionByMarketSymbol(market);
            if (positions.size() != otherPositions.size()) {
                throw new IllegalStateException("The size of position containers is different " + positions.size() + "; " + otherPositions.size());
            }
            for (Position otherPosition : otherPositions) {
                if (positions.contains(otherPosition)) {
                    boolean found = false;
                    for (Position position : positions) {
                        if (position.equals(otherPosition)) {
                            if (!position.comparePosition(otherPosition)) {
                                throw new IllegalStateException("The positions are different " + otherPosition + ", " + position);
                            }
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new IllegalStateException("The position doesn't exist " + otherPosition);
                    }

                } else {
                    throw new IllegalStateException("The position doesn't exist " + otherPosition);
                }
            }
        }
    }

    @Override
    public String toString() {
        return positionMap.values().toString();
    }

    public String toLineString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PositionContainer {").append(System.lineSeparator());
        for (Position position : getAllPositionListTimeOrdered()) {
            sb.append(position).append(System.lineSeparator());
        }
        sb.append("}").append(System.lineSeparator());
        return sb.toString();
    }
}
