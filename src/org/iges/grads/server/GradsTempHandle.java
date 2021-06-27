/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;
import java.util.*;

import org.iges.anagram.*;

/** Implementation of the TempDataHandle interface for GrADS datasets. 
 * @see org.iges.anagram.TempDataHandle for full documentation of methods. 
 */
public class GradsTempHandle
    implements TempDataHandle {

    public GradsTempHandle(String name, String longName, 
			   GradsDataInfo info, File dataFile, 
			   Set dependencies) 
	throws AnagramException {

	createTime = System.currentTimeMillis();

	this.info = info;
	this.dataFile = dataFile;
	this.dataHandles = new DataHandle[] {
	    new DataHandle(name, info.getTitle(), info, createTime),
	    new DataHandle(longName, info.getTitle(), info, createTime),
	};
	this.dependencies = dependencies;
    }
    
    public GradsTempHandle(String name, GradsDataInfo info, File dataFile) 
	throws AnagramException {

	long createTime = System.currentTimeMillis();

	this.info = info;
	this.dataFile = dataFile;
	this.dataHandles = new DataHandle[] {
	    new DataHandle(name, info.getTitle(), info, createTime),
	};
	this.dependencies = dependencies;
    }
    public Set getDependencies() {
	return dependencies;
    }
    public DataHandle[] getDataHandles() {
	return dataHandles;
    }
    public long getStorageSize() {
	return info.getDescriptorFile().length() + dataFile.length();
    }

    public long getCreateTime() {
	return createTime;
    }

    public void deleteStorage() {
	
	for (int i = 0; i < dataHandles.length; i++) {
	    dataHandles[i].setAvailable(false);
	}
	info.getDescriptorFile().delete();
	dataFile.delete();
    }

    protected Set dependencies;
    protected GradsDataInfo info;
    protected DataHandle[] dataHandles;
    protected long createTime;
    protected File dataFile;
}
