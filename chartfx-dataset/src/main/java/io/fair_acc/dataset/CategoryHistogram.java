package io.fair_acc.dataset;

// for library loggers
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// for application loggers
// import io.fair_acc.cs.co.ap.common.gui.elements.logger.AppLogger;

/**
 * @deprecated Work in Progress, don't use yet
 * @author Alexander Krimm
 */
@Deprecated
public interface CategoryHistogram extends Histogram {
    /**
     * Increment bin with name with by 1. if x is less than the low-edge of the first bin, the Underflow bin is
     * incremented if x is equal to or greater than the upper edge of last bin, the Overflow bin is incremented
     *
     * @param name name to be added
     * @return corresponding bin number which has its content incremented by w
     */
    default int fill(final String name) {
        return fill(name, 1.0);
    }

    /**
     * Increment bin with name with a weight w. if x is less than the low-edge of the first bin, the Underflow bin is
     * incremented if x is equal to or greater than the upper edge of last bin, the Overflow bin is incremented
     *
     * @param name name to be added
     * @param w weight for given name
     * @return corresponding bin number which has its content incremented by w
     */
    int fill(final String name, double w);
}
