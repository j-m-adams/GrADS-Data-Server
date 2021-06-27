/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.service;

import java.io.*;
import java.util.Date;
import java.util.zip.*;
import javax.servlet.http.*;

import dods.dap.Server.ServerDDS;

import org.iges.anagram.*;

/** Provides data subsets in DODS binary format */
public class BinaryDataService 
    extends Service {

    public String getServiceName() {
	return "binary";
    }

    public void configure(Setting setting) {
    }

    public void handle(ClientRequest clientRequest)
	throws ModuleException {

	HttpServletRequest request = clientRequest.getHttpRequest();
	HttpServletResponse response = clientRequest.getHttpResponse();
	
	if (clientRequest.getCE() == null) {
	    throw new ModuleException
		(this, "subset requests must include a constraint expression");
	}

	DataHandle data = getDataFromPath(clientRequest);

	response.setContentType("application/octet-stream");
	response.setHeader("XDODS-Server", "dods/3.2");
	response.setHeader("XDAP", "3.2");
	response.setHeader("Content-Description", "dods_data");
	response.setDateHeader("Last-Modified", data.getCreateTime());
	
	String encoding = request.getHeader("Accept-Encoding");
	boolean useCompressed =
  	    (encoding != null) 
  	    && encoding.equalsIgnoreCase("deflate");

	try {
	
	    OutputStream dataOut;
	    if (useCompressed){
		if (debug()) log.debug(this, clientRequest + 
				       "using compression");
		response.setHeader("Content-Encoding", "deflate");
		dataOut = new DeflaterOutputStream(response.getOutputStream());
	    } else {
		dataOut = response.getOutputStream();
	    }
	    
	    server.getTool().writeBinaryData(data, 
					     clientRequest.getCE(), 
					     clientRequest.getPrivilege(),
					     dataOut);
	    if (debug()) debug("wrote binary data");
	    dataOut.flush();
	    if (debug()) debug("flushed stream");
	    if (useCompressed) {
		((DeflaterOutputStream)dataOut).finish();
		if (debug()) debug("finished off compression");
	    }
	} catch (IOException ioe) {}
    }
	
}

