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

/** Provides an HTML page with links to information about the
 *  server's contents and how to use it.
 */
public class HelpService
    extends Service {

    public String getServiceName() {
	return "help";
    }

    public void configure(Setting setting) {
	cachedResponse = null;
    }

    public void handle(ClientRequest clientRequest)
	throws ModuleException {
	
	PrintStream page = startHTML(clientRequest);
	if (page == null) {
	    return;
	}
	
	String baseURL = getBaseURL(clientRequest);
	printHeader(page, "help", "help", null, baseURL);
	
	// After cache is initialized
	if (cachedResponse == null) {
	    cachedResponse = buildResponse(baseURL);
	}
	
	page.print(cachedResponse); 
	
	printFooter(page, null, 0, baseURL);
	
	page.flush();
	page.close();
	    
    }
    
    private String cachedResponse = null;

    private String buildResponse(String baseURL) {
	StringBuffer buffer = new StringBuffer();
	
	buffer.append("This server provides online access to, and analysis\n");
	buffer.append("of, scientific data, using the OPeNDAP protocol.\n");
	buffer.append("<p>For more information about: </p>\n");
	buffer.append("<ul>\n");
	buffer.append("  <li>this site's data holdings, ");
	buffer.append("and other site-specific information - see\n");
	buffer.append("    <a href=\"");
	buffer.append(server.getSiteHomePage(baseURL));
	buffer.append("\">");
	buffer.append("this site's home page</a>\n");
	buffer.append(" .&nbsp;<br>\n");
	buffer.append("    <br>\n");
	buffer.append("  </li>\n");
	buffer.append("  <li>The ");
	buffer.append(server.getImplName());
	buffer.append(", and features specific to this server, ");
	buffer.append("such as remote analysis - see the\n");
	buffer.append("    <a href=\"");
	buffer.append(server.getImplHomePage());
	buffer.append("\">");
	buffer.append(server.getImplName());
	buffer.append(" home page</a>\n");
	buffer.append(" .<br>\n");
	buffer.append("    <br>\n");
	buffer.append("  </li>\n");
	buffer.append("  <li>the OPeNDAP protocol, how to access data on OPeNDAP servers, and how to obtain\n");
	buffer.append("OPeNDAP-enabled client software - see the&nbsp; <a href=\"http://www.opendap.org\">\n");
	buffer.append("OPeNDAP home page</a>\n");
	buffer.append(".\n");
	buffer.append("  </li>\n");
	buffer.append("</ul>\n");

	return buffer.toString();
    }

}
