/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.filter;

import org.iges.anagram.*;

/** A module that performs a step in the handling of a client request. */
public abstract class Filter 
    extends AbstractModule {

    public String getModuleID() {
	if (moduleID == null) {
	    moduleID = "filter-" + getFilterName();
	}
	return moduleID;
    }

    public void configure(Setting setting) 
	throws ConfigException {
	enabled = setting.getAttribute("enabled", "true").equals("true");
	if (debug()) debug(getModuleID() + " enabled = " + enabled);
    }
	

    /** The name of this filter. Used to build the module ID */
    public abstract String getFilterName();

    /** Sets the filter that this filter should pass requests to */
    public void setNext(Filter next) {
	this.next = next;
    }

    /** Sets whether this filter is enabled */
    public void setEnabled(boolean enabled) {
	this.enabled = enabled;
    }

    /** Indicates whether this filter is enabled. Requests should not
     *  be sent to a filter that is not enabled. */
    public boolean isEnabled() {
	return enabled;
    }

    /** Handles a client request. 
     *  Unless the filter is throwing an exception it should
     *  pass the request on to <code>next.handle()</code>. 
     *  If the <code>enabled</code> property
     *  is set to false, this method should perform no action, and
     *  always pass requests directly to <code>next.handle()</code>.
     */
    public void handle(ClientRequest request) 
	throws ModuleException {
	
	if (enabled) {
	    if (debug()) debug("running " + getModuleID());
	    doFilter(request);
	} else {
	    if (debug()) debug("skipping " + getModuleID());
	    if (next != null) {
		next.handle(request);
	    }
	}
    }

    protected abstract void doFilter(ClientRequest request)
	throws ModuleException;

    protected Filter next;
    protected String filterName;
    protected String moduleID;

    protected boolean enabled = true;

}
