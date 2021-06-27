/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;

import org.iges.anagram.*;

/** Updates a data handle to be in synch with the actual data
 *  source. This provides a quick way for the Catalog to check for
 *  changes to the back-end data store. 
 *
 */
public class GradsUpdateModule
    extends AbstractModule {

    public String getModuleID() {
	return "updater";
    }

    public GradsUpdateModule(GradsTool tool) {
	this.tool = tool;
    }

    protected GradsTool tool;

    public void configure(Setting setting)
	throws ConfigException {
					      
    }

    /** Checks if the DataHandle provided is out of date and updates
     * it if necessary.  A DataHandle is considered out of date iff the
     * descriptor file, source file, or supplemental DAS file have
     * been modified or deleted since the DataHandle was created.
     *  @return null if the Datahandle has not changed, or else, the
     *  new, updated DataHandle object
     *  @throws ModuleException if the data can no longer be accessed.
     */
    public boolean doUpdate(DataHandle data) 
	throws ModuleException {

	GradsDataInfo info = (GradsDataInfo)data.getToolInfo();
	
	File descriptorFile = info.getDescriptorFile();
	File sourceFile = info.getSourceFile();
	File userDAS = info.getUserDAS();
	long createTime = data.getCreateTime();

	boolean modified = false;

	if (!data.isAvailable()) {
	    modified = true;	
	}

	if (sourceFile != null) {
	    if (!sourceFile.exists()) {
		throw new ModuleException(this, 
					  "source file moved or deleted");
	    }
	    if (fileModified(sourceFile, createTime)) {
		if (debug()) debug("source file has changed");
		modified = true;	
	    }
	} 

	if (fileModified(descriptorFile, createTime)) {
	    if (debug()) debug("descriptor file has changed");
	    modified = true;	
	}
	    
	if (userDAS != null && fileModified(userDAS, createTime)) {
	    if (debug()) debug("DAS file has changed");
	    modified = true;	
	}

	if (modified) {
	    reload(data);
	}
	
	return modified;
	
    }
        
    protected boolean fileModified(File file, long createTime) {
	return !file.exists() || file.lastModified() > createTime;
    }

    /** Brings internal structures in a DataHandle up-to-date with
     *  respect to the data files. There is redundancy here with
     *  respect to the code in GradsImportModule, which indicates that
     *  the design could definitely be improved.  
     */
    protected void reload(DataHandle data) 
	throws ModuleException {

	if (debug()) debug(data.getCompleteName() + 
			   " is out of date; regenerating");
	if (debug()) debug("data lock: " + data.getSynch());

	// prevent any access during the update
	data.getSynch().lockExclusive();

	GradsDataInfo oldInfo = (GradsDataInfo)data.getToolInfo();
	File descriptorFile = null;
// 	if (oldInfo.getGradsBinaryType() != GradsDataInfo.CLASSIC) {
	if (oldInfo.getFormat().equals("ctl")) { 
	    descriptorFile = oldInfo.getDescriptorFile();
	} else {
	    if (debug()) debug("regenerating descriptor");
	    descriptorFile = 
		tool.importer.makeDescriptorFile(data.getCompleteName(),
						 oldInfo.getGradsArgument(),
						 oldInfo.getGradsBinaryType());
	}

	if (debug()) debug("regenerating tool info");
	try {
	    GradsDataInfo newInfo = 
		new GradsDataInfo(data.getCompleteName(),
				  oldInfo.getGradsBinaryType(),
				  oldInfo.getGradsArgument(),
				  descriptorFile,
				  oldInfo.getSourceFile(),
				  oldInfo.getUserDAS(),
				  oldInfo.getDocURL(),
				  null,
				  oldInfo.isDirectSubset(),
				  oldInfo.getMetadataFilters(),
				  oldInfo.getMetadata(),
				  oldInfo.hasLevels(),
				  oldInfo.hasEnsemble(),
				  oldInfo.getFormat());
	    data.setDescription(newInfo.getTitle());
	    data.setToolInfo(newInfo);

	    data.setAvailable(true);

	} catch (AnagramException ae) {
	    data.setAvailable(false);
	    throw new ModuleException(this, ae.getMessage());
	} finally {
	    data.getSynch().release();
	}
    }



}
