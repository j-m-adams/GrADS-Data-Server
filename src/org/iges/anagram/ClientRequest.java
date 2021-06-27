/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram;

import java.net.*;
import javax.servlet.http.*;

import org.iges.anagram.service.Service;

/** Contains all information associated with a particular server request */
public class ClientRequest {

    /** Creates a new client request with the specified settings */
    public ClientRequest(HttpServletRequest httpRequest, 
			 HttpServletResponse httpResponse, 
			 Privilege privileges,
			 Service service,
			 String serviceName,
			 String dataPath,
			 Handle handle) {
	this.httpRequest = httpRequest;
	this.httpResponse = httpResponse;
	this.privileges = privileges;
	this.service = service;
	this.serviceName = serviceName;
	this.ce = httpRequest.getQueryString();
	if (ce != null) {
	    ce = URLDecoder.decode(ce);
	}
	this.dataPath = dataPath;
	this.handle = handle;

	buildSummary();

    }

    /** Returns the servlet request object associated with this request */
    public HttpServletRequest getHttpRequest() {
	return httpRequest;
    }

    /** Returns the servlet response object associated with this request */
    public HttpServletResponse getHttpResponse() {
	return httpResponse;
    }

    /** Returns the complete URL of this request including query string */
    public String getFullURL() {
	if (ce == null) {
	    return HttpUtils.getRequestURL(httpRequest).toString();
	} else {
	    return HttpUtils.getRequestURL(httpRequest).toString() +
		"?" + ce;
	}
    }

    /** Returns the URL of this request, minus the query string */
    public String getURL() {
	return HttpUtils.getRequestURL(httpRequest).toString();
    }

    /** Returns the catalog data path associated with this request */
    public String getDataPath() {
	return dataPath;
    }

    /** Returns the catalog entry associated with this request, if any */
    public Handle getHandle() {
	return handle;
    }

    /** Sets the catalog entry associated with this request */
    public void setHandle(Handle handle) {
	this.handle = handle;
    }

    /** Returns the name of the service associated with this request */
    public String getServiceName() {
	return serviceName;
    }

    /** Returns the service associated with this request, if any */
    public Service getService() {
	return service;
    }

    /** Returns the constraint expression associated with this request */
    public String getCE() {
	return ce;
    }

    /** Returns the privilege set associated with this request */
    public Privilege getPrivilege() {
	return privileges;
    }
    
    public String toString() {
	return summary;
    }

    /** Used in writing log messages */
    protected void buildSummary() {
	StringBuffer buf = new StringBuffer();
	buf.append("[ ");
	buf.append(Thread.currentThread().getName());
	buf.append(" ");
	buf.append(httpRequest.getRemoteHost());
	buf.append(" ");
	buf.append(httpRequest.getMethod());
	buf.append(" ");
	buf.append(httpRequest.getServletPath().replace(' ','+'));
	if(ce != null) {
	    buf.append("?");
	    buf.append(ce);
	}
	buf.append(" ] ");
	summary = buf.toString();
    }

    protected HttpServletRequest httpRequest;
    protected HttpServletResponse httpResponse;
    protected Handle handle;
    protected Service service;

    protected String dataPath;
    protected String serviceName;
    protected String ce;
    protected String summary;
    protected Privilege privileges;

}
