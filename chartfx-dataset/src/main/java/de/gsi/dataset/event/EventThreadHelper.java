package de.gsi.dataset.event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author rstein
 *
 */
public class EventThreadHelper {
	private static final int MAX_THREADS = Math.max(4, Runtime.getRuntime().availableProcessors());
	private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(2 * MAX_THREADS);
	
	/**
	 * @return maximum number of threads used for event notification
	 */
	public static int getMaxThreads() {
		return MAX_THREADS;
	}
	
	/**
	 * @return event update executor service
	 */
	public static ExecutorService getExecutorService() {
		return EXECUTOR_SERVICE;
	}
}
