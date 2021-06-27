/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.service;

import java.io.*;

import org.iges.anagram.ClientRequest;
import org.iges.anagram.AbstractModule;
import org.iges.anagram.ModuleException;

/** A special type of service called when normal request processing
 *  produces an error. 
 */
public abstract class ErrorService
    extends Service {

    /** Not used by an error service */
    public void handle(ClientRequest request) {
	throw new RuntimeException("not implemented");
    }

    /** Handles "known" errors, i.e. those which are intentionally
     *  generated by other Anagram modules.
     *  Calls the subclass's implementation of sendErrorMsg() 
     *  to send an error message to the client, and logs the error */
    public void handle(ClientRequest request, ModuleException me) {
	String msg = me.getMessage();
	log.error(me.getModule(), request + " " + msg);
	if (me.getClientMessage() != null) {
	    msg = me.getClientMessage();
	    //	    log.error(this, "response to client: " + msg);
	}
	sendErrorMsg(request, msg);
    }

    /** Handles "unexpected" errors, i.e. those which result from 
     *  runtime problems and coding errors.
     *  Calls the subclass's implementation of sendUnexpectedErrorMsg() 
     *  to send an error message to the client, and logs the error */
    public void handleUnexpected(ClientRequest request, Throwable t) {
	if (t instanceof OutOfMemoryError) {
	    log.error(this, request + " ran out of available memory.\n" +
		      "your server is not configured properly to handle " + 
		      " peak loads. please see documentation.\n");
	    sendErrorMsg(request, "server is low on resources. " +
			 "please try again later.\n");
	} else {
	    StringWriter debugInfo = new StringWriter();
	    PrintWriter p = new PrintWriter(debugInfo);
	    t.printStackTrace(p);

	    log.error(this, request + 
		      "oops, exception " + t.getClass() + 
		      " was not caught.\n" +
		      "please report this as a bug, along with " +
		      "the following debug info:\n" +
		      debugInfo.toString() );

	    sendUnexpectedErrorMsg(request, debugInfo.toString());
	}
    }

    /** Sends the message provided in a format defined by the 
     *  ErrorService implementation. Called by <code>handle()</code>. 
     */
    protected abstract void sendErrorMsg(ClientRequest request, 
					 String msg);

    /** Sends the "unexpected error" message provided in a format 
     *  defined by the ErrorService implementation. 
     *  Called by <code>handle()</code>. 
     */
    protected abstract void sendUnexpectedErrorMsg(ClientRequest request, 
						   String debugInfo);
    
}