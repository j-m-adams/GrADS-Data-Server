/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram;

import java.io.*;
import java.util.*;

// Can't import * because Filter conflicts with org.iges.anagram.filter.Filter
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.iges.anagram.service.*;
import org.iges.anagram.filter.*;

/** The servlet component of the Anagram framework.<p>
 *
 *  This module implements the HTTP servlet interface, and thus handles
 *  all communication with the servlet container. 
 */
public class AnagramServlet 
    extends HttpServlet
    implements Module {

    // Module interface

    public String getModuleID() {
	return "servlet";
    }

    public void init(Server server, Module parent) {
	this.server = server;
	this.log = server.getLog();
	moduleName = parent.getModuleName() + "/" + getModuleID();
	dodsErrorServices = new HashSet(Arrays.asList(new String[]{
	    "dds", "das", "dods", "upload"
	}));
	dodsErrorHandler = new DODSErrorService();
	dodsErrorHandler.init(server, this);
	webErrorHandler = new WebErrorService();
	webErrorHandler.init(server, this);
	createFilters();
    }

    /** Called by init(Server, Module). Sets up filter chain. */
    protected void createFilters() {
	filters = new ArrayList();
	filters.add(new AnalysisFilter());
	filters.add(new AbuseFilter());
	filters.add(new OverloadFilter());
	filters.add(new DispatchFilter());
	Iterator it = filters.iterator();
	Filter next = (Filter)it.next();
	next.init(server, this);
	while (it.hasNext()) {
	    Filter current = next;
	    next = (Filter)it.next();
	    next.init(server, this);
	    current.setNext(next);
	}
    }

    public void configure(Setting setting)
	throws ConfigException {

	Iterator it = filters.iterator();
	try {
	    while (it.hasNext()) {
		Filter filter = (Filter)it.next();
		Setting filterSetting = 
		    setting.getUniqueSubSetting(filter.getModuleID());
		filter.configure(filterSetting);
	    }
	} catch (AnagramException ae) {
	    throw new ConfigException(this, ae.getMessage());
	}

    }

    /** Same as in AbstractModule. We can't use AbstractModule because
     * we have to inherit from HttpServlet.
     */
    public String getModuleName() {
	return moduleName;
    }

    /** Same as in AbstractModule. We can't use AbstractModule because
     * we have to inherit from HttpServlet.
     */
    protected boolean debug() {
	return log.enabled(Log.DEBUG, this);
    }

    /** Same as in AbstractModule. We can't use AbstractModule because
     * we have to inherit from HttpServlet.
     */
    protected boolean verbose() {
	return log.enabled(Log.VERBOSE, this);
    }


    // HttpServlet interface

    /** Called when the servlet is first loaded. Creates a
     *  Server object registered with this servlet. */		
    public void init(ServletConfig config) 
	throws ServletException {
	
	super.init(config);
	
	server = new Server(this, config);

    }


    /** Called when the server is shutting down */
    public void destroy() {
	server.destroy();
    }    

    /** Handles POST requests. This method simply passes the
     *  POST request to doGet().
     */
    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) {
	doGet(request, response);
    }

    /** Handles all incoming requests by doing the following:
     *  <list>
     *  <li>A non-exclusive lock on the server is obtained.</li>
     *  <li>A ClientRequest object is obtained from the Mapper module</li>
     *  <li>The request is logged and a timer is started.</li>
     *  <li>The request is passed to the head of the chain of Filters</li>
     *  <li>If an error occurs, the request is passed to the appropriate
     *   error handler.</li>
     *  <li>The total elapsed time is logged, the output stream is closed,
     *  and the server lock is released.</li>
     *  </list> 
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {

	// Set up for request handling

	if (debug()) log.debug(this, "getting lock for thread: " + 
			       Thread.currentThread().getName());

	server.getSynch().lock();

	long startTime = System.currentTimeMillis();
	boolean completed = false;
	
	ClientRequest clientRequest = 
	    server.getMapper().map(request, response);

	log.info(this, clientRequest + "STARTED");

	if (verbose()) log.verbose(this, clientRequest + "privilege is " + 
				   clientRequest.getPrivilege());
	if (debug()) log.debug(this, clientRequest + 
			       "service is " + 
			       clientRequest.getService());
	if (debug()) log.debug(this, clientRequest + 
			       "handle is " + 
			       clientRequest.getDataPath());

	// Handle request
	try {
	    if (debug()) log.debug(this, clientRequest + 
				   "running filters");
	    /*
	    Iterator it = filters.iterator();
	    while (it.hasNext()) {
		Filter next = (Filter)it.next();
		if (next.isEnabled()) {
		    if (debug()) log.debug(this, "applying filter: " + next);
		    next.handle(clientRequest);
		} else {
		    if (debug()) log.debug(this, "skipping filter: " + next);
		}
	    }
	    */
	    Filter head = (Filter)filters.get(0);
	    head.handle(clientRequest);
	    	    
	    completed = true;

	} catch (ModuleException me) {
	    findErrorHandler(clientRequest).handle(clientRequest, me);
	} catch (Throwable t) {
	    findErrorHandler(clientRequest).handleUnexpected(clientRequest, t);
	} 
	
	long endTime = System.currentTimeMillis();
	String completedMsg = (completed) ? "success" : "error";
	log.info(this, clientRequest + "FINISHED (" + 
		 completedMsg + " in " +
		 (endTime - startTime) + "ms)");

	// Release resources
	try {
	    clientRequest.getHttpResponse().getOutputStream().close();
	} catch (IOException ioe) {}


	if (clientRequest.getHandle() != null) {
	    if (debug()) log.debug(this, "releasing lock for handle: " + 
				   clientRequest.getHandle());
	    clientRequest.getHandle().getSynch().release();
	}

	if (debug()) log.debug(this, "releasing lock for thread: " + 
			       Thread.currentThread().getName());

	server.getSynch().release();

	if (debug()) log.debug(this, 
			       "returning control to Tomcat on thread: " + 
			       Thread.currentThread().getName());

    }

    /** Returns an error service which uses the appropriate format based
     *  on the type of request.
     */
    protected ErrorService findErrorHandler(ClientRequest request) {
	if (request.getServiceName() != null && 
	    dodsErrorServices.contains(request.getServiceName())) {
	    return dodsErrorHandler;
	} else {
	    return webErrorHandler;
	}
    }


    protected String moduleName;
    protected Server server;
    protected Log log;

    protected Set dodsErrorServices;

    protected ErrorService dodsErrorHandler;
    protected ErrorService webErrorHandler;

    protected List filters;

}
