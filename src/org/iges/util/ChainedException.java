/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.util;

/** An exception class supporting chaining. 
 *  
 *  Taken from the article <a href=
 *  "http://developer.java.sun.com/developer/technicalArticles/Programming/exceptions2/"> Exceptional practices, Part 2</a> by Brian Goetz, JavaWorld Oct 2001
 */
public class ChainedException extends Exception {
    private Throwable cause = null;
    
    public ChainedException() {
	super();
    }
    
    public ChainedException(String message) {
	super(message);
    }
    
    public ChainedException(String message, Throwable cause) {
	super(message);
	this.cause = cause;
    }
    
    /** @return The exception that caused this exception, or null if 
     *          there is none.
     */
    public Throwable getCause() {
	return cause;
    }
    
    public String getMessage() {
	if (cause != null) {
	    return super.getMessage() + "; " + cause.getMessage();
	} else {
	    return super.getMessage();
	}
    }
    
    /** Prints the stack trace for the entire chain of exceptions */
    public void printStackTrace() {
	super.printStackTrace();
	if (cause != null) {
	    System.err.println("Caused by:");
	    cause.printStackTrace();
	}
    }

    /** Prints the stack trace for the entire chain of exceptions */
    public void printStackTrace(java.io.PrintStream ps) {
	super.printStackTrace(ps);
	if (cause != null) {
	    ps.println("Caused by:");
	    cause.printStackTrace(ps);
	}
    }

    /** Prints the stack trace for the entire chain of exceptions */
    public void printStackTrace(java.io.PrintWriter pw) {
	super.printStackTrace(pw);
	if (cause != null) {
	    pw.println("Caused by:");
	    cause.printStackTrace(pw);
	}
    }
}

