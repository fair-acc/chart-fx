package de.gsi.chart.axes.spi;

import javafx.application.Application;

/**
 * Small test to demonstrate that the SoftHashMap TickMark cache sizes are in fact limited, memory-bound and do not leak
 * rather than their earlier WeakHashMap-based counterpart that had issues when the weak key was also part of the kept value.
 *
 * See following references for details:
 * https://ewirch.github.io/2013/12/weakhashmap-memory-leaks.html
 * https://franke.ms/memoryleak1.wiki
 * N.B. latter author filed this as a bug at http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7145759 which was apparently
 * dropped by Oracle devs as supposedly the intended behaviour for a WeakHashMap-based cache.
 *
 * effect is best seen with limiting the jvm's max memory: -Xmx20m
 *
 * @author rstein
 */
public class MemoryLeakTestDefaultAxis_run { // NOPMD -nomen est omen
    public static void main(final String[] args) {
        Application.launch(MemoryLeakTestDefaultAxis.class);
    }
}
