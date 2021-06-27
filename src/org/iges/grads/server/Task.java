/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;

import org.iges.anagram.AnagramException;

/** A wrapper for invoking an external process. */
public class Task {

    /** Sets up an external process. */
    public Task(String[] cmd,
		String [] env,
		File workDir,
		long timeLimit) {
	this.cmd = cmd;
	this.env = env;
	this.workDir = workDir;
	this.timeLimit = timeLimit;
	this.cmdString = buildCmdString(cmd);
	this.output = new StringBuffer();
    }

    /** Executes the external process, returning when it is finished
     *  or when it exceeds the time limit specified in the constructor.
     * @throws AnagramException If the process fails, or if the 
     * output parser finds an error message in the output.
     */
    public void run() 
	throws AnagramException {

	try {
	    long startTime = System.currentTimeMillis();
	    Process process = Runtime.getRuntime().exec(cmd, env, workDir);
	    if (process == null) {
		throw new AnagramException ("creation of child process " +
					    "failed for unknown reasons\n" +
					    "command: " + cmdString);
	    }

	    finish(process, startTime);

	} catch (IOException ioe) {
	    throw new AnagramException ("creation of child process failed\n" + 
					"command: " + cmdString, ioe);
	}
    }

    /** Executes the external process, returning when it is finished
     *  or when it exceeds the time limit specified.
     * @param timeLimit Overrides the time limit specified in the constructor.
     * @throws AnagramException If the process fails, or if there is an error 
     * message (a line beginning with "error: ") in the output.
     */
    public void run(long timeLimit) 
	throws AnagramException {

	this.timeLimit = timeLimit;
	run();
    }


    /** Returns a printable string version of the external command.
     */
    public String getCmd() {
	return cmdString;
    }

    /** Returns a string version of the command's console output */
    public String getOutput() {
	return output.toString();
    }


    /** Turns an array of Strings into a single space-separated String */
    protected String buildCmdString(String[] cmd) {
	StringBuffer buffer = new StringBuffer();
	for (int i = 0; i < cmd.length; i++) {
	    if (i > 0) {
		    buffer.append(" ");
	    }
	    buffer.append(cmd[i]);
	}
	return buffer.toString();
    }

    /** Waits for a running process to complete (or time out), and
     *  then performs cleanup and error-handling tasks.
     */
    protected void finish(Process process, long startTime) 
	throws AnagramException {
	
	BufferedReader stream 
	    = new BufferedReader
		(new InputStreamReader
		    (process.getInputStream()));
	
	char[] buffer = new char[1024];
	
	// wait in 10ms increments for the script to complete
	while (true) {
	    try {
		// check if process finished
		process.exitValue(); 

		// if we're still here it did
		break;

	    } catch (IllegalThreadStateException itse) {
		// not finished yet
		try {
		    Thread.currentThread().sleep(10);
		} catch (InterruptedException ie) {}
		try {
		    // check for new output
		    if (stream.ready()) {
			int charsRead = stream.read(buffer);
			output.append(buffer, 0, charsRead);
		    }
		} catch (IOException ioe) {}
		
		// check for timeout
		long endTime = System.currentTimeMillis();
		if (endTime - startTime > timeLimit) {
		    try {
			stream.close();
		    } catch (IOException ioe) {}
		    process.destroy();
		    throw new AnagramException
			("process exceeded time limit of " +
			 (timeLimit / 1000)
			 + " sec");
		}
	    }

	}

	// read remaining output from completed process
	try {
	    while (stream.ready()) {
		int charsRead = stream.read(buffer);
		output.append(buffer, 0, charsRead);
	    }
	} catch (IOException ioe) {}

	checkErrors();
	
	try {
	    stream.close();	
	} catch (IOException ioe) {}
	
    }

    /** Parses output looking for error messages, defined as lines
     *  beginning with the string contained in ERROR_INDICATOR
     *  (currently "error: "). The remainder of the line is treated
     * as an error message to pass to the user.
     *
     * @throws AnagramException if a line containing an error message
     * is found.
     */
    protected void checkErrors() 
	throws AnagramException {

	BufferedReader in = 
	    new BufferedReader
		(new StringReader
		    (output.toString()));

	String line;
	try {
	    while ((line = in.readLine()) != null) {
		if (line.startsWith(ERROR_INDICATOR)) {
		    String msg = line.substring(ERROR_INDICATOR.length());
		    throw new AnagramException(msg);
		}
	    }
	} catch (IOException ioe) {}
    }

    /** String that subprocesses can print to indicate to the server
     * that an error has occurred in their processing. */
    protected static String ERROR_INDICATOR = "error: ";

    protected StringBuffer output;
    protected String cmdString;
    protected String[] cmd;
    protected String[] env;
    protected File workDir;
    protected long timeLimit;
}
