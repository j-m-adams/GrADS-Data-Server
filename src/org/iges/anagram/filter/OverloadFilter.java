/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.filter;

import java.util.*;

import org.iges.anagram.*;

/** Tracks the number of simultaneous requests, and blocks those
 *  that exceed the server's limit. 
 */
public class OverloadFilter 
    extends Filter {

    public String getFilterName() {
	return "overload";
    }

    public void configure(Setting setting) 
	throws ConfigException {

	super.configure(setting);
	limit = (int)setting.getNumAttribute("limit", 0);
    }

    public void doFilter(ClientRequest clientRequest) 
	throws ModuleException {

	synchronized(this) {
	    if (limit > 0 && currentLoad >= limit) {
		fail("server is experiencing heavy load. " + 
		     "please try again later.");
	    }
	    currentLoad++;
	    if (debug()) debug("current load: " + currentLoad + " threads");
	    if (currentLoad > maxLoad) {
		maxLoad = currentLoad;
	    }
	} 
	
	try {
	    next.handle(clientRequest);
	} catch (ModuleException me) {
	    throw me;
	} catch (RuntimeException re) {
	    throw re;
	} catch (Error e) {
	    throw e;
	} finally {
	    synchronized(this) {
		currentLoad--;
	    }
	}
    }

    protected int limit;
    protected int currentLoad;
    protected int maxLoad;
    
}
