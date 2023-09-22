package io.fair_acc.bench;

import java.util.function.IntSupplier;

/**
 * Levels for different types of benchmark information.
 * Copied from logging conventions.
 *
 * @author ennerf
 */
public enum BenchLevel implements IntSupplier {
    /**
     * Coarse-grained high-level information
     */
    Info(400),

    /**
     * Information often used for debugging
     */
    Debug(500),

    /**
     * Used for debugging purposes. Includes the most detailed information
     */
    Trace(600);

    BenchLevel(int level) {
        this.level = level;
    }

    @Override
    public int getAsInt() {
        return level;
    }

    final int level;

}
