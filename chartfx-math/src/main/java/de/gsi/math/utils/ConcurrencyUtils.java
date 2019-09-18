/* ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK ***** */
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
 */
public class ConcurrencyUtils {
	private static final ExecutorService	THREAD_POOL	= Executors.newCachedThreadPool(
			new CustomThreadFactory(new CustomExceptionHandler()));	
	private static int	THREADS_BEGIN_N_1D_FFT_2THREADS	= 8192;
	private static int	THREADS_BEGIN_N_1D_FFT_4THREADS	= 65536;
	private static int	THREADS_BEGIN_N_1D				= 32768;
	private static int	THREADS_BEGIN_N_2D				= 65536;
	private static int	THREADS_BEGIN_N_3D				= 65536;
	
	private static boolean 	forceThreads  = false;
	private static int 		forceNThreads = 1;

	private static class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {
		@Override
        public void uncaughtException(Thread t, Throwable e) {
			e.printStackTrace();
		}
	}

	private static class CustomThreadFactory implements ThreadFactory {
		private static final ThreadFactory	defaultFactory	= Executors.defaultThreadFactory();
		private final Thread.UncaughtExceptionHandler	handler;
		private static int threadCounter = 1;
		
		CustomThreadFactory(Thread.UncaughtExceptionHandler handler) {
			this.handler = handler;
		}

		@Override
        public Thread newThread(Runnable r) {
			Thread t = defaultFactory.newThread(r);
			t.setDaemon(true);
			t.setName("daemonised_chartfx_math_thread"+threadCounter);
			threadCounter++;
			t.setUncaughtExceptionHandler(handler);
			return t;
		}
	};

