/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram;

import java.io.*;
import java.util.*;
import org.iges.util.*;

/** The base class for all Catalog entries. */
public abstract class Handle 
    implements Serializable{

    protected Handle(String completeName) 
	throws AnagramException {

	validateName(completeName);
	this.completeName = completeName;
	this.synch = new ExclusiveLock();
    }

    /** Returns the portion of the entry's complete name that follows the 
     *  final '/'.
     */
    public String getName() {
	int lastSlash = completeName.lastIndexOf('/');
	if (lastSlash < 0) {
	    lastSlash = 0;
	}
	return completeName.substring(lastSlash + 1);
    }

    /** Returns the full online name of this entry */
    public String getCompleteName() {
	return completeName;
    }

    public String toString() {
	return completeName;
    }

    /** Allows multiple threads to synchronize operations on this handle. 
     *  Before performing operations that depend on the state of the handle,
     *  a non-exclusive lock should always be obtained. 
     *  Before performing operations that alter the state of a handle,
     *  an exclusive lock should always be obtained.
     *  Locks that have been obtained must always be released (even if the synchronized
     *  operation throws an exception), or other requests may become deadlocked.
     */
    public ExclusiveLock getSynch() {
	if (synch == null) {
	    synch = new ExclusiveLock();
	}
	return synch;
    }
    
    protected void validateName(String name) 
	throws AnagramException {
	
    }

    protected String completeName;

    protected transient ExclusiveLock synch;

}
