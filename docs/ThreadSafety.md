# Thread-Safety and Concurrency in ChartFX (outdated)

Large data sets are typically -- or better 'should be' -- primarily updated in separate data-acquisition or processing threads in order not to block or overload JavaFX's main rendering thread.
The `[DataSet](chartfx-dataset/src/main/java/io/fair_acc/dataset/DataSet.java)` interface thus deploys for this purpose a Read-Write-Lock pair (interface: `[DataSetLock](master/chartfx-dataset/src/main/java/io/fair_acc/dataset/locks/DataSetLock.java)`) that facilitates the thread-safety of the UI and non-UI parts of the ChartFx library.

While the user is free to provide null or custom implementations variants when implementing custom DataSets, all default data sets (notably their setters) use the [DefaultDataSetLock](chartfx-dataset/src/main/java/io/fair_acc/dataset/locks/DefaultDataSetLock.java), that implements a mutually exclusive pair of 'multi-reader' and 'reentrant single-writer' lock pair.
Ie. while the same writer thread can hold multiple (also recursive) write-locks, it may not hold a read-lock on the same object. Same applies vice-versa for the read-locks.

**Important design choices taken:**
* DataSet write-lock event auto-notification suppression: the write-lock pauses event auto-notification (ie. `dataSet.autonotifcation().set(false)`) while holding the lock and re-enables its old state (ie. `dataSet.autonotifcation().set(oldState)`). This default behaviour was done since
   a) most write protected code usually modifies more than one aspects of the data set (e.g. adding of multiple points, changing of multiple styles or labels, etc.) that would otherwise individually trigger a data set update and thus cause an unnecessary event notification avalanche, and also
   b) because of the nature of the compound data set notification event being usually different than its individual constituents.
* DataSet read-protection in user-code: while all relevant DataSet setters are write-lock protected, the corresponding getters are not and need to be locked in the user-/library-code that uses them. This was done to minimise the risks of hot-spots and premature optimisation.
* Exclusive read/write locks: In code protected by a write lock, it is not possible (and also not needed) to acquire a read lock. Trying to do so will result in a dead-lock.

**Usage Examples:**

In order to acquire an exclusive write-lock:

 ```Java
    dataSet.lock().writeLock();
        // [..] execute write protected code [..]
    dataSet.lock().writeUnLock();
    dataSet.fireInvalidated(new AddedDataEvent(this)); // optional
 ```
or via the recommended short-hand form also used in most parts of the library:

 ```Java
    dataSet.lock().writeLockGuard(() -> {
        // [..] execute write protected code [..]
    });
    dataSet.fireInvalidated(new RemovedDataEvent(this, "clearData()"));
 ```
It is strongly recommended to use the latter version, since this is less error-prone w.r.t. non-matching lock-unlock pairs, particularly for cases with early returns and/or in cases where exceptions could be thrown.

The read-lock can be used similarly. Their preferred usage is:

 ```Java
    dataSet.lock().readLockGuard(() -> {
        // [..] some read-lock protected code [..]
    });
 ```
or alternatively

 ```Java
    Result ret = lock.readLockGuardOptimistic(() -> {
        // [..] some read-lock protected code [..]
        return retVal; // optional return
    });
 ```
The latter is the best performing option for frequent simple reads without major data processing and assumes infrequent writes (e.g. a single writer thread) and frequent unobstructed reads (ie. many reader threads).
The lock internally acquires the data w/o explicitly locking, checks afterwards if the data has potentially been changed by a write-lock acquiring thread, and as an automatic fall-back uses the guaranteed (but more expensive) read lock to assure that the read data structure is consistent.
