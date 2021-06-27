/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.util;

import java.util.*;

/** An extension of the thread-lock concept
 *  which supports the concept of exclusive vs non-exclusive locking. 
 *  This concept is a way to eliminate unnecessary thread blocks 
 *  on operations that do not modify an object's state.<p>
 * 
 *  Non-exclusive locks may be simultaneously held on the same Locker by any 
 *  number of threads.<p>
 * 
 *  In contrast, only one thread at a time may hold an exclusive lock, 
 *  and the granting 
 *  of an exclusive lock guarantees that all non-exclusive locks have been
 *  released. <p>
 * 
 *  Furthermore, requests for exclusive locks take priority over
 *  requests for non-exclusive locks.<p>
 *  
 *  The envisioned use of this system is to require exclusive locks for all 
 *  state-change operations on an
 *  object, and non-exclusive locks for all state-query operations. It is then
 *  possible to guarantee that:<p>
 *  
 *  1) any number of threads may simultaneously query the object's state<br>
 *  2) no two threads can simultaneously attempt to change 
 *     the object's state<br>
 *  3) the object will never change state while it is being queried <br>
 *  4) any thread can temporarily block the initiation of new query operations
 *     in order to change the Object's state <p>
 *
 *  One important difference between the ExclusiveLock mechanism and Java's 
 *  <code>synchronized</code> blocks is that for efficiency reasons, 
 *  ExclusiveLock does not keep track of nested of lock/release operations.<p>
 *  It is therefore important to avoid redundant locking.
 *  For instance the following code will not work as desired:
 *  <code>
 *  protected ExclusiveLock synch = new ExclusiveLock();
 *  public void foo() {
 *    synch.lock();
 *    // do something
 *    synch.release();
 *  }
 *  
 *  public void bar() {
 *    synch.lock();
 *    // do op 1
 *    foo();            // lock has been released when foo returns!
 *    // do op 2        // op 2 is operating without a lock
 *    synch.release();  // not what we wanted..
 *  }
 *  </code>
 */
