/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram;

import java.util.*;

/** Represents a sub-directory of the server catalog. 
 *  Directory names do not end with '/'. 
 *  The one exception is that the root directory of the catalog has 
 *  the name "/". 
 */
public class DirHandle
    extends Handle {

    public DirHandle(String completeName)
	throws AnagramException {

	super(completeName);
	this.entries = new TreeMap();
    }

    /** Used by Catalog */
    void add(Handle handle) {
	entries.put(handle.getCompleteName(), handle);
    }

    /** Used by Catalog */
    void remove(String completeName) {
	entries.remove(completeName);
    }

    /** Returns true if this directory contains a handle that
     *  matches the name given */     
    public boolean contains(String completeName) {
	return entries.keySet().contains(completeName);
    }

    
    /** Used by Catalog */
    Handle get(String completeName) {
	return (Handle)entries.get(completeName);
    }

    /** Returns a Map containing all entries in this directory.
     *  The keys are the names of the entries, and the values are the
     *  Handle objects associated with those names.
     *  @param recurse If true, the Map will also contain all entries in all
     *   sub-directories. In this case, handles for the subdirectories 
     *   themselves will be omitted.
     */
    public Map getEntries(boolean recurse) {
	if (recurse) {
	    SortedMap recursedEntries = new TreeMap();
	    Iterator it = entries.values().iterator();
	    while (it.hasNext()) {
		Handle next = (Handle)it.next();
		if (next instanceof DirHandle) {
		    recursedEntries.putAll(((DirHandle)next).getEntries(true));
		} else {
		    recursedEntries.put(next.getCompleteName(), next);
		}
	    }
	    return recursedEntries;
	} else {
	    return entries;
	}
    }

    protected SortedMap entries;

}
