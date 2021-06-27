/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.service;

import java.io.*;
import java.util.Date;
import javax.servlet.http.*;
import org.iges.anagram.*;

/** Provides the DODS Data Attribute Structure for a data object */
public class DASService 
    extends Service {

    public String getServiceName() {
	return "das";
    }

    public void configure(Setting setting) {
    }

    public void handle(ClientRequest clientRequest)
	throws ModuleException {

	HttpServletRequest request = clientRequest.getHttpRequest();
	HttpServletResponse response = clientRequest.getHttpResponse();
	
	DataHandle data = getDataFromPath(clientRequest);
	
	response.setContentType("text/plain");
	response.setHeader("XDODS-Server", "dods/3.2");
	response.setHeader("XDAP", "3.2");
	response.setHeader("Content-Description", "dods_das");
	response.setDateHeader("Last-Modified", data.getCreateTime());
	
	try {
	    server.getTool().writeDAS(data, 
				      response.getOutputStream());
	} catch (IOException ioe) {}
    }
	
}

