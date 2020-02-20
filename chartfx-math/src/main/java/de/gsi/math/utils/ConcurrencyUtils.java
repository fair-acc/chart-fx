/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Parallel Colt.
 *
 * The Initial Developer of the Original Code is
 * Piotr Wendykier, Emory University.
 * Portions created by the Initial Developer are Copyright (C) 2007
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK *****
 */
package de.gsi.math.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Concurrency utilities.
 *
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * @author rstein - updates and code reformatting/removing obsolete code
 */
public class ConcurrencyUtils {
    private ConcurrencyUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final ExecutorService THREAD_POOL = Executors
            .newCachedThreadPool(new CustomThreadFactory(new CustomExceptionHandler()));

    private static boolean forceThreads = false;
    private static int forceNThreads = 1;

    public static int extendDimension(int x) {
        if (x < 1) {
            throw new IllegalArgumentException("x must be greater or equal 1");
        }
        final int nextExp = nextExp2(x);
        final int nextPow = nextExp + 1;
        return (int) Math.round(Math.pow(2.0, nextPow));
    }

    /**
     * Returns the number of available processors
     *
     * @return number of available processors
     */
    public static int getNumberOfProcessors() {
        return (forceThreads ? forceNThreads : Runtime.getRuntime().availableProcessors());
    }

    /**
     * Returns the current number of threads.
     *
     * @return the current number of threads.
     */
    public static int getNumberOfThreads() {
        return getNumberOfProcessors();
    }

    /**
     * @return the forceThreads
     */
    public static boolean isForceThreads() {
        return forceThreads;
    }

    /**
     * Checks if n is a power-of-two number
     *
     * @param n input parameter
     * @return true if n is power of 2
     */
    public static boolean isPowerOf2(int n) {
        if (n <= 0) {
            return false;
        }
        return (n & (n - 1)) == 0;
    }

    public static int nextExp2(final int n) {

        final double e = Math.log(n) / Math.log(2.0);
        int p = (int) Math.ceil(e);
        final double f = n / Math.pow(2.0, p);
        if (f == 0.5) {
            p -= 1;
        }
        return p;
    }

    /**
     * Returns the closest power of two greater than or equal to x.
     *
     * @param n input parameter
     * @return the closest power of two greater than or equal to x
     */
    public static int nextPow2(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be greater or equal 1");
        }
        if ((n & (n - 1)) == 0) {
            return n; // x is already a power-of-two number
        }

        int x = n;
        x |= (x >>> 1);
        x |= (x >>> 2);
        x |= (x >>> 4);
        x |= (x >>> 8);
        x |= (x >>> 16);
        //x |= (x >>> 32);
        return x + 1;
    }

    /**
     * Returns the closest power of two less than or equal to x
     *
     * @param x input parameter
     * @return the closest power of two less then or equal to x
     */
    public static int prevPow2(int x) {
        if (x < 1) {
            throw new IllegalArgumentException("x must be greater or equal 1");
        }
        return (int) Math.pow(2, Math.floor(Math.log(x) / Math.log(2)));
    }

    /**
     * @param forceThreads the forceThreads to set
     */
    public static void setForceThreads(boolean forceThreads) {
        ConcurrencyUtils.forceThreads = forceThreads;
    }

    /**
     * Sets the number of threads
     *
     * @param n number of requested threads
     */
    public static void setNumberOfThreads(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be greater or equal 1");
        }
        ConcurrencyUtils.setForceThreads(true);
        forceNThreads = n;
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease execution) for the specified number of
     * milliseconds.
     *
     * @param millis sleep duration in [ms]
     */
    public static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Submits a value-returning task for execution and returns a Future representing the pending results of the task.
     *
     * @param <T> value type of callable
     * @param task task for execution
     * @return a handle to the task submitted for execution
     */
    public static <T> Future<T> submit(Callable<T> task) {
        return THREAD_POOL.submit(task);
    }

    /**
     * Submits a Runnable task for execution and returns a Future representing that task.
     *
     * @param task task for execution
     * @return a handle to the task submitted for execution
     */
    public static Future<?> submit(Runnable task) {
        return THREAD_POOL.submit(task);
    }

    /**
     * Waits for all threads to complete computation.
     *
     * @param futures handles to running threads
     */
    public static void waitForCompletion(Future<?>[] futures) {
        final int size = futures.length;
        try {
            for (int j = 0; j < size; j++) {
                futures[j].get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            e.printStackTrace();
        }
    }

    private static class CustomThreadFactory implements ThreadFactory {
        private static final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        private static int threadCounter = 1;
        private final Thread.UncaughtExceptionHandler handler;

        CustomThreadFactory(Thread.UncaughtExceptionHandler handler) {
            this.handler = handler;
        }

        @Override
        public Thread newThread(Runnable r) {
            final Thread t = defaultFactory.newThread(r);
            t.setDaemon(true);
            t.setName("daemonised_chartfx_math_thread" + threadCounter);
            threadCounter++;
            t.setUncaughtExceptionHandler(handler);
            return t;
        }
    }
}