	/**
	 * Causes the currently executing thread to sleep (temporarily cease
	 * execution) for the specified number of milliseconds.
	 * 
	 * @param millis sleep duration in [ms]
	 */
	public static void sleep(long millis) {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Submits a value-returning task for execution and returns a Future
	 * representing the pending results of the task.
	 * 
	 * @param <T> value type of callable
	 * @param task
	 *            task for execution
	 * @return a handle to the task submitted for execution
	 */
	public static <T> Future<T> submit(Callable<T> task) {
		return THREAD_POOL.submit(task);
	}

	/**
	 * Submits a Runnable task for execution and returns a Future representing
	 * that task.
	 * 
	 * @param task
	 *            task for execution
	 * @return a handle to the task submitted for execution
	 */
	public static Future<?> submit(Runnable task) {
		return THREAD_POOL.submit(task);
	}

	/**
	 * Returns the number of available processors
	 * 
	 * @return number of available processors
	 */
	public static int getNumberOfProcessors() {		
		return (forceThreads?forceNThreads:Runtime.getRuntime().availableProcessors());
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
	 * Waits for all threads to complete computation.
	 * 
	 * @param futures
	 *            handles to running threads
	 */
	public static void waitForCompletion(Future<?>[] futures) {
		int size = futures.length;
		try {
			for (int j = 0; j < size; j++) {
				futures[j].get();
			}
		} catch (ExecutionException ex) {
			ex.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the minimal size of 1D data for which threads are used.
	 * 
	 * @return the minimal size of 1D data for which threads are used
	 */
	public static int getThreadsBeginN_1D() {
		return THREADS_BEGIN_N_1D;
	}

	/**
	 * Returns the minimal size of 1D data for which two threads are used.
	 * 
	 * @return the minimal size of 1D data for which two threads are used
	 */
	public static int getThreadsBeginN_1D_FFT_2Threads() {
		return THREADS_BEGIN_N_1D_FFT_2THREADS;
	}

	/**
	 * Returns the minimal size of 1D data for which four threads are used.
	 * 
	 * @return the minimal size of 1D data for which four threads are used
	 */
	public static int getThreadsBeginN_1D_FFT_4Threads() {
		return THREADS_BEGIN_N_1D_FFT_4THREADS;
	}

	/**
	 * Returns the minimal size of 2D data for which threads are used.
	 * 
	 * @return the minimal size of 2D data for which threads are used
	 */
	public static int getThreadsBeginN_2D() {
		return THREADS_BEGIN_N_2D;
	}

	/**
	 * Returns the minimal size of 3D data for which threads are used.
	 * 
	 * @return the minimal size of 3D data for which threads are used
	 */
	public static int getThreadsBeginN_3D() {
		return THREADS_BEGIN_N_3D;
	}

	/**
	 * Sets the minimal size of 1D data for which two threads are used.
	 * 
	 * @param n
	 *            the minimal size of 1D data for which two threads are used
	 */
	public static void setThreadsBeginN_1D_FFT_2Threads(int n) {
		if (n < 512) {
			THREADS_BEGIN_N_1D_FFT_2THREADS = 512;
		} else {
			THREADS_BEGIN_N_1D_FFT_2THREADS = n;
		}
	}

	/**
	 * Sets the minimal size of 1D data for which four threads are used.
	 * 
	 * @param n
	 *            the minimal size of 1D data for which four threads are used
	 */
	public static void setThreadsBeginN_1D_FFT_4Threads(int n) {
		if (n < 512) {
			THREADS_BEGIN_N_1D_FFT_4THREADS = 512;
		} else {
			THREADS_BEGIN_N_1D_FFT_4THREADS = n;
		}
	}

	/**
	 * Sets the minimal size of 1D data for which threads are used.
	 * 
	 * @param n
	 *            the minimal size of 1D data for which threads are used
	 */
	public static void setThreadsBeginN_1D(int n) {
		THREADS_BEGIN_N_1D = n;
	}

	/**
	 * Sets the minimal size of 2D data for which threads are used.
	 * 
	 * @param n
	 *            the minimal size of 2D data for which threads are used
	 */
	public static void setThreadsBeginN_2D(int n) {
		THREADS_BEGIN_N_2D = n;
	}

	/**
	 * Sets the minimal size of 3D data for which threads are used.
	 * 
	 * @param n
	 *            the minimal size of 3D data for which threads are used
	 */
	public static void setThreadsBeginN_3D(int n) {
		THREADS_BEGIN_N_3D = n;
	}

	/**
	 * Resets the minimal size of 1D data for which two and four threads are
	 * used.
	 */
	public static void resetThreadsBeginN_FFT() {
		THREADS_BEGIN_N_1D_FFT_2THREADS = 8192;
		THREADS_BEGIN_N_1D_FFT_4THREADS = 65536;
	}

	/**
	 * Resets the minimal size of 1D, 2D and 3D data for which threads are used.
	 */
	public static void resetThreadsBeginN() {
		THREADS_BEGIN_N_1D = 32768;
		THREADS_BEGIN_N_2D = 65536;
		THREADS_BEGIN_N_3D = 65536;

	}

	/**
	 * Sets the number of threads
	 * 
	 * @param n number of requested threads
	 */
	public static void setNumberOfThreads(int n) {
		if (n < 1)
			throw new IllegalArgumentException("n must be greater or equal 1");
		ConcurrencyUtils.setForceThreads(true);
		forceNThreads = n;		
	}

	/**
	 * Returns the closest power of two greater than or equal to x.
	 * 
	 * @param x input parameter
	 * @return the closest power of two greater than or equal to x
	 */
	public static int nextPow2(int x) {
		if (x < 1)
			throw new IllegalArgumentException("x must be greater or equal 1");
		if ((x & (x - 1)) == 0) {
			return x; // x is already a power-of-two number 
		}
		x |= (x >>> 1);
		x |= (x >>> 2);
		x |= (x >>> 4);
		x |= (x >>> 8);
		x |= (x >>> 16);
		x |= (x >>> 32);
		return x + 1;
	}

	public static int extendDimension(int x) {
		if (x < 1)
			throw new IllegalArgumentException("x must be greater or equal 1");
		int nextExp = nextExp2(x);
		int nextPow = nextExp + 1;
		int extDim = (int) Math.round(Math.pow(2.0, (double) nextPow));
		return extDim;
	}

	public static int nextExp2(int n) {

		double e = Math.log((double) n) / Math.log(2.0);
		int p = (int) Math.ceil(e);
		double f = n / Math.pow(2.0, (double) p);
		if (f == 0.5) {
			p = p - 1;
		}
		return p;
	}

	/**
	 * Returns the closest power of two less than or equal to x
	 * 
	 * @param x input parameter
	 * @return the closest power of two less then or equal to x
	 */
	public static int prevPow2(int x) {
		if (x < 1)
			throw new IllegalArgumentException("x must be greater or equal 1");
		return (int) Math.pow(2, Math.floor(Math.log(x) / Math.log(2)));
	}
		

	/**
	 * Checks if n is a power-of-two number
	 * 
	 * @param n input parameter
	 * @return true if n is power of 2
	 */
	public static boolean isPowerOf2(int n) {
		if (n <= 0)
			return false;
		else
			return (n & (n - 1)) == 0;
	}

	/**
	 * @return the forceThreads
	 */
	public static boolean isForceThreads() {
		return forceThreads;
	}

	/**
	 * @param forceThreads the forceThreads to set
	 */
	public static void setForceThreads(boolean forceThreads) {
		ConcurrencyUtils.forceThreads = forceThreads;
	}	
}
