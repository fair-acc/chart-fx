package de.gsi.dataset.locks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;

/**
 * A Simple ReadWriteLock for the DataSet interface and its fluent-design
 * approach
 * 
 * Some implementation recommendation: write lock guards behave the same as
 * ReentrantLock with the additional functionality, that a
 * <code>writeLock()</code> and subsequent <code>writeUnLock()</code> mute and,
 * respectively, un-mute the given DataSet's auto-notification states, e.g.
 * example:
 * 
 * <pre>
 *  lock.writeLock(); // stores isAutoNotification state
 *     [..] some other code [..]
 *  lock.writeUnLock(); // restores isAutoNotification state
 * </pre>
 * 
 * However, the recommended usage is using the lock guard primitives, e.g.
 * 
 * <pre>
 * lock.readLockGuard(() -&gt; {
 *    [..] some read-lock protected code [..]
 *    return retVal; // N.B. optional return - here: assumes Objects or boxed primitives
 * });
 * </pre>
 *
 * Alternatively the best performing option for frequent simple reads without
 * major data processing
 * 
 * <pre>
 * Result ret = lock.readLockGuardOptimistic(() -&gt; {
 *    [..] some read-lock protected code [..]
 * 	  return retVal; // N.B. optional return - here: assumes Objects or boxed primitives
 * });
 * </pre>
 * 
 * The latter assumes infrequent writes (e.g. a single writer thread) and
 * frequent unobstructed reads (ie. many reader threads). The lock internally
 * acquires the data w/o explicitly locking, checks afterwards if the data has
 * potentially changed a write-lock acquiring thread, and as a automatic
 * fall-back uses the guaranteed (but more expensive) read lock to assure that
 * the read data structure is consistent.
 * 
 * @author rstein
 * @param <D> generics reference, usually to
 *            <code>&lt;? extends DataSet&gt;</code>
 */
@SuppressWarnings({"PMD.DoNotUseThreads", "PMD.CommentSize"}) // Runnable used as functional interface
public class DefaultDataSetLock<D extends DataSet> implements DataSetLock<D> {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataSetLock.class);
	private final transient StampedLock stampedLock = new StampedLock();
	private transient long lastReadStamp;
	private transient long lastWriteStamp;
	private transient Thread writeLockThread; // NOPMD
	private final transient AtomicInteger readerCount = new AtomicInteger(0);
	private final transient AtomicInteger writerCount = new AtomicInteger(0);
	private transient boolean autoNotifyState;
	private final transient D dataSet;

	/**
	 * @param dataSet dataSet this set is associate with
	 */
	public DefaultDataSetLock(final D dataSet) {
		this.dataSet = dataSet;
		if (dataSet == null) {
			throw new IllegalArgumentException("dataSet must not be null");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.atDebug().log("init lock");
		}
	}

	/**
	 * @return last reader stamp
	 * @see java.util.concurrent.locks.StampedLock
	 */
	public long getLastReadStamp() {
		return lastReadStamp;
	}

	/**
	 * @return the last stored auto-notification state
	 */
	public boolean getLastStoredAutoNotificationState() { // NOPMD
		return autoNotifyState;
	}

	/**
	 * @return last writer stamp
	 * @see java.util.concurrent.locks.StampedLock
	 */
	public long getLastWriteStamp() {
		return lastWriteStamp;
	}

	/**
	 * @return the internal StampedLock object
	 */
	public StampedLock getLockObject() {
		return stampedLock;
	}

	/**
	 * @return number of readers presently locked on this data set
	 */
	public int getReaderCount() {
		return readerCount.get();
	}

	/**
	 * @return number of writers presently locked on this data set (N.B. all from
	 *         the same thread)
	 */
	public int getWriterCount() {
		return writerCount.get();
	}

	@Override
	public D readLock() {
		if (readerCount.getAndIncrement() == 0) {
			lastReadStamp = stampedLock.readLock();
		}

		return dataSet;
	}

	@Override
	public D readLockGuard(final Runnable reading) {
		long stamp = stampedLock.readLock();
		try {
			reading.run();
		} finally {
			stampedLock.unlockRead(stamp);
		}
		return dataSet;
	}

	@Override
	public <R> R readLockGuard(final Supplier<R> reading) {
		long stamp = stampedLock.readLock();
		R result;
		try {
			result = reading.get();
		} finally {
			stampedLock.unlockRead(stamp);
		}
		return result;
	}

	@Override
	public D readLockGuardOptimistic(final Runnable reading) { // NOPMD -- runnable not used in a thread context
		long stamp = stampedLock.tryOptimisticRead();
		reading.run();
		if (!stampedLock.validate(stamp)) {
			stamp = stampedLock.readLock();
			try {
				reading.run();
			} finally {
				stampedLock.unlockRead(stamp);
			}
		}
		return dataSet;
	}

	@Override
	public <R> R readLockGuardOptimistic(final Supplier<R> reading) {
		long stamp = stampedLock.tryOptimisticRead();
		R result = reading.get();
		if (!stampedLock.validate(stamp)) {
			stamp = stampedLock.readLock();
			try {
				result = reading.get();
			} finally {
				stampedLock.unlockRead(stamp);
			}
		}
		return result;
	}

	@Override
	public D readUnLock() {
		if (readerCount.decrementAndGet() == 0) {
			stampedLock.unlockRead(lastReadStamp);
		} else if (readerCount.get() < 0) {
			throw new IllegalStateException("read lock alread unlocked");
		}

		return dataSet;
	}

	@Override
	public D writeLock() {
		if (!stampedLock.isWriteLocked()) {
			lastWriteStamp = stampedLock.writeLock();
			writeLockThread = Thread.currentThread();
			// copy threadID
		}

		if (writeLockThread != Thread.currentThread()) {
			lastWriteStamp = stampedLock.writeLock();
			writeLockThread = Thread.currentThread();
		}

		if (writerCount.getAndIncrement() == 0) {
			// store present auto-notify state
			autoNotifyState = dataSet.isAutoNotification();
			dataSet.setAutoNotifaction(false);
		}

		return dataSet;
	}

	@Override
	public D writeLockGuard(final Runnable writing) { // NOPMD -- runnable not used in a thread context
		writeLock();
		final boolean oldAutoNotificationState = dataSet.isAutoNotification();
		dataSet.setAutoNotifaction(false);

		try {
			writing.run();
		} finally {
			dataSet.setAutoNotifaction(oldAutoNotificationState);
			writeUnLock();
		}
		return dataSet;
	}

	@Override
	public <R> R writeLockGuard(final Supplier<R> writing) {
		writeLock();
		final boolean oldAutoNotificationState = dataSet.isAutoNotification();
		dataSet.setAutoNotifaction(false);

		R result;
		try {
			result = writing.get();
		} finally {
			dataSet.setAutoNotifaction(oldAutoNotificationState);
			writeUnLock();
		}
		return result;
	}

	@Override
	public D writeUnLock() {
		if (writerCount.decrementAndGet() == 0) {
			// restore present auto-notify state
			dataSet.setAutoNotifaction(autoNotifyState);
			final long temp = lastWriteStamp;
			lastWriteStamp = 0;
			writeLockThread = null; // NOPMD
			stampedLock.unlockWrite(temp);

		} else if (writerCount.get() < 0) {
			throw new IllegalStateException("write lock alread unlocked");
		}
		return dataSet;
	}
}
