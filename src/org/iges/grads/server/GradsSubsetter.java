/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;

import dods.dap.Server.*;

import org.iges.anagram.*;

/** Generic interface for modules that handle subsetting of GrADS datasets. */
public abstract class GradsSubsetter 
    extends AbstractModule {

    public String getModuleID() {
	return "subset";
    }

    /** @param tool Used to access other GrADS-specific modules */
    public void setTool(GradsTool tool) {
	this.tool = tool;
    }

    public void configure(Setting setting) {
    }

    /** @param bufferSize Size of memory buffer to use when streaming
     * subsets from disk */
    public void setBufferSize(int bufferSize) {
	this.bufferSize = bufferSize;
    }

    /** Streams a subset to the output stream given, using the
     *  CEEvaluator given.
     * @param useASCII If true, print ASCII text; if false, send
     * DODS/3.2 binary stream.
     * @param subsetLimit maximum allowable size for the subset
     * @throws ModuleException if subsetLimit bytes have already been
     * written and there is still more data; or, if any errors occur
     * during the subset operation
    */
    public abstract void subset(DataHandle data, 
				CEEvaluator ce,
				long subsetLimit,
				boolean useASCII,
				OutputStream out) 
	throws ModuleException;

    protected int bufferSize;

    protected GradsTool tool;
    
}
