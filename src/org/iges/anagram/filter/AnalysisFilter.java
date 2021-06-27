/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.filter;

import java.util.*;

import org.iges.anagram.*;
import org.iges.anagram.service.*;
    
/** Performs analysis for requests that contain analysis expressions.
 */
public class AnalysisFilter 
    extends Filter {

    protected static String ANALYSIS_PREFIX = "/_expr_";

    public AnalysisFilter() {
	generating = new LinkedList();
	applicable = Arrays.asList(new String[] {
	    "dds", "das", "dods", "info", "asc", "ascii"
	});
    }

    public String getFilterName() {
	return "analysis";
    }

    protected void doFilter(ClientRequest clientRequest) 
	throws ModuleException {

	if (applicable.contains(clientRequest.getServiceName()) &&
	    clientRequest.getHandle() == null &&
	    clientRequest.getDataPath().startsWith(ANALYSIS_PREFIX)) {

	    doAnalysis(clientRequest);
	} else {
	    if (debug()) debug("no analysis to do");
	}

	next.handle(clientRequest);
	
    }

    protected void doAnalysis(ClientRequest clientRequest) 
	throws ModuleException {

	String name = clientRequest.getDataPath();
	String ae = name.substring(ANALYSIS_PREFIX.length());

	if (debug()) log.debug(this, 
			       clientRequest + 
			       "doing analysis for expression " +
			       ae);

	synchronized (generating) {
	    while (generating.contains(name)) {
		if (debug()) log.debug(this, clientRequest + 
				       "waiting for analysis to complete");
		try {
		    generating.wait(0);
		} catch (InterruptedException ie) {}
		
	    }
	    /*
	    if (server.getCatalog().contains(name)) {
		if (debug()) log.debug(this, clientRequest + 
				       "analysis result already in cache");
		return;
	    } 
	    */
	    log.info(this, "evaluating analysis expression: " + ae);
	    generating.add(name);
	}
	
	try {
	    TempDataHandle result = 
		server.getTool().doAnalysis(name, ae, 
					    clientRequest.getPrivilege());
	    
	    server.getCatalog().addTemp(result);
	    clientRequest.setHandle(server.getCatalog().getLocked(clientRequest.getDataPath()));
	    
	    if (debug()) log.debug(this, clientRequest + 
				   "finished analysis");

	} catch (ModuleException me) {
	    throw me;
	} finally {
	    synchronized (generating) {
		generating.remove(name);
		generating.notifyAll();
	    }
	}
    }


    protected List applicable;

    protected List generating;

}
