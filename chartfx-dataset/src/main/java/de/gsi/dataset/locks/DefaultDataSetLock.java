package de.gsi.dataset.locks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import de.gsi.dataset.DataSet;

/**
 * A Simple ReadWriteLock for the DataSet interface and its fluent-design approach Some implementation recommendation:
 * write lock guards behave the same as ReentrantLock with the additional functionality, that a <code>writeLock()</code>
 * and subsequent <code>writeUnLock()</code> mute and, respectively, un-mute the given DataSet's auto-notification
 * states, e.g. example:
 *
 * <pre>
 *  lock.writeLock(); // stores isAutoNotification state
 *     [..] some other code [..]
 *  lock.writeUnLock(); // restores isAutoNotification state
 * </pre>
 * <p>
 * However, the recommended usage is using the lock guard primitives, e.g.
 *
 * <pre>
 * lock.readLockGuard(() -&gt; {
 *    [..] some read-lock protected code [..]
 *    return retVal; // N.B. optional return - here: assumes Objects or boxed primitives
 * });
 * </pre>
 * <p>
 * Alternatively the best performing option for frequent simple reads without major data processing
 *
 * <pre>
 * Result ret = lock.readLockGuardOptimistic(() -&gt; {
 *    [..] some read-lock protected code [..]
 * 	  return retVal; // N.B. optional return - here: assumes Objects or boxed primitives
 * });
 * </pre>
 * <p>
 * The latter assumes infrequent writes (e.g. a single writer thread) and frequent unobstructed reads (ie. many reader
 * threads). The lock internally acquires the data w/o explicitly locking, checks afterwards if the data has potentially
 * changed a write-lock acquiring thread, and as a automatic fall-back uses the guaranteed (but more expensive) read
 * lock to assure that the read data structure is consistent.
 *
 * @param <D> generics reference, usually to <code>&lt;? extends DataSet&gt;</code>
 * @author rstein
 */
@SuppressWarnings({ "PMD.DoNotUseThreads", "PMD.CommentSize", "PMD.TooManyMethods" })
// Runnable used as functional interface
public class DefaultDataSetLock<D extends DataSet> implements DataSetLock<D> {
    private static final long serialVersionUID = 1L;
    private final transient StampedLock stampedLock = new StampedLock();
    private transient long lastReadStamp;
    private final AtomicLong lastWriteStamp = new AtomicLong(-1L);
    private final AtomicLong writerLockedByThreadId = new AtomicLong(-1L);
    private final transient Object readerCountLock = new Object();
    private int readerCount;
    private final AtomicInteger writerCount = new AtomicInteger(0);
    private final transient AtomicBoolean autoNotifyState = new AtomicBoolean(true);
    private final transient D dataSet;

    /**
     * @param dataSet dataSet this set is associate with
     */
    public DefaultDataSetLock(final D dataSet) {
        this.dataSet = dataSet;
        if (dataSet == null) {
            throw new IllegalArgumentException("dataSet must not be null");
        }
    }

    /**
     * experimental down-grading of the writer lock
     *
     * @return corresponding data set
     * @deprecated do not use (yet)
     */
    @Deprecated(since = "still under test")
    public D downGradeWriteLock() {
        if (!stampedLock.isWriteLocked()) {
            throw new IllegalStateException("cannot down-convert lock - lock is not write locked");
        }
        if (getWriterCount() > 1) {
            throw new IllegalStateException("cannot down-convert lock - holding n write locks = " + getWriterCount());
        }
        final long result = stampedLock.tryConvertToReadLock(lastWriteStamp.get());
        if (result == 0L) { // NOPMD to be expected return value from 'tryConvertToReadLock'
            throw new IllegalStateException("cannot down-convert lock - tryConvertToReadLock return '0'");
        }
        readerCount++;
        writerCount.decrementAndGet();
        if ((lastReadStamp == 0) && stampedLock.isReadLocked() && (getReaderCount() > 1)) {
            stampedLock.unlockRead(lastReadStamp);
        }
        lastReadStamp = result;

        return dataSet;
    }

    /**
     * @return number of readers presently locked on this data set - this counts only (deprecated) readers using read(Un)Lock()
     */
    public int getReaderCount() {
        synchronized (readerCountLock) {
            return readerCount;
        }
    }

