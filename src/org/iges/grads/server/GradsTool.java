/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;

import dods.dap.*;
import dods.dap.Server.*;

import org.iges.anagram.*;

/** GrADS-based implementation of the Anagram framework. This just
 *  ties together the various specialized modules in to a single
 *  interface.  For example, the actual work for routine data and
 *  metadata requests is done by the GradsDODSModule.
 * @see GradsUploadModule
 * @see GradsUpdateModule
 * @see GradsAnalysisModule
 * @see GradsImportModule
 * @see GradsDODSModule
 * @see GradsTaskModule
 */
public class GradsTool 
    extends Tool {
   
    public String getModuleID() {
	return "grads";
    }

    public void init(Server server, Module parent) {
	super.init(server, parent);

	if (verbose()) log.verbose(this, "creating importer module");
	importer = new GradsImportModule(this);
	importer.init(server, this);
	    
	if (verbose()) log.verbose(this, "creating invoker module");
	task = new GradsTaskModule(this);
	task.init(server, this);

	if (verbose()) log.verbose(this, "creating dods module");
	dods = new GradsDODSModule(this);
	dods.init(server, this);

	if (verbose()) log.verbose(this, "creating analyzer module");
	analyzer = new GradsAnalysisModule(this);
	analyzer.init(server, this);

	if (verbose()) log.verbose(this, "creating uploader module");
	uploader = new GradsUploadModule(this);
	uploader.init(server, this);

	if (verbose()) log.verbose(this, "creating updater module");
	updater = new GradsUpdateModule(this);
	updater.init(server, this);
    }	

    public void configure(Setting setting) 
	throws ConfigException {

	if (debug()) debug("setting is " + setting);

	configModule(importer, setting);
	configModule(task, setting);
	configModule(dods, setting);
	configModule(analyzer, setting);
	configModule(uploader, setting);
	configModule(updater, setting);
    }	

    public DataHandle[] doImport(Setting setting) {
	return importer.doImport(setting);
    }


    /** @param ae The analysis expression to be evaluated
     * @return A handle to the result(s) of the analysis
     * @throws ModuleException if the analysis fails for any reason
     */
    public TempDataHandle doAnalysis(String name,
					      String ae, 
					      Privilege privilege) 
	throws ModuleException {
	return analyzer.doAnalysis(name, ae, privilege);
    }

    /** @param input The stream of data to be stored
     * @return Handle(s) to the stored data
     * @throws ModuleException if the operation fails for any reason
     */
    public TempDataHandle doUpload(String name,
					    InputStream input,
					    long size,
					    Privilege privilege)
	throws ModuleException {
	return uploader.doUpload(name, input, size, privilege);
    }


    /** Creates an up-to-date version of the data handle provided.
     *  Returns null if the data handle has not changed.
     *  Throws an exception if the data can no longer be accessed.
     */
    public boolean doUpdate(DataHandle data) 
	throws ModuleException {
	return updater.doUpdate(data);
    }


    /** Provides an object representation of a Data Descriptor Structure. 
     * @param data The data to be accessed
     * @param ce A constraint to be applied to the DDS. Set to null to 
     *           retrieve an unconstrained DDS.
     * @return an object representing the DDS 
     * @throws ModuleException if the request fails for any reason
     */
    public ServerDDS getDDS(DataHandle data, String ce)
	throws ModuleException {
	return dods.getDDS(data, ce);
    }

    /** Provides an object representation of a Data Attribute Structure. 
     * @param data The data to be accessed
     * @return an object representing the DAS 
     * @throws ModuleException if the request fails for any reason
     */
    public DAS getDAS(DataHandle data)
	throws ModuleException {
	return dods.getDAS(data);
    }

    /** Writes a Data Descriptor Structure to a stream.
     *	@param data The data to be accessed
     *  @param ce A constraint to be applied to the DDS. Set to null to 
     *           retrieve an unconstrained DDS.
     *  @param out A stream to which to write the DDS 
     * @throws ModuleException if the request fails for any reason
     */
    public void writeDDS(DataHandle data, String ce, OutputStream out)
	throws ModuleException {
	dods.writeDDS(data, ce, out);
    }

    /** Writes a Data Attribute Structure to a stream.
     *	@param data The data to be accessed
     *  @param out A stream to which to write the DAS 
     * @throws ModuleException if the request fails for any reason
     */
    public void writeDAS(DataHandle data, OutputStream out)
	throws ModuleException {
	dods.writeDAS(data, out);
    }

    public void writeWebInfo(DataHandle data, OutputStream out)
	throws ModuleException {
	dods.writeWebInfo(data, out);
    }

    public void writeTHREDDSTag(DataHandle data, OutputStream out)
	throws ModuleException {
	dods.writeTHREDDSTag(data, out);
    }
    
    /** Writes a data subset to a stream in binary format.
     *	@param data The data to be accessed
     *  @param ce A constraint expression specifying the subset to be sent
     *  @param out A stream to which to write the subset
     * @throws ModuleException if the request fails for any reason
     */
    public void writeBinaryData(DataHandle data, 
				String ce, 
				Privilege privilege,
				OutputStream out)
	throws ModuleException {
	dods.writeBinaryData(data, ce, privilege, out);
    }

    /** Writes a data subset to a stream in ASCII format.
     *	@param data The data to be accessed
     *  @param ce A constraint expression specifying the subset to be sent
     *  @param out A stream to which to write the subset
     * @throws ModuleException if the request fails for any reason
     */
    public void writeASCIIData(DataHandle data, 
			       String ce, 
			       Privilege privilege,
			       OutputStream out)
	throws ModuleException {
	dods.writeASCIIData(data, ce, privilege, out);
    }

    /** Used internally. The task module is the module in charge of
     *  invoking the GrADS executable. 
     */
    public GradsTaskModule getTask() {
	return task;
    }


    protected GradsUploadModule uploader;
    protected GradsUpdateModule updater;
    protected GradsAnalysisModule analyzer;
    protected GradsImportModule importer;
    protected GradsDODSModule dods;
    protected GradsTaskModule task;

} 
