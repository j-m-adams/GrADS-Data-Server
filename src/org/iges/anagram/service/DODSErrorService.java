/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.service;

import java.io.*;

import dods.dap.DODSException;

import org.iges.anagram.Setting;
import org.iges.anagram.ClientRequest;

/** Sends an error message in the DODS protocol format */
public class DODSErrorService
    extends ErrorService {

    public String getServiceName() {
	return "error-dods";
    }

    public void configure(Setting setting) {
    }

    protected void sendErrorMsg(ClientRequest request, 
				String msg) {
	PrintWriter out = null;
	try {
	    out = 
		new PrintWriter
		    (new OutputStreamWriter
			(request.getHttpResponse().getOutputStream()));
	} catch (IOException ioe) {}

	setHeaders(request);

	sendDODSError(out, msg);
	
    }

    protected void sendUnexpectedErrorMsg(ClientRequest request, 
					  String debugInfo) {
	PrintWriter out = null;
	try {
	    out = 
		new PrintWriter
		    (new OutputStreamWriter
			(request.getHttpResponse().getOutputStream()));
	} catch (IOException ioe) {}

	setHeaders(request);

	StringWriter msgString = new StringWriter();
	PrintWriter msg = new PrintWriter(msgString);
	msg.print("Oops! The server encountered an unexpected error ");
	msg.print("while serving the this request.\n");
	msg.print("Please report this error at\n\t");
	msg.print(server.getImplHomePage());
	msg.print("\nand include the following debug information:\n");
	msg.print(debugInfo);

	sendDODSError(out, msgString.toString());
	
    }

    protected void setHeaders(ClientRequest request) {
	request.getHttpResponse().setContentType("text/plain");
	request.getHttpResponse().setHeader("XDODS-Server", "dods/3.2");
	request.getHttpResponse().setHeader("XDAP", "3.2");
	request.getHttpResponse().setHeader("Cache-Control", "no-cache");
	request.getHttpResponse().setHeader("Content-Description", 
					    "dods_error");
    }	

    protected void sendDODSError(PrintWriter out, String msg) {

	DODSException de = new DODSException(msg);
	de.print(out);
	out.close();
    }

}
