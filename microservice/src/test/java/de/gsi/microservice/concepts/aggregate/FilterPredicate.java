package de.gsi.microservice.concepts.aggregate;

import java.util.function.Predicate;

//@FunctionalInterface
public interface FilterPredicate {
    /**
     * Evaluates this predicate on the given arguments.
     *
     * @param filterClass the filter class
     * @param filterPredicate the filter predicate object
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    <R extends Filter> boolean test(Class<R> filterClass, Predicate<R> filterPredicate);

    //    /**
    //     * @param other a filter predicate that will be logically-ANDed with this predicate
    //     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
    //     */
    //    FilterPredicate and(FilterPredicate other);

    //    /**
    //     * Returns a predicate that represents the logical negation of this
    //     * predicate.
    //     *
    //     * @return a predicate that represents the logical negation of this predicate
    //     */
    //    <R extends Filter> FilterPredicate negate();

    //
    //    /**
    //     * @param other a predicate that will be logically-ORed with this predicate
    //     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
    //     * @throws NullPointerException if other is null
    //     */
    //    default FilterPredicate or(FilterPredicate other) {
    //        Objects.requireNonNull(other);
    //        return (t, u) -> test(t, u) || other.test(t, u);
    //    }
}