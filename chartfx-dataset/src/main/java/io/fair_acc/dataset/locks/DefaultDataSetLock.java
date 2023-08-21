package io.fair_acc.dataset.locks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.profiler.DurationMeasure;
import io.fair_acc.dataset.profiler.Profiler;

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
    private final AtomicLong lastReadStamp = new AtomicLong(-1L);
    private final AtomicLong lastWriteStamp = new AtomicLong(-1L);
    private final AtomicLong writerLockedByThreadId = new AtomicLong(-1L);
    private final AtomicInteger readerCount = new AtomicInteger(0);
    private final AtomicInteger writerCount = new AtomicInteger(0);
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
        readerCount.incrementAndGet();
        writerCount.decrementAndGet();
        if ((lastReadStamp.get() == 0) && stampedLock.isReadLocked() && (readerCount.get() > 1)) {
            stampedLock.unlockRead(lastReadStamp.get());
        }
        lastReadStamp.set(result);

        return dataSet;
    }

    /**
     * @return number of readers presently locked on this data set - this counts only (deprecated) readers using read(Un)Lock()
     */
    public int getReaderCount() {
        return readerCount.get();
    }

    /**
     * @return number of writers presently locked on this data set (N.B. all from the same thread)
     */
    public int getWriterCount() {
        return writerCount.get();
    }

    @Override
    public D readLock() {
        benchReadLock.start();
        if (lastReadStamp.get() == -1 && readerCount.get() == 0) {
            // first reader needs to acquire a lock to guard against writes
            final long stamp = stampedLock.readLock();
            if (lastReadStamp.compareAndExchange(-1, stamp) != -1) {
                // meanwhile already locked by another thread
                stampedLock.unlockRead(stamp);
            }
        }
        // other readers just increment the reader lock
        readerCount.getAndIncrement();
        benchReadLock.stop();
        return dataSet;
    }

    @Override
    public D readLockGuard(final Runnable reading) {
        readLock();
        try {
            reading.run();
        } finally {
            readUnLock();
        }
        return dataSet;
    }

    @Override
    public <R> R readLockGuard(final Supplier<R> reading) {
        R result;
        readLock();
        try {
            result = reading.get();
        } finally {
            readUnLock();
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
        readLock();
        try {
            reading.run();
        } finally {
            readUnLock();
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
        readLock();
        try {
            result = reading.get();
        } finally {
            readUnLock();
        }
        return result;
    }

    @Override
    public D readUnLock() {
        if (readerCount.get() == 1 && lastReadStamp.get() != -1) {
            final long lastReadStampLocal = lastReadStamp.get();
            //noinspection StatementWithEmptyBody
            if (lastReadStamp.compareAndExchange(lastReadStampLocal, -1L) != lastReadStampLocal) { // NOPMD NOSONAR - for better logic readability (humans)
                // already unlocked by another thread
            } else {
                // last reader needs to release the lock that guards against writes
                stampedLock.unlockRead(lastReadStampLocal);
            }
        }
        if (readerCount.decrementAndGet() < 0) {
            throw new IllegalStateException("read lock/unlock mismatch - already unlocked");
        }
        return dataSet;
    }

    @Override
    public D writeLock() {
        benchWriteLock.start();
        final long callingThreadId = Thread.currentThread().getId();
        if (writerLockedByThreadId.get() != callingThreadId) {
            // new/not matching existing thread holding lock - need to acquire new lock
            long stamp;
            do {
                //stamp = stampedLock.tryWriteLock()
                stamp = stampedLock.writeLock();
            } while (stamp == 0);
            // acquired lock
            writerLockedByThreadId.set(callingThreadId);
            lastWriteStamp.set(stamp);
        }
        // we acquired a new lock or are already owner of a previously acquired lock
        writerCount.incrementAndGet();
        benchWriteLock.stop();
        return dataSet;
    }

    @Override
    public D writeLockGuard(final Runnable writing) { // NOPMD -- runnable not used in a thread context
        writeLock();
        try {
            writing.run();
        } finally {
            writeUnLock();
        }
        return dataSet;
    }

    @Override
    public <R> R writeLockGuard(final Supplier<R> writing) {
        writeLock();
        R result;
        try {
            result = writing.get();
        } finally {
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
            writerLockedByThreadId.set(-1L);
            stampedLock.unlockWrite(lastWriteStamp.getAndSet(-1L));
        }
        return dataSet;
    }

    @Override
    public void setProfiler(Profiler profiler) {
        benchReadLock = profiler.newTraceDuration("lock-readLock");
        benchWriteLock = profiler.newTraceDuration("lock-writeLock");
    }

    private DurationMeasure benchReadLock = DurationMeasure.DISABLED;
    private DurationMeasure benchWriteLock = DurationMeasure.DISABLED;

}
