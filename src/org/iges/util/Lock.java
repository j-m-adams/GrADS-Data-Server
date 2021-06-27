/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.util;

import java.util.*;

/** A generalized representation of the thread-lock concept.
 *  This can be used to create other locking systems besides the basic 
 *  one-thread-at-a-time model of Java's <code>synchronized</code> keyword. 
 */
public interface Lock {

    /** @return True if the current thread owns this
     *  lock. */
    public boolean isLocked();
	
    /** Obtains this lock for the current thread, 
     *  blocking until the lock is available. */
    public void lock();
	    
    /** Releases this lock. If the current thread does not own this lock,
     *  does nothing. */
    public void release();
    
    /** Tries to obtain this lock for the current thread. 
     *  This method always returns immediately but does not 
     *  guarantee that the lock will be obtained.
     *  @return True if the lock was succesfully obtained. 
     */
    public boolean tryLock();

}
