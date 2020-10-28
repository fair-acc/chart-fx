package de.gsi.microservice.concepts.aggregate;

/**
 * Basic filter interface description
 *
 * @author rstein
 *  N.B. while 'toString()', 'hashCode()' and 'equals()' is ubiquously defined via the Java 'Object' class, these definition are kept for symmetry with the C++ implementation
 */
public interface Filter {
    /**
     * reinitialises the filter to safe default values
     */
    void clear();

    /**
     * @param other filter this filter should copy its data to
     */
    void copyTo(Filter other);

    boolean equals(Object other);

    int hashCode();

    /**
     * @return filter description including internal state (if any).
     */
    String toString();
}
