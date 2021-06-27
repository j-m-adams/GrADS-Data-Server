/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.service;

import java.io.*;
import java.util.*;
import java.text.*;
import javax.servlet.http.*;

import org.iges.anagram.*;

/** Sends a complete listing of the server's contents in XML format.
 */
public class XMLCatalogService
    extends Service {

    public String getServiceName() {
	return "xml";
    }

    public void configure(Setting setting) {
    }

    public void handle(ClientRequest clientRequest)
	throws ModuleException {
	
	HttpServletRequest request = clientRequest.getHttpRequest();
	HttpServletResponse response = clientRequest.getHttpResponse();

	PrintWriter page;
	try {
	    page = 
		new PrintWriter
		    (new OutputStreamWriter
			(response.getOutputStream()));
	} catch (IOException ioe) { return;}
	    
	String baseURL = "http://" + 
	    request.getServerName() + ":" +
	    request.getServerPort() + 
		request.getContextPath();
	
	String servletURL = baseURL + request.getServletPath();
	
	StringBuffer buffer = new StringBuffer("");
	
	// Retrieve dataset list
	DirHandle root = (DirHandle)server.getCatalog().getLocked("/");
	
	Collection datasets = root.getEntries(true).values();
	
	response.setHeader("CacheControl", "no-cache");
	
	buffer.append("<?xml version=\"1.0\"?>\n" +
		      "  <serverdirectory count=\"");
	buffer.append(datasets.size());
	buffer.append("\">\n");
	
	Iterator it = datasets.iterator();
	int i = 1;
	while (it.hasNext()) {
	    Handle handle = (Handle)it.next();
	    if (handle instanceof DirHandle) {
		continue;
	    }
	    DataHandle dataset = (DataHandle)handle;
	    dataset.getSynch().lock();
	    buffer.append("    <dataset protocol=\"dods\" rank=\"");
	    buffer.append(i);
	    buffer.append("\">\n");
	    buffer.append("      <name>");
	    buffer.append(dataset.getCompleteName());
	    buffer.append("</name>\n");
	    buffer.append("      <description>");
	    buffer.append(dataset.getDescription());
	    buffer.append("</description>\n");

	    buffer.append("      <dods>");
	    buffer.append(baseURL);
	    buffer.append(dataset.getCompleteName());
	    buffer.append("</dods>\n");

	    buffer.append("      <dds>");
	    buffer.append(baseURL);
	    buffer.append(dataset.getCompleteName());
	    buffer.append(".dds</dds>\n");

	    buffer.append("      <das>");
	    buffer.append(baseURL);
	    buffer.append(dataset.getCompleteName());
	    buffer.append(".das</das>\n");

	    buffer.append("   </dataset>\n");
	    dataset.getSynch().release();
	    i++;
	}
	buffer.append("</serverdirectory>\n");
	
	root.getSynch().release();

	page.println(buffer);
	page.flush();
    }

}
