package io.fair_acc.chartfx.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javafx.animation.AnimationTimer;

import org.apache.commons.lang3.tuple.Pair;

import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.events.EventProcessor;

/**
 * An event processor class which processes dataset events un the UI thread of the chart.
 * All datasets added to this processor will be processed whenever they are invalidated.
 * <p>
 * TODO: check how to ensure that everything gets garbage-collected correctly
 */
public class FxEventProcessor extends AnimationTimer implements EventProcessor {
    BitState localState = BitState.initDirty(this, ChartBits.DataSetMask);
    BitState stateRoot = BitState.initDirtyMultiThreaded(this, ChartBits.DataSetMask);
    List<Pair<BitState, Runnable>> actions = new ArrayList<>();
    private static final AtomicReference<FxEventProcessor> INSTANCE = new AtomicReference<>();

    public static FxEventProcessor getInstance() {
        FxEventProcessor result = INSTANCE.get();
        if (result != null) {
            return result;
        }
        // probably does not exist yet, but initialise in thread safe way
        result = new FxEventProcessor();
        if (INSTANCE.compareAndSet(null, result)) {
            return result;
        } else {
            return INSTANCE.get();
        }
    }

    public FxEventProcessor() {
        start();
    }

    @Override
    public void handle(final long now) {
        localState.setDirty(stateRoot.clear());
        if (localState.isDirty()) {
            for (final var action : actions) {
                if (action.getLeft().isDirty(ChartBits.DataSetMask)) {
                    action.getLeft().clear();
                    try {
                        action.getRight().run();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        localState.clear();
        // TODO: perform multiple iterations if there are changes to handle wrongly ordered processing chains and break after timeout
    }

    public BitState getBitState() {
        return stateRoot;
    }

    @Override
    public void addAction(final BitState obj, final Runnable action) {
        obj.addInvalidateListener(stateRoot);
        actions.add(Pair.of(obj, action));
    }
}
