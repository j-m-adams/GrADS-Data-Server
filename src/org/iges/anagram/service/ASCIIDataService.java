/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.service;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;

import dods.dap.*;
import dods.dap.Server.*;

import org.iges.anagram.*;


/** Provides data subsets in ASCII comma-delimited format.
 * 
 * This service is part of the standard DODS services, 
 * however the format of its output is not specified.
 * For compatibility reasons, it is suggested that implementations of this
 * service match the output of the DODS netCDF server.
 *
 * Last modified: $Date: 2008/07/22 17:22:27 $ 
 * Revision for this file: $Revision: 1.5 $
 * Release name: $Name: v2_0 $
 * Original for this file: $Source: /homes/cvsroot/gds/gds/src/org/iges/anagram/service/ASCIIDataService.java,v $
 */
public class ASCIIDataService 
    extends Service {

    public String getServiceName() {
	return "ascii";
    }

    public void configure(Setting setting) {
    }

    /** Handles a request from the main dispatching servlet */
    public void handle(ClientRequest clientRequest)
	throws ModuleException {

	HttpServletRequest request = clientRequest.getHttpRequest();
	HttpServletResponse response = clientRequest.getHttpResponse();

	if (clientRequest.getCE() == null) {
	    throw new ModuleException
		(this, "subset requests must include a constraint expression");
	}

	try {
	    DataHandle data = getDataFromPath(clientRequest);

	    response.setContentType("text/plain");
	    response.setDateHeader("Last-Modified", data.getCreateTime());

	    server.getTool().writeASCIIData(data, 
					    clientRequest.getCE(), 
					    clientRequest.getPrivilege(),
					    response.getOutputStream());
	    
	} catch (IOException ioe){
	    // Ignore if user disconnects
	}

    }

}
