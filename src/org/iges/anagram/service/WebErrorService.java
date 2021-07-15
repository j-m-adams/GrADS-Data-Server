/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.service;

import java.io.*;
import java.util.*;

import javax.servlet.http.*;

import org.iges.anagram.Setting;
import org.iges.anagram.ModuleException;
import org.iges.anagram.ClientRequest;

/** Sends an error message as an HTML page */
public class WebErrorService
    extends ErrorService {

    public String getServiceName() {
	return "error-html";
    }

    public void configure(Setting setting) {
    }

    protected void sendErrorMsg(ClientRequest request, 
				String msg) {
	if (request.getHttpResponse().isCommitted()) {
	    try {
		PrintStream out = new PrintStream
		    (request.getHttpResponse().getOutputStream());
		sendErrorText(out, request, msg);
		out.close();
	    } catch (IOException ioe) {}
	} else {
	    request.getHttpResponse().setHeader("Cache-Control", "no-cache");
	    String baseURL = getBaseURL(request);
	    PrintStream out = startHTML(request);
	    if (out == null) {
		return;
	    }
	    printHeader(out, "error", "error", null, baseURL);
	    sendErrorHTML(out, request.getFullURL(), msg);
	    printFooter(out, null, 0, baseURL);
	    out.close();
	}	    
	
	
    }

    protected void sendUnexpectedErrorMsg(ClientRequest request, 
					  String debugInfo) {
	if (request.getHttpResponse().isCommitted()) {
	    try {
		PrintStream out = new PrintStream
		    (request.getHttpResponse().getOutputStream());
		sendErrorText(out, request, debugInfo);
		out.close();
	    } catch (IOException ioe) {}
	    
	} else {
	    request.getHttpResponse().setHeader("Cache-Control", "no-cache");
	    String baseURL = getBaseURL(request);
	    PrintStream out = startHTML(request);
	    if (out == null) {
		return;
	    }
	    printHeader(out, "error", "error", null, 
			getBaseURL(request));
	    sendUnexpectedHTML(out, 
			       request, 
			       debugInfo);
	    printFooter(out, null, 0, baseURL);
	    out.close();
	}	    
    }

    protected void sendErrorHTML(PrintStream out,
				 String url,
				 String msg) {
	    out.print("The server could not fulfill this request ");
	    out.print("because of the following error:<p>\n");
	    out.print("<b>");
	    out.print(msg);
	    out.print("</b><p>\n");
	    out.print("Check the syntax of your request, ");
	    out.print("or click <a href=\".help\">here</a> for help ");
	    out.print("using the server.\n");
    }

    protected void sendUnexpectedHTML(PrintStream out, 
				      ClientRequest request,
				      String debugInfo) {
	out.print("Oops! The server encountered an unexpected error.<p>");
	out.print("Please report this error to <a href=\"");
	out.print(server.getImplHomePage());
	out.print("\">");
	out.print(server.getImplHomePage());
	out.print("</a>");
	out.print(", and include the following debug information:<p>\n");
	out.print("<code>");
	out.print(new Date(System.currentTimeMillis()));
	out.print(": ");
	out.print(request);
	out.print("\n");
	out.print(debugInfo);
	out.print("</code>");
    }

    protected void sendErrorText(PrintStream out, 
				 ClientRequest request,
				 String debugInfo) {
	out.print("\n\n");
	out.print("Error during request processing.\n");
	out.print("Please report this error to\n\t");
	out.print(server.getImplHomePage());
	out.print("\nand include the following debug information:\n");
	out.print(new Date(System.currentTimeMillis()));
	out.print(": ");
	out.print(request);
	out.print("\n");
	out.print(debugInfo);
    }

}
