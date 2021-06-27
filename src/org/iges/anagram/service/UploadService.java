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

/** Receives client uploads of data objects */
public class UploadService
    extends Service {

    public String getServiceName() {
	return "upload";
    }

    public void configure(Setting setting) {
	generating = new ArrayList();
    }

    public void handle(ClientRequest clientRequest)
	throws ModuleException {
	
	String prefix = clientRequest.getDataPath();
	if (!prefix.startsWith("/_upload_")) {
	    throw new ModuleException(this,
				      "path for uploaded dataset must begin " +
				      "with '_upload_'");
	}

	String name;
	synchronized (generating) {
	    int counter = 0;
	    do {
		counter++;
		name = prefix + "_" + counter;
	    } while (server.getCatalog().contains(name) || 
		     generating.contains(name));
	    log.info(this, "storing upload: " + name);
	    generating.add(name);
	}
 
	HttpServletRequest request = clientRequest.getHttpRequest();
	HttpServletResponse response = clientRequest.getHttpResponse();

	try {
	
	    // Pass stream to cache to create dataset
	    long uploadSize = request.getIntHeader("Content-Length");
	    if (uploadSize <= 0) {
		throw new ModuleException(this,
					  "Content-Length <= 0 in request");
	    }

	    InputStream uploadStream;
	    try {
		uploadStream = request.getInputStream();
	    } catch (IOException ioe) {
		throw new ModuleException(this, "can't read request content");
	    }
	    
	    TempDataHandle handle = 
		server.getTool().doUpload(name, 
					  uploadStream, 
					  uploadSize, 
					  clientRequest.getPrivilege());
	    server.getCatalog().addTemp(handle);
	    
	    if (debug()) log.debug(this, clientRequest + 
				   "finished upload");
	    try {
		
		PrintWriter page = 
		    new PrintWriter
			(new OutputStreamWriter(response.getOutputStream()));
		page.println(name + " created successfully.");
		page.flush();
		
	    } catch (IOException ioe) {}

	    
	} catch (ModuleException me) {
	    throw me;
	} finally {
	    synchronized (generating) {
		generating.remove(name);
		generating.notifyAll();
	    }
	}
    }

    protected List generating;

}
