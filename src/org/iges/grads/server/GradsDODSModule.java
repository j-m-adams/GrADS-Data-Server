/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;
import java.util.*;

import org.iges.util.Spooler;

import dods.dap.*;
import dods.dap.Server.*;

import org.iges.anagram.*;
import org.iges.grads.server.dap.*;

/** Provides DODS metadata and subsets for GrADS data objects.
 *  This is the main back end for the GradsTool class. 
 *  Uses cache files for DDS, DAS, and INFO data.
 */
public class GradsDODSModule 
    extends AbstractModule {

    public String getModuleID() {
	return "dods";
    }

    public GradsDODSModule(GradsTool tool) {
	this.tool = tool;
    }

    protected GradsTool tool;

    public void configure(Setting setting) {
	this.defaultSubsetSize = 
	    setting.getNumAttribute("subset_size", 0);

	int bufferSize = (int)setting.getNumAttribute("buffer_size", 16384);

	store = server.getStore();
	baseTypeFactory = new GradsServerFactory();
	gridSubsetter = new GradsGridSubsetter();
	gridSubsetter.init(server, this);
	gridSubsetter.setTool(tool);
	gridSubsetter.setBufferSize(bufferSize);

	stnSubsetter = new GradsStnSubsetter();
	stnSubsetter.init(server, this);
	stnSubsetter.setTool(tool);
	stnSubsetter.setBufferSize(bufferSize);

	//	buildFunctionMap();
	buildClauseFactory();
    }

    /** Creates a constrained DDS object for the given dataset (which
     *  is the Java-DODS encapsulation of a subset request).
     * @param ce The constraint to apply to the DDS
     * @see loadDDS()
     */
    public ServerDDS getDDS(DataHandle data, 
			    String ce)
	throws ModuleException {

	InputStream ddsStream = loadDDS(data);

	if (debug()) log.debug(this, "creating dds object for " + data);
	ServerDDS dds = new ServerDDS(baseTypeFactory);
	try {
	    dds.parse(ddsStream);
        } catch (Exception e){
            throw new ModuleException(this, "dds load failed", e);
	} finally {
	    try {
		ddsStream.close();
	    } catch (IOException ioe) {}
	}	    

	if (ce == null) {
	    return dds;
	} 

	if (debug()) log.debug(this, "parsing constraint " + ce + 
			       " for " + data);
	try {
	    CEEvaluator evaluator = 
		new CEEvaluator(dds, clauseFactory);
	    evaluator.parseConstraint(ce);
	} catch (Exception e) {
            throw new ModuleException(this, "constraint parsing failed", e);
	} 
	if (debug()) log.debug(this, "dds created successfully for " + data);
	    
	return dds;
    }

    /** Creates a DAS object for the given dataset (which
     *  is the Java-DODS encapsulation of a subset request).
     * @param ce The constraint to apply to the DDS
     * @see loadDDS()
     */
    public DAS getDAS(DataHandle data)
	throws ModuleException {

	InputStream dasStream = loadDAS(data);

	if (debug()) log.debug(this, "creating das object for " + data);

	DAS das = new DAS();
	try {
	    das.parse(dasStream);
        } catch (Exception e){
            throw new ModuleException(this, "das load failed", e);
	} finally {
	    try {
		dasStream.close();
	    } catch (IOException ioe) {}
	}

	return das;
    }

    /** Writes a DDS object directly to an output stream. If there is 
     *  no constraint to apply, the DDS can simply be streamed directly
     *  from disk, avoiding the parsing overhead of getDDS().
     */
    public void writeDDS(DataHandle data, 
			 String ce, 
			 OutputStream out)
	throws ModuleException {

	if (ce == null) {
	    InputStream ddsStream = loadDDS(data);
	    if (debug()) log.debug(this, "writing dds for " + data + 
				   " to stream");
	    try {
		Spooler.spool(ddsStream, out);
	    } catch (IOException ioe){
		throw new ModuleException(this, "io error on dds write", ioe);
	    } finally {
		try {
		    ddsStream.close();
		} catch (IOException ioe) {}
	    }
	} else {
	    ServerDDS dds = getDDS(data, ce);
	    if (debug()) log.debug(this, "writing constrained dds for " + 
				   data + " to stream");
	    dds.printConstrained(out);
	} 
    }

    /** Writes a DAS object directly to an output stream. The DAS
     *  is cached and streamed directly from disk, avoiding the parsing
     *  overhead of getDAS().
     */
    public void writeDAS(DataHandle data, 
			 OutputStream out)
	throws ModuleException {

	InputStream dasStream = loadDAS(data);
	if (debug()) log.debug(this, "writing das for " + data + " to stream");
	try {
	    Spooler.spool(dasStream, out);
	} catch (IOException ioe){
	    throw new ModuleException(this, "io error on das write", ioe);
	} finally {
	    try {
		dasStream.close();
	    } catch (IOException ioe) {}
	}
    }

    /** Writes an info page directly to an output stream. The info page
     *  is cached and streamed directly from disk.
     */
    public void writeWebInfo(DataHandle data, 
			     OutputStream out) 
	throws ModuleException {

	GradsDataInfo gradsInfo = (GradsDataInfo)data.getToolInfo();

	File infoFile =  store.get(this, gradsInfo.getDODSName() + ".info", 
				   data.getCreateTime());
	synchronized(data) {
	    if (!infoFile.exists()) {
		extract(data);
	    }
	}

	if (debug()) log.debug(this, "loading web info for " + data);
	
	try {
	    InputStream infoStream = new BufferedInputStream
		(new FileInputStream
		    (infoFile));
	    Spooler.spool(infoStream, out);
	    infoStream.close();
	} catch (FileNotFoundException fnfe) {
	    throw new ModuleException(this, infoFile.getAbsolutePath() + 
				      " not found");
	} catch (IOException ioe){
	    throw new ModuleException(this, "io error on das write", ioe);
	} 


    }

    /** Writes an info page directly to an output stream. The info page
     *  is cached and streamed directly from disk.
     */
    public void writeTHREDDSTag(DataHandle data, 
			     OutputStream out) 
	throws ModuleException {

	GradsDataInfo gradsInfo = (GradsDataInfo)data.getToolInfo();

	File infoFile =  store.get(this, gradsInfo.getDODSName() + ".thredds", 
				   data.getCreateTime());
	synchronized(data) {
	    if (!infoFile.exists()) {
		extract(data);
	    }
	}

	if (debug()) log.debug(this, "loading THREDDS tag for " + data);
	
	try {
	    InputStream infoStream = new BufferedInputStream
		(new FileInputStream
		    (infoFile));
	    Spooler.spool(infoStream, out);
	    infoStream.close();
	} catch (FileNotFoundException fnfe) {
	    throw new ModuleException(this, infoFile.getAbsolutePath() + 
				      " not found");
	} catch (IOException ioe){
	    throw new ModuleException(this, "io error on THREDDS write", ioe);
	} 
    }

    /** Writes a data subset to a stream in binary format. */
    public void writeBinaryData(DataHandle data, 
				String ce, 
				Privilege privilege,
				OutputStream out)
	throws ModuleException {

	// Evaluate constraint
	ServerDDS dds = getDDS(data, null);
	CEEvaluator evaluator = new CEEvaluator(dds, clauseFactory);
	try {
	    evaluator.parseConstraint(ce);
	} catch (Exception e) {
            throw new ModuleException(this, "constraint parsing failed", e);
	} 

	if (debug()) debug("evaluated constraint: " + ce);

	// Print DDS for subset
	PrintStream ddsOut = new PrintStream(out);
	dds.printConstrained(ddsOut);
	ddsOut.println("Data:");
	ddsOut.flush();

	if (debug()) log.debug(this, "streamed DDS to client");

	// Write data for subset
	if (debug()) log.debug(this, "writing binary subset for " + data + 
			       ", " + ce);
	GradsDataInfo gradsInfo = (GradsDataInfo)data.getToolInfo();
	if (gradsInfo.isDirectSubset() && gradsInfo.getCTL() == null) {
	    if (debug()) debug("missing CTL info for " + data);
	    extract(data);
	}

	long subsetSize = privilege.getNumAttribute("dods_subset_size", 
					       defaultSubsetSize);
	if (gradsInfo.getDataType() == GradsDataInfo.GRID) {
	    gridSubsetter.subset(data, evaluator, subsetSize, false, out);
	} else if (gradsInfo.getDataType() == GradsDataInfo.STN) {
	    stnSubsetter.subset(data, evaluator, subsetSize, false, out);
	} 	
    }

    /** Writes a data subset to a stream in ASCII format. */
    public void writeASCIIData(DataHandle data, 
			       String ce, 
			       Privilege privilege,
			       OutputStream out)
	throws ModuleException {

	
	// Evaluate constraint
	ServerDDS dds = getDDS(data, null);
	CEEvaluator evaluator = new CEEvaluator(dds, clauseFactory);
	try {
	    evaluator.parseConstraint(ce);
	} catch (Exception e) {
            throw new ModuleException(this, "constraint parsing failed", e);
	} 

	// Write data for subset
	if (debug()) log.debug(this, "writing ASCII subset for " + data + 
			       ", " + ce);

	GradsDataInfo gradsInfo = (GradsDataInfo)data.getToolInfo();

	long subsetSize = privilege.getNumAttribute("dods_subset_size", 
					       defaultSubsetSize);
	if (gradsInfo.getDataType() == GradsDataInfo.GRID) {
	    gridSubsetter.subset(data, evaluator, subsetSize, true, out);
	} else if (gradsInfo.getDataType() == GradsDataInfo.STN) {
	    stnSubsetter.subset(data, evaluator, subsetSize, true, out);
	} 	

    }


    protected InputStream loadDDS(DataHandle data)
	throws ModuleException {

	GradsDataInfo gradsInfo = (GradsDataInfo)data.getToolInfo();

	File ddsFile =  store.get(this, gradsInfo.getDODSName() + ".dds", 
				   data.getCreateTime());
	synchronized(data) {
	    if (!ddsFile.exists()) {
		extract(data);
	    }
	}

	if (debug()) log.debug(this, "loading dds for " + data);
	
	try {
	    return new BufferedInputStream
		(new FileInputStream
		    (ddsFile));
	} catch (Exception e) {
	    throw new ModuleException(this, ddsFile.getAbsolutePath() + 
				      " not found" + e);
	}
    }

    protected InputStream loadDAS(DataHandle data)
	throws ModuleException {

	GradsDataInfo gradsInfo = (GradsDataInfo)data.getToolInfo();

	File dasFile =  store.get(this, gradsInfo.getDODSName() + ".das", 
				   data.getCreateTime());
	synchronized(data) {
	    if (!dasFile.exists()) {
		extract(data);
	    }
	}

	if (debug()) log.debug(this, "loading das for " + data);

	try {
	    return new BufferedInputStream
		(new FileInputStream
		    (dasFile));
	} catch (Exception e) {
	    throw new ModuleException(this, dasFile.getAbsolutePath() + 
				      " not found" + e);
	} 
    }
    
    public void extract(DataHandle data) 
	throws ModuleException {

	GradsDataInfo gradsInfo = (GradsDataInfo)data.getToolInfo();

	GradsExtracter extracter;
	if (gradsInfo.getDataType() == GradsDataInfo.GRID) {
	    if (debug()) log.debug(this, "extracting dds/das info " + 
				   "for gridded data " + data);
	    extracter = new GradsGridExtracter(); 
								       
	} else if (gradsInfo.getDataType() == GradsDataInfo.STN) {
	    if (debug()) log.debug(this, "extracting dds/das info " + 
				   "for station data " + data);
	    extracter = new GradsStnExtracter();
	} else {
	    throw new ModuleException(this, data + " has unknown data type " + 
				       gradsInfo.getDataType());
	}

	String prefix = 
	    store.get(this, gradsInfo.getDODSName()).getAbsolutePath();
	try {
	    extracter.init(server, this);
	    extracter.parse(data, tool.getTask(), prefix);
	} catch (AnagramException ae) {
	    throw new ModuleException(this, "extraction failed", ae);
	}
			
    }

    protected void buildClauseFactory() {
	FunctionLibrary dummyFunctions =  new FunctionLibrary();
	dummyFunctions.add(new DummyFunction("stid"));
	dummyFunctions.add(new DummyFunction("bounds"));
	clauseFactory = new ClauseFactory(dummyFunctions);
    }

    protected GradsSubsetter gridSubsetter;
    protected GradsSubsetter stnSubsetter;

    protected long defaultSubsetSize;

    protected Store store;
    protected BaseTypeFactory baseTypeFactory;
    protected ClauseFactory clauseFactory;

}
