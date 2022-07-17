/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram;

import java.io.*;
import java.util.*;

import org.iges.util.*;

/** Manages the list of available data objects. */
public class Catalog
    extends AbstractModule {

    public Catalog() {
	try {
	    root = new DirHandle("/");
	    tempEntries = new DirHandle("/");
	} catch (AnagramException ae) {}
	synch = new ExclusiveLock();
    }
	

    public String getModuleID() {
	return "catalog";
    }

    public void configure(Setting setting)
	throws ConfigException {

	tempStorageLimit = 
	    setting.getNumAttribute("temp_storage_limit", 0);
	if (tempStorageLimit == 0) {
	    if (verbose()) log.verbose(this, 
				       "temp storage limit is 0 (no limit)");
	} else {
	    if (verbose()) log.verbose(this, "temp storage limit is " + 
				       tempStorageLimit + " MB");
	}

	tempEntryLimit = setting.getNumAttribute("temp_entry_limit", 0);
	if (tempEntryLimit == 0) {
	    if (verbose()) log.verbose(this, 
				       "temp entry limit is 0 (no limit)");
	} else {
	    if (verbose()) log.verbose(this, "temp entry limit is " + 
				       tempEntryLimit);
	}

	tempAgeLimit = setting.getNumAttribute("temp_age_limit", 0);
	if (tempAgeLimit == 0) {
	    if (verbose()) log.verbose(this, "temp age limit is 0 (no limit)");
	} else {
	    if (verbose()) log.verbose(this, "temp age limit is " + 
				       tempAgeLimit + " hours");
	}

	if (tempDeleteQueue == null) {
	    loadTempEntriesFromStore();
	    loadCatalogFromStore();
	}
	
	Setting dataTag = null;
	try {
	    dataTag = setting.getUniqueSubSetting("data");
	} catch (AnagramException ae) {
	    throw new ConfigException(this, ae.getMessage());
	}
	if (verbose()) log.verbose(this, "importing data");
	DataHandle[] handles = server.getTool().doImport(dataTag);
	List handleList = Arrays.asList(handles);
	if (verbose()) log.verbose(this, "clearing deleted entries");
	unloadEntries(handleList);
	if (verbose()) log.verbose(this, "adding new entries");
	loadEntries(handleList);
	
	saveCatalogToStore();
	saveTempEntriesToStore();
	
    }

    /** Adds a temporary dataset to the catalog. */
    public void addTemp(TempDataHandle tempData) {
	synch.lockExclusive();
	addTemp(tempData, true);
	synch.release();
    }

    protected void addTemp(TempDataHandle tempData, 
					boolean save) {

	tempDeleteQueue.addLast(tempData);

	DataHandle[] handles = tempData.getDataHandles();
	for (int i = 0; i < handles.length; i++) {
	    if (debug()) log.debug(this,  "adding temp data as " + 
				   handles[i].getCompleteName());
	    tempEntries.add(handles[i]);
	}

	tempStorage += tempData.getStorageSize();
	if (debug()) log.debug(this,  "temp queue size is " + 
			       tempDeleteQueue.size());

	if (debug()) log.debug(this, "temp entries: " + 
			       tempEntries.getEntries(true).keySet());
	    
	if (outdated(tempData)) {
	    removeTemp("clearing outdated cache entry", tempData);
	}

	
	checkLimits();

	if (save) {
	    saveTempEntriesToStore();
	}
    }

    protected boolean outdated(TempDataHandle tempData) {
	if (debug()) debug("out-of-date check for temp data");
	Iterator it = tempData.getDependencies().iterator();
	while (it.hasNext()) {
	    String name = (String)it.next();
	    Handle handle = get(name);
	    if (handle != null && 
		handle instanceof DataHandle &&
		((DataHandle)handle).getCreateTime() > 
		tempData.getCreateTime()) {
		return true;
	    }
	}
	return false;
    }

    protected void checkDependencies(DataHandle data) {
	if (debug()) debug("checking temp entries for dependencies on " +
			   data.getCompleteName());
	Iterator it = tempDeleteQueue.iterator();
	while (it.hasNext()) {
	    TempDataHandle tempHandle = (TempDataHandle)it.next();
	    if(tempHandle.getCreateTime() < data.getCreateTime() &&
	       tempHandle.getDependencies().contains(data.getCompleteName())) {
		it.remove();
		removeTemp("dependency " + data.getCompleteName() + 
			   " has changed", tempHandle);
	    }
	}
    }


    /** Retrieves the handle, if any, that maches the given pathname.
     *  If a handle is found, it will be non-exclusively locked for the
     *  calling thread. This lock <i>must</i> be released
     *  when the caller finishes using the handle or other requests
     *  may become deadlocked.
     */ 
    public Handle getLocked(String path) {
	synch.lock();
	Handle handle = get(path);
	if (handle != null) {
	    handle.getSynch().lock();
	}
	synch.release();
	return handle;
    }

    /** Returns true if the catalog contains a handle that matches the 
     *  given pathname.
     */
    public boolean contains(String path) {
	synch.lock();
	Handle handle = get(path);
	synch.release();
	if (handle != null) {
	    return true;
	} else {
	    return false;
	}
	    //	return handle != null;
    }

    protected Handle get(String path) {
	Handle handle = null;
	if (path.startsWith("/_")) {
	    if (debug()) debug("looking in temp entries for " + path);
	    handle = tempEntries.get(path);
	} else {
	    if (debug()) debug("looking in permanent catalog for " + path);
	    if (path.endsWith("/")) {
		path = path.substring(0, path.length() - 1);
	    }
	    if (path.equals("")) {
		handle = root;
	    } else {
		DirHandle dir = getDir(path, root);
		handle = dir.get(path);
		if (handle instanceof DataHandle) {
		    try {
			if (server.getTool().doUpdate((DataHandle)handle)) {
			    checkDependencies((DataHandle)handle);
			}
		    } catch (ModuleException me) {
			error("update of dataset  " + 
			      handle.getCompleteName() + " failed; " + 
			      me.getMessage());
		    }
		} 
	    }
	} 
	return handle;
    }

    /** Deletes all temporary entries in the catalog */
    public void clearTemp() {
	while (tempDeleteQueue.size() > 0) {
	    removeTemp("clearing cache", null);
	}
	synch.lock();
	saveTempEntriesToStore();
	synch.release();
    }

    /** Returns the parent of the handle given. The resulting handle is not
     *  locked, since directory handles are only modified during
     *  reconfiguration. 
     */ 
    public DirHandle getParent(Handle handle) {
	return getDir(handle.getCompleteName(), root);
    }

    /** Used by add, remove, get, and getParent to look up datasets */
    protected DirHandle getDir(String path, DirHandle dir) {
	//	if (debug()) debug("matched " + path + " to " + dir);
	if (path.endsWith("/")) {
	    path = path.substring(0, path.length() - 1);
	}
	String subPath = getChildPath(path, dir);

	if (subPath.equals("") || path.startsWith("_")) {
	    return dir;
	}

	Iterator it = dir.getEntries(false).values().iterator();
	while (it.hasNext()) {
	    Handle current = (Handle)it.next();
	    if (!(current instanceof DirHandle)) {
		continue;
	    }
	    DirHandle subdir = (DirHandle)current;
	    if (path.startsWith(subdir.getCompleteName() + "/")) {
		return getDir(path, subdir);
	    }
	}

	return dir;
	
    }

    /** Used by add, remove and getDir 
     *  Takes "basepath[/name1[/name2/../nameN]]"
     *  Returns name1 if followed by name2, or "" otherwise
     */
    protected String getChildPath(String completePath, DirHandle dir) {
	int subPathStart = dir.getCompleteName().length();
	int subPathEnd = completePath.indexOf('/', subPathStart + 1);
	if (subPathEnd < 0) { 
	    return "";
	} else {
	    return completePath.substring(subPathStart, subPathEnd);
	}
    }
	
    /** Adds a handle to the correct dir, creating it if necessary */
    protected void addHandle(DataHandle handle) {
	try {
	    if (debug()) debug("adding handle " + handle);
	    DirHandle parent = getDir(handle.getCompleteName(), root);
	    if (debug()) debug("handle matches dir " + parent);
	    String childPath = getChildPath(handle.getCompleteName(), parent);
	    if (debug()) debug("subdir is " + childPath);
  	    while (!childPath.equals("") && 
		   !handle.getCompleteName().startsWith("_")) {
		DirHandle newDir = 
		    new DirHandle(parent.getCompleteName() + childPath);
		synchronized(parent) {
		    parent.add(newDir);
		}
		parent = newDir;
		if (debug()) debug("created dir " + newDir);
		childPath = getChildPath(handle.getCompleteName(), parent);
		if (debug()) debug("subdir is " + childPath);
	    } 	
	    synchronized(parent) {
		if (debug()) debug("adding handle to " + parent);
		parent.add(handle);
	    }
	    checkDependencies(handle);
	} catch (AnagramException ae) {
	    error("failed adding " + handle + "; " + ae.getMessage());
	}
    }	

    /** Removes a handle, removing the parent dir if
     *  appropriate. */
    protected void removeHandle(Handle handle) {
	DirHandle parent = getDir(handle.getCompleteName(), root);
	synchronized(parent) {
	    parent.remove(handle.getCompleteName());
	}
	if (parent != root && parent.getEntries(false).size() == 0) {
	    if (debug()) debug("destroying dir " + parent);
	    removeHandle(parent);
	}
    }	    

    protected void unloadEntries(List newEntries) {
	List killList = new ArrayList();
	Iterator it = root.getEntries(true).values().iterator();
	while (it.hasNext()) {
	    Handle current = (Handle)it.next();
	    if (current instanceof DirHandle) {
		continue;
	    }
	    if (!(newEntries.contains(current))) {
		removeHandle(current);
		if (verbose()) log.verbose(this, "unloaded dataset " + 
					   current.getCompleteName());
	    }
	}
    }

    protected void loadEntries(List newEntries) {
	Iterator it = newEntries.iterator();
	while (it.hasNext()) {
	    DataHandle current = (DataHandle)it.next();
	    if (this.get(current.getCompleteName()) == null) {
		addHandle(current);
		if (verbose()) log.verbose(this, "loaded dataset " + 
					   current.getCompleteName());
	    }
	}
    }

    protected void loadTempEntriesFromStore() {

	tempDeleteQueue = new LinkedList();
	File tempEntryFile = server.getStore().get(this, TEMP_ENTRY_FILE);
	if (!tempEntryFile.exists()) {
	    log.info(this, "no temp entries to reload");
	    return;
	}
	    
	try {
	    ObjectInputStream entryStream = 
		new ObjectInputStream
		    (new FileInputStream
			(tempEntryFile));
	    Collection restoredEntries = (Collection)entryStream.readObject();
	    Iterator it = restoredEntries.iterator();
	    while (it.hasNext()) {
		addTemp((TempDataHandle)it.next(), false);
	    }
	    
	    log.info(this, "reloaded " + restoredEntries.size() + 
		     " temp entries from " + 
		     tempEntryFile.getAbsolutePath());
	    entryStream.close();	    
	} catch (IOException ioe) {
	    log.error(this,
		      "temp entries could not be reloaded from " + 
		      tempEntryFile.getAbsolutePath() + "; message: " + 
		      ioe.getMessage());
	} catch (ClassNotFoundException cnfe) {
	    log.error(this,
		      "temp entries could not be reloaded from " + 
		      tempEntryFile.getAbsolutePath() + "; message: " + 
		      cnfe.getMessage());
	} finally {
	    if (tempDeleteQueue == null) {
		tempDeleteQueue = new LinkedList();
	    }
	}
    }

    protected void loadCatalogFromStore() {

	File catalogFile = server.getStore().get(this, CATALOG_FILE);
	if (!catalogFile.exists()) {
	    log.info(this, "no catalog entries to reload");
	    return;
	}
	    
	try {
	    ObjectInputStream entryStream = 
		new ObjectInputStream
		    (new FileInputStream
			(catalogFile));
	    Collection oldEntries = (Collection)entryStream.readObject();
	    entryStream.close();	    
	    Iterator it = oldEntries.iterator();
	    while (it.hasNext()) {
		DataHandle data = (DataHandle)it.next();
		addHandle(data);
	    }
	    log.info(this, "reloaded " + oldEntries.size() + 
		     " catalog entries from " + 
		     catalogFile.getAbsolutePath());
	} catch (IOException ioe) {
	    log.error(this,
		      "temp entries could not be reloaded from " + 
		      catalogFile.getAbsolutePath() + "; message: " + 
		      ioe.getMessage());
	} catch (ClassNotFoundException cnfe) {
	    log.error(this,
		      "temp entries could not be reloaded from " + 
		      catalogFile.getAbsolutePath() + "; message: " + 
		      cnfe.getMessage());
	} 
    }

    protected void saveTempEntriesToStore() {
	try {
	    File tempEntryFile = 
		server.getStore().get(this, TEMP_ENTRY_FILE);
	    if (debug()) log.debug(this, "writing " + tempDeleteQueue.size() +
				   " temp entries to " +
				   tempEntryFile.getAbsolutePath());
	    ObjectOutputStream entryStream =
		new ObjectOutputStream
		    (new FileOutputStream
			(tempEntryFile));
	    entryStream.writeObject(tempDeleteQueue);
	    entryStream.close();
	} catch (IOException ioe) {
	    log.error(this,
		      "saving to persistence mechanism failed; " +
		      "temp entries will not persist after reboot; message: " +
		      ioe);
	}
    }

    public void saveCatalogToStore() {
	try {
	    synch.lock(); // synchronization added since this is public
	    File catalogFile = server.getStore().get(this, CATALOG_FILE);
	    Collection entries = new HashSet(root.getEntries(true).values());
	    if (debug()) log.debug(this, "writing " + entries.size() +
				   " catalog entries to " +
				   catalogFile.getAbsolutePath());
	    ObjectOutputStream entryStream =
		new ObjectOutputStream
		    (new FileOutputStream
			(catalogFile));
	    entryStream.writeObject(entries);
	    entryStream.close();
	} catch (IOException ioe) {
	    log.error(this,
		      "saving to persistence mechanism failed; " +
		      "temp entries will not persist after reboot; message: " +
		      ioe);
	} finally {
	    synch.release();
	}
    }

    protected void checkLimits() {
	while (tempEntryLimit != 0 && 
	       tempDeleteQueue.size() > tempEntryLimit) {
	    removeTemp("entry limit exceeded", null);
	}

	while (tempStorageLimit != 0 && 
	       tempStorage > (tempStorageLimit * 1e6)) {
	    removeTemp("storage limit exceeded", null);
	}

	long now = System.currentTimeMillis();
	long limit = tempAgeLimit * 60 * 60 * 1000;
	while (tempAgeLimit != 0) {
	    try {
		TempDataHandle current = 
		    (TempDataHandle)tempDeleteQueue.getFirst();
		long age = now - current.getCreateTime();
		if (age < limit) {
		    break;
		} 
		removeTemp("age limit exceeded", null);
	    }
	    catch (NoSuchElementException nsee) {
		if (debug()) log.debug(this,
				       "Nothing left in the tempDeleteQueue");
		break;
  
	    }		
	}
    }

    protected void removeTemp(String reason, TempDataHandle tempHandle) {
	synch.lockExclusive();
	if (tempHandle == null) {
	    tempHandle = (TempDataHandle)tempDeleteQueue.removeFirst();
	}
	DataHandle[] handles = tempHandle.getDataHandles();
	log.info(this, reason + "; deleting temp data " + handles[0]);
	for (int i = 0; i < handles.length; i++) {
	    handles[i].getSynch().lockExclusive();
	    tempEntries.remove(handles[i].getCompleteName());
	    handles[i].getSynch().release();
	}
	tempStorage -= tempHandle.getStorageSize();
	tempHandle.deleteStorage();
	synch.release();
    }
	

    protected final static String TEMP_ENTRY_FILE = "temp_entry_data.obj";
    protected final static String CATALOG_FILE = "catalog_data.obj";

    protected ExclusiveLock synch;

    protected DirHandle root;
    protected DirHandle tempEntries;

    protected LinkedList tempDeleteQueue;

    protected long tempStorage;

    protected long tempStorageLimit;
    protected long tempEntryLimit;
    protected long tempAgeLimit;

}
