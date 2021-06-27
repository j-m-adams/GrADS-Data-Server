/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;

import org.iges.util.FileResolver;

import org.iges.anagram.*;

/** This module handles invoking GrADS as an external process.  */
public class GradsTaskModule 
    extends AbstractModule {

    public GradsTaskModule(GradsTool tool) {
	this.tool = tool;
    }

    protected GradsTool tool;

    public String getModuleID() {
	return "invoker";
    }
    
    public void configure(Setting setting) 
	throws ConfigException {

	// script_dir setting not currently in user documentation
	String scriptDirName = setting.getAttribute("script_dir", "scripts");
	scriptDir = FileResolver.resolve(server.getHome(), scriptDirName);
	if (verbose()) verbose("script directory is " + 
			       scriptDir.getAbsolutePath());
	if (!scriptDir.exists()) {
	    throw new ConfigException(this, "script directory " + 
				      scriptDir.getAbsolutePath() + 
				      " not found");
	}

	timeLimit = setting.getNumAttribute("time_limit", 300) * 1000;
	if (verbose()) verbose("default time limit set to " + 
			       (timeLimit / 1000) + " sec");

	findGrads(setting);

	verifyGrads();
	
	useNice = true;

    }

    /** Attempts to locate a GrADS executable using settings given.
     * Order of search is: 
     * 1) file specified in grads_bin setting, if it exists; 
     * 2) full distribution given by grads_dir setting,
     * if both GrADS binaries (grads and gradsdap) are present;
     */
    protected void findGrads(Setting setting)
	throws ConfigException {
	
	completeGrads = true;
	String gradsBinaryString = setting.getAttribute("grads_bin");
	String gradsDirString = setting.getAttribute("grads_dir", "grads");
	File gradsDir = FileResolver.resolve(server.getHome(), gradsDirString);

	if (debug()) debug("grads_bin: " + gradsBinaryString +
			   " grads_dir: " + gradsDirString);


	if (!gradsBinaryString.equals("")) {

	    // single binary

	    if (debug()) debug("using a single binary");
	    completeGrads = false;
	    gradsBinary = FileResolver.resolve(server.getHome(), 
					       gradsBinaryString);

	    if (!gradsBinary.exists() || gradsBinary.isDirectory()) {
		error("specified GrADS executable " + 
		      gradsBinary.getAbsolutePath() + 
		      "does not exist, or is a directory");
		gradsBinary = null;
	    }

	} else {

	    // full distro

	    if (debug()) debug("trying for a full grads distribution");
	    if (gradsDir.exists()) {
		gradsBinaries = new File[2];
		for (int i = 0; i < gradsBinaries.length; i++) {
		    File binary = new File(gradsDir, binaryNames[i]);
		    if (binary.exists()) {
			if (debug()) debug("found binary " + binary);
			gradsBinaries[i] = binary;
			gradsBinary = binary;
		    } else {
			completeGrads = false;
			info("missing binary " + 
			      binary.getAbsolutePath() + 
			      " from distribution");
		    }

		}
	    } else {
		error("specified GrADS directory " + 
		      gradsDir.getAbsolutePath() + 
		      " does not exist");
		completeGrads = false;
	    }

	}
	
	if (!completeGrads) {
	    if (gradsBinary == null) {
		throw new ConfigException(this, "couldn't locate any GrADS " + 
					  "executables",
					  setting);
	    } 
	    info("using GrADS executable " + gradsBinary.getAbsolutePath());

	} else {
	    info("using full GrADS distribution at " + 
		 gradsDir.getAbsolutePath());

	}

    }

    /** Checks that the GrADS executable specified is, in fact, a
     * GrADS executable by running a little do-nothing GrADS script. 
     */
    protected void verifyGrads() 
	throws ConfigException {

	verbose("checking GrADS executables");
	if (completeGrads) {
	    for (int i = 0; i < gradsBinaries.length; i++) {
		checkOutput(i, gradsBinaries[i]);
	    }
	} else {
	    checkOutput(GradsDataInfo.CLASSIC, gradsBinary);
	}
    }

    /** Does the actual work for verifyGrads() */
    protected void checkOutput(int gradsBinaryType, File binary)
	throws ConfigException {

	// try to run GrADS

	Task task;
	try {
	    task = task(gradsBinaryType, "test", new String[0]);
	    task.run();
	} catch (AnagramException ae) {
	    debug("task error: " + ae.getMessage());
	    throw new ConfigException(this, 
				      "file " + binary.getAbsolutePath() +
				      " does not appear to be a GrADS executable");
	}

	// parse output from "q ctlinfo" which is only in 1.8sl10 and later
	// and "q ens_name" which is only in version 2.0.a2 or later

	String output = task.getOutput();
	BufferedReader in = new BufferedReader(new StringReader(output));
	try {
	    String testCtl = in.readLine();
	    String testEns = in.readLine();
	    if (testCtl == null || 
		testEns == null) {
		throw new ConfigException(this, 
					  "file " + binary.getAbsolutePath() +
					  " does not appear to be a GrADS " +
					  "executable");
	    }
	    if (testCtl.indexOf("Invalid") >= 0 || 
		testEns.indexOf("Invalid") >= 0) {
		throw new ConfigException(this, 
					  "file " + binary.getAbsolutePath() +
					  " appears to be an out-of-date " +
					  "GrADS version; " + 
					  "must be 2.0.a2 or newer");
	    }		
	} catch (IOException ioe) {}

	verbose("verified GrADS executable " + binary.getAbsolutePath());
    }
	
    /** Creates a Task object that will run a GrADS script. The actual
     *  command will be of the form: <code> [nice] <i>grads_binary</i> -bpcx
     *  "<i>script_dir</i>/<i>task_name</i>.gs <i>args</i>" </code>
     * @param gradsBinaryType Which of the two GrADS binaries to run
     * (grads or gradsdap)
     * @param taskName The name of the GrADS script to run. The actual
     * filename of the script will be generated by prepending the
     * script_dir setting, and appending ".gs.
     * @param args Arguments to the script
     */
    public Task task(int gradsBinaryType, String taskName, String[] args)
	throws ModuleException {

	File scriptFile = new File(scriptDir, taskName + ".gs");
	if (!scriptFile.exists()) {
	    throw new ModuleException(this, "missing script for " + taskName);
	}

	// the script name and all the script arguments have to go 
	// into a single system argument, in order for GrADS to parse it all
	// as a single command
	StringBuffer argBuffer = 
	    new StringBuffer(scriptFile.getAbsolutePath());
	for (int i = 0; i < args.length; i++) {
	    argBuffer.append(" ");
	    argBuffer.append(args[i]);
	}

	int offset = (useNice) ? 1 : 0;
	String[] cmd = new String[offset + 3];
	if (useNice) {
	    cmd[0] = "nice";
	}

	if (!completeGrads) {
	    cmd[offset] = gradsBinary.getAbsolutePath();
	} else {
	    cmd[offset] = gradsBinaries[gradsBinaryType].getAbsolutePath();
	}	    

	cmd[offset + 1] = "-bpcx";
	cmd[offset + 2] = argBuffer.toString();
	Task task =  new Task(cmd, null, null, timeLimit);

	if (debug()) log.debug(this, "command line for '" + taskName + 
			       "' task is:\n" + task.getCmd());
	
	return task;

    }

    /** @return Maximum time a GrADS process is allowed to run */
    public long getTimeLimit() {
	return timeLimit;
    }

    protected final static String[] binaryNames = { "grads", "gradsdap" };

    
    protected boolean completeGrads;

    protected boolean useNice;

    protected File scriptDir;

    protected long timeLimit;

    protected File gradsBinary;

    protected File[] gradsBinaries;
}