public class ExclusiveLock 
    implements Lock {

    /** Creates a new ExclusiveLock */
    public ExclusiveLock() {
	this.locks = new HashSet();
    }

    /** Creates a new ExclusiveLock with sufficient storage for 
     *  the expected maximum number of non-exclusive locks, to prevent
     *  frequent reallocation of internal storage.
     */
    public ExclusiveLock(int expectedMaxLocks) {
	locks = new HashSet(expectedMaxLocks);
    }

    /** Obtains an exclusive lock for the current thread, 
     *  blocking until the lock is available. Requests for non-exclusive locks
     *  will block starting from when this method is <i>called</i> (not  
     *  from when it returns), until the resulting exclusive
     *  lock is released. If the current thread 
     *  already owns an exclusive lock, does nothing. */
    public synchronized void lockExclusive() {
	if (isLockedExclusive()) {
	    if (DEBUG) debug("already have ex lock");
	    return;
	}
	while (exclusive != null) {
	    // exclusive block already exists, wait for release
	    if (DEBUG) debug("ex block exists for ex");
	    try {
		this.wait(0);
	    } catch (InterruptedException ie) {}
	}
	exclusive = Thread.currentThread();
	while (locks.size() > 0) {
	    if (DEBUG) debug("non-ex block exists for ex");
	    try {
		this.wait(0);
	    } catch (InterruptedException ie) {}
	}
	if (DEBUG) debug("ex lock established");
    }
	
    /** @return True if the current thread owns an exclusive
     *  lock. */
    public synchronized boolean isLockedExclusive() {
	return exclusive == Thread.currentThread();
    }
		
    /** Tries to obtain an exclusive lock for the current thread. 
     *  This method always returns immediately but does not 
     *  guarantee that the lock will be obtained. If the current thread 
     *  already owns an exclusive lock, does nothing.
     *  @return True if the exclusive lock was succesfully obtained. 
     */
    public synchronized boolean tryLockExclusive() {
	if (isLockedExclusive()) {
	    return true;
	}
	if (exclusive == null && locks.size() == 0) {
	    exclusive = Thread.currentThread();
	    if (DEBUG) debug("try for ex succeeded");
	    return true;
	} else {
	    if (DEBUG) debug("try for ex failed");
	    return false;
	}
    }

    /** Tries to obtain an exclusive lock for the current thread.
     *  This method will return within the timeout given, more or
     *  less, but does not guarantee that the lock will be
     *  obtained. If the current thread already owns an exclusive
     *  lock, does nothing.
     *  @param timeout Maximum time in milliseconds to wait for an
     *  exclusive lock
     *  @return True if the exclusive lock was succesfully obtained. 
     */
    public synchronized boolean tryLockExclusive(long timeout) {
	if (isLockedExclusive()) {
	    return true;
	}
	if (DEBUG) debug("trying " + timeout + "ms for ex");
	long start = System.currentTimeMillis();
	long remaining = timeout;
	while (remaining > 0) {
	    if (tryLockExclusive()) {
		return true;
	    }
	    if (DEBUG) debug("waiting " + remaining + "ms for ex");
	    try {
		this.wait(remaining);
	    } catch (InterruptedException ie) {}
	    remaining = timeout - (System.currentTimeMillis() - start);
	}
	if (DEBUG) debug("timed out waiting for ex");
	return false;
    }

    /** @return True if the current thread owns a non-exclusive lock. */
    public synchronized boolean isLocked() {
	return locks.contains(Thread.currentThread());
    }
	
    /** Obtains a non-exclusive lock for the current thread, 
     *  blocking until the lock is available. If the current thread already
     *  owns a non-exclusive lock, does nothing. */
    public synchronized void lock() {
	if (isLocked()) {
	    if (DEBUG) debug("already have non-ex lock");
	    return;
	}
	while (!tryLock()) {
	    if (DEBUG) debug("block for non-ex");
	    try {
		this.wait(0);
	    } catch (InterruptedException ie) {}
	}
	if (DEBUG) debug("non-ex lock established");
    }
	
    /** Releases the current thread's lock.
     *  If the current thread does not own a lock,
     *  does nothing. */
    public synchronized void release() {
	if (exclusive == Thread.currentThread()) {
	    exclusive = null;
	    if (DEBUG) debug("ex lock released; locks.size = " + locks.size());
	    this.notifyAll();
	} else {
	    locks.remove(Thread.currentThread());
	    if (DEBUG) debug("non-ex lock released; locks.size = " + locks.size());
	    if (locks.size() == 0) {
		this.notifyAll();
	    }
	}
    }
	
    /** Tries to obtain a non-exclusive lock for the current thread. 
     *  This method always returns immediately but does not 
     *  guarantee that the lock will be obtained. If the current thread 
     *  already owns a non-exclusive lock, does nothing.
     *  @return True if a non-exclusive was succesfully obtained. 
     */
    public synchronized boolean tryLock() {
	if (isLocked()) {
	    return true;
	}
	if (exclusive == null) {
	    locks.add(Thread.currentThread());
	    return true;
	} else {
	    return false;
	}
    }

    /** Prints a list of the threads that currently own locks on this 
     *  object. */
    public synchronized String toString() {
	StringBuffer sb = new StringBuffer();
	sb.append(hashCode());
	Iterator it = locks.iterator();
	if (exclusive != null) {
	    sb.append(" ex = ");
	    sb.append(exclusive.getName());
	} else if (locks.size() > 0) {
	    sb.append(" non-ex = [ ");
	    while (it.hasNext()) {
		sb.append(((Thread)it.next()).getName());
		sb.append(" ");
	    }
	    sb.append("]");
	} else {
	    sb.append(" no locks");
	}
	return sb.toString();
    }
    
    protected Thread exclusive;
    protected Set locks;

    

    //    private static boolean DEBUG = false;
    private static boolean DEBUG = false;

    private void debug(String msg) {
	System.err.println("(" + Thread.currentThread().getName() + "/" + 
			   (int)(System.currentTimeMillis() % 1e6) + ")" +
			   this + " --- " + msg);
    }

}