    /**
     * @return number of writers presently locked on this data set (N.B. all from the same thread)
     */
    public int getWriterCount() {
        return writerCount.get();
    }

    @Override
    public D readLock() {
        synchronized (readerCountLock) {
            if (readerCount == 0) {
                lastReadStamp = stampedLock.readLock();
            }
            readerCount++;
        }

        return dataSet;
    }

    @Override
    public D readLockGuard(final Runnable reading) {
        final long stamp = stampedLock.readLock();
        try {
            reading.run();
        } finally {
            stampedLock.unlockRead(stamp);
        }
        return dataSet;
    }

    @Override
    public <R> R readLockGuard(final Supplier<R> reading) {
        R result;
        final long stamp = stampedLock.readLock();
        try {
            result = reading.get();
        } finally {
            stampedLock.unlockRead(stamp);
        }
        return result;
    }

    @Override
    public D readLockGuardOptimistic(final Runnable reading) { // NOPMD -- runnable not used in a thread context
        final long stamp = stampedLock.tryOptimisticRead();
        reading.run();
        if (stampedLock.validate(stamp)) {
            return dataSet;
        }
        final long stampHard = stampedLock.readLock();
        try {
            reading.run();
        } finally {
            stampedLock.unlockRead(stampHard);
        }
        return dataSet;
    }

    @Override
    public <R> R readLockGuardOptimistic(final Supplier<R> reading) {
        R result = reading.get();
        // try optimistic read
        final long stamp = stampedLock.tryOptimisticRead();
        if (stampedLock.validate(stamp)) {
            return result;
        }
        // fallback to blocking read
        final long stampHard = stampedLock.readLock();
        try {
            result = reading.get();
        } finally {
            stampedLock.unlockRead(stampHard);
        }
        return result;
    }

    @Override
    public D readUnLock() {
        synchronized (readerCountLock) {
            readerCount--;
            if (readerCount == 0) {
                stampedLock.unlockRead(lastReadStamp);
                lastReadStamp = 0L;
            } else if (readerCount < 0) {
                throw new IllegalStateException("read lock already unlocked");
            }
        }

        return dataSet;
    }

    @Override
    public D writeLock() {
        final long callingThreadId = Thread.currentThread().getId();
        if (writerLockedByThreadId.get() != callingThreadId) {
            // wrong not matching thread - need to acquire lock
            long stamp;
            do {
                stamp = stampedLock.tryWriteLock();
                // stamp = stampedLock.writeLock() - would be the natural choice (performance) but blocks in write-lock racing conditions
            } while (stamp == 0);
            // acquired lock
            writerLockedByThreadId.set(callingThreadId);
            lastWriteStamp.set(stamp);
            autoNotifyState.set(dataSet.autoNotification().getAndSet(false));
        }
        // we acquired a new lock or are already owner of a previously acquired lock
        writerCount.incrementAndGet();
        return dataSet;
    }

    @Override
    public D writeLockGuard(final Runnable writing) { // NOPMD -- runnable not used in a thread context
        writeLock();
        final boolean oldAutoNotificationState = dataSet.autoNotification().getAndSet(false);

        try {
            writing.run();
        } finally {
            dataSet.autoNotification().set(oldAutoNotificationState);
            writeUnLock();
        }
        return dataSet;
    }

    @Override
    public <R> R writeLockGuard(final Supplier<R> writing) {
        writeLock();
        final boolean oldAutoNotificationState = dataSet.autoNotification().getAndSet(false);

        R result;
        try {
            result = writing.get();
        } finally {
            dataSet.autoNotification().set(oldAutoNotificationState);
            writeUnLock();
        }
        return result;
    }

    @Override
    public D writeUnLock() {
        if (writerCount.decrementAndGet() == 0) {
            final long callingThreadId = Thread.currentThread().getId();
            if (writerLockedByThreadId.get() != callingThreadId) {
                throw new IllegalStateException("unlock attempt by tid = " + callingThreadId + " (" + Thread.currentThread() + ") - but locked by " + writerLockedByThreadId.get());
            }

            // restore present auto-notify state
            dataSet.autoNotification().set(autoNotifyState.get());
            writerLockedByThreadId.set(-1L);
            stampedLock.unlockWrite(lastWriteStamp.getAndSet(-1L));
        }
        return dataSet;
    }
}
