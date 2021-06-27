/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;

import org.iges.util.FileResolver;

import org.iges.anagram.*;

/** Creates data handles for GrADS datasets specified by 
 *  XML configuration tags.
 */
public class GradsImportModule 
    extends AbstractModule {

    public String getModuleID() {
	return "importer";
    }

    public GradsImportModule(GradsTool tool) {
	this.tool = tool;
    }

    protected GradsTool tool;

    public void configure(Setting setting) {
	store = server.getStore();
    }
 
    public DataHandle[] doImport(Setting setting) {
	List handles = getHandlesFromXML(setting.getXML(), "/");
	return (DataHandle[])handles.toArray(new DataHandle[0]);
    }


    private List getHandlesFromXML(Element tag, String baseDir) {

	List returnVal = new ArrayList();
	
	NodeList children = tag.getChildNodes();
	// Run through each tag 
	for (int i = 0; i < children.getLength(); i++) {
	    if (!(children.item(i) instanceof Element)) {
		continue;
	    }
	    Element current = (Element)children.item(i);
	    
	    String tagName = current.getTagName();
	    
	    if (tagName.equals("mapdir")) {
		if (!current.hasAttribute("name")) {
		    continue;
		}
		String subDir  = current.getAttribute("name");
		if (subDir.startsWith("/")) {
		    subDir = subDir.substring(1);
		}
		if (subDir.endsWith("/")) {
		    subDir = subDir.substring(0, subDir.length() - 1);
		}
		List handles = 
		    getHandlesFromXML(current, baseDir + subDir + "/");
		if (handles != null) {
		    returnVal.addAll(handles);
		}

	    } else if (tagName.equals("dataset")) {
		DataHandle handle = parseDatasetTag(current, baseDir);
		if (handle != null) {
		    returnVal.add(handle);
		}
	    } else if (tagName.equals("datadir")) {
		List handles = parseDatadirTag(current, baseDir);
		if (handles != null) {
		    returnVal.addAll(handles);
		}
	    } else if (tagName.equals("datalist")) {
		List handles = parseDatalistTag(current, baseDir);
		if (handles != null) {
		    returnVal.addAll(handles);
		}
	    }
	}

	return returnVal;
    }



    private DataHandle parseDatasetTag(Element tag, String mapDir) {
	// Get name for entry
	String name  = tag.getAttribute("name");

	// Get documentation url if any
	String docURL = null;
	if (tag.hasAttribute("doc")) {
	    docURL = tag.getAttribute("doc");
	}
	
	// Get format for entry
	String format = "ctl";
	if (tag.hasAttribute("format")) {
	    format = tag.getAttribute("format");
	}
	
	// Get filename for entry
	if (!tag.hasAttribute("file") && !tag.hasAttribute("url")) {
	    log.error(this, "skipping dataset " + name + 
			     "; no file or url provided");
	    return null;
	}
	String file = tag.getAttribute("file");
	if (file.equals("")) {
	    file = tag.getAttribute("url");
	}

	File userDAS = null;
	if (tag.hasAttribute("das")) {
	    userDAS = FileResolver.resolve(server.getHome(), 
					   tag.getAttribute("das"));
	}

	boolean directSubset = 
	    tag.hasAttribute("direct_subset") 
	    && (tag.getAttribute("direct_subset").equals("true"));

	File sourceFile = null;
	if (tag.hasAttribute("source")) {
	    sourceFile = FileResolver.resolve(server.getHome(), 
					      tag.getAttribute("source"));
	}

	try { 
	    List metadataFilters = 
		parseMetadataFilters(tag.getElementsByTagName("metadata-filter"));
	    
	    List metadataAttributes = 
		parseMetadataAttributes(tag.getElementsByTagName("metadata"));	

	    if (name.equals("")) {
		name = file.substring(file.lastIndexOf("/") + 1);
		if (name.endsWith(".ctl")) {
		    name = name.substring(0, name.length() - 4);
		}
	    }
	    if (name.startsWith("/")) {
		name = mapDir + name.substring(1);
	    } else {
		name = mapDir + name;
	    }
	    
	    return createHandle(name, file, userDAS, docURL, format, 
				directSubset, sourceFile, metadataFilters,
				metadataAttributes);
	} catch (AnagramException ae) {
	    log.error(this, "can't import " + name + "; " + 
		      ae.getMessage());
	    return null;
	}			
    }

    private List parseDatadirTag(Element tag, String mapDir) {
	// Get filename for entry
	if (!tag.hasAttribute("file")) {
	    log.error(this, "skipping datadir" + 
			     "; no filename provided");
	    return null;
	}
	File dir = FileResolver.resolve(server.getHome(), 
					    tag.getAttribute("file"));
	if (!dir.isDirectory()) {
	    log.error(this, "skipping datadir " + dir + 
			     "; not a directory");
	    return null;
	}
	// Get format for entry
	String format = "ctl";
	if (tag.hasAttribute("format")) {
	    format = tag.getAttribute("format");
	}
	// Check recurse
	boolean recurse = true;
	if (tag.hasAttribute("recurse") &&
	    tag.getAttribute("recurse").equals("false")) {
	    recurse = false;
	}

	// Set prefix and suffix
	String prefix = tag.getAttribute("prefix");
	String suffix = tag.getAttribute("suffix");

	// Get documentation url if any
	String docURL = null;
	if (tag.hasAttribute("doc")) {
	    docURL = tag.getAttribute("doc");
	}

	File userDAS = null;
	if (tag.hasAttribute("das")) {
	    userDAS = FileResolver.resolve(server.getHome(), 
					   tag.getAttribute("das"));
	}

	boolean directSubset = 
	    tag.hasAttribute("direct_subset") 
	    && (tag.getAttribute("direct_subset").equals("true"));

	String sourcePrefix = tag.getAttribute("source_prefix");
	String sourceSuffix = tag.getAttribute("source_suffix");

	String basePath = mapDir;
	if (tag.hasAttribute("name")) {
	    String name  = tag.getAttribute("name");
	    if (name.startsWith("/")) {
		name = name.substring(1);
	    }
	    if (!name.endsWith("/")) {
		name += "/";
	    }
	    basePath += name;
	} 

	try {

	List metadataFilters = 
	    parseMetadataFilters(tag.getElementsByTagName("metadata-filter"));
	
	List metadataAttributes = 
	    parseMetadataAttributes(tag.getElementsByTagName("metadata"));
	
	return loadDir(dir, basePath, prefix, suffix, 
		       userDAS, docURL, format, recurse, directSubset, 
		       sourcePrefix, sourceSuffix, 
		       metadataFilters, metadataAttributes);
	} catch (AnagramException ae) {
	    log.error(this, "can't import directory " + dir + "; " + 
		      ae.getMessage());
	    return null;
	}

    }

    private List loadDir(File dir, 
			 String basePath,
			 String prefix,
			 String suffix, 
			 File userDAS,
			 String docURL,
			 String format, 
			 boolean recurse,
			 boolean directSubset,
			 String sourcePrefix,
			 String sourceSuffix,
			 List metadataFilters,
			 List metadataAttributes) {
	if (verbose()) log.verbose(this, "searching directory " + 
				   dir.getAbsolutePath());
	List returnVal = new ArrayList();
	File[] contents = dir.listFiles();
	for (int i = 0; i < contents.length; i++) {
	    if (recurse && contents[i].isDirectory()) {
		String subPath = basePath + contents[i].getName() + "/";
		List subDirEntries = 
		    loadDir(contents[i], subPath, prefix, suffix, userDAS, 
			    docURL, format, recurse, directSubset, 
			    sourcePrefix, sourceSuffix, 
			    metadataFilters, metadataAttributes);
		returnVal.addAll(subDirEntries);
	    }
	    if (contents[i].isFile() && 
		contents[i].getName().startsWith(prefix) && 
		contents[i].getName().endsWith(suffix)) {
		
		// remove prefix and suffix from name
		String name = contents[i].getName().substring
		    (prefix.length());
		name = name.substring(0, name.length() - suffix.length());

		File sourceFile = null;
		if (directSubset) {
		    String sourceFilename = 
			sourcePrefix + name + sourceSuffix;
		    sourceFile = FileResolver.resolve(dir.getAbsolutePath(), 
						      sourceFilename);
		    if (debug()) debug("source file is " + sourceFile);
		}

		if (debug()) debug("truncated " + contents[i].getName() +
				   " to " + name);

		name = basePath + name;
		try {
		    returnVal.add(createHandle(name, 
					       contents[i].getAbsolutePath(), 
					       userDAS, 
					       docURL,
					       format,
					       directSubset, 
					       sourceFile,
					       metadataFilters,
					       metadataAttributes));
		} catch (AnagramException ae) {
		    log.error(this, "can't import " + name + "; " + 
			      ae.getMessage());
		}			
	    }
	}
	return returnVal;
    }

    private List parseDatalistTag(Element tag, String mapDir) {

	// Get documentation url if any
	String docURL = null;
	if (tag.hasAttribute("doc")) {
	    docURL = tag.getAttribute("doc");
	}

	File userDAS = null;
	if (tag.hasAttribute("das")) {
	    userDAS = FileResolver.resolve(server.getHome(), 
					   tag.getAttribute("das"));
	}

	boolean directSubset = 
	    tag.hasAttribute("direct_subset") 
	    && (tag.getAttribute("direct_subset").equals("true"));


	// Get filename for entry
	if (!tag.hasAttribute("file")) {
	    log.error(this, "skipping datalist" + 
			     "; no filename provided");
	    return null;
	}
	File listFile = FileResolver.resolve(server.getHome(), 
						 tag.getAttribute("file"));
	if (verbose()) log.verbose(this, "reading list file " + 
				   listFile.getAbsolutePath());
	
	String basePath = mapDir;
	if (tag.hasAttribute("name")) {
	    String name  = tag.getAttribute("name");
	    if (name.startsWith("/")) {
		name = name.substring(1);
	    }
	    if (!name.endsWith("/")) {
		name += "/";
	    }
	    basePath += name;
	} 
	// Get format for datasets
	String format = "ctl";
	if (tag.hasAttribute("format")) {
	    format = tag.getAttribute("format");
	}

	// Get format of list
	boolean readNames = false;
	if (tag.hasAttribute("list_format") && 
	    tag.getAttribute("list_format").equals("name")) {
	    readNames = true;
	}

	try {

	    List metadataFilters = 
		parseMetadataFilters(tag.getElementsByTagName("metadata-filter"));
	    
	    List metadataAttributes = 
		parseMetadataAttributes(tag.getElementsByTagName("metadata"));
	    
	    List returnVal = new ArrayList();
	    BufferedReader listReader = 
		new BufferedReader
		    (new FileReader
			(listFile));
	    int lineNo = 0;
	    while (true) {
		String currentLine = listReader.readLine();
		if (currentLine == null) {
		    break;
		}
		lineNo++;
		currentLine = currentLine.trim();
		if (currentLine.startsWith("*") ||
		    currentLine.equals("")) {
		    continue;
		}
		StringTokenizer tokens = new StringTokenizer(currentLine);
		String name = null;
		String file = null;
		try {
		    if (readNames) {
			name = basePath + tokens.nextToken();
			file = tokens.nextToken();
		    } else {
			file = tokens.nextToken();
			name = basePath + file;
		    }
		    try {
			returnVal.add(createHandle(name, file, userDAS, 
						   docURL, format, 
						   directSubset, null,
						   metadataFilters,
						   metadataAttributes
						 ));
		    } catch (AnagramException ae) {
			log.error(this, "can't import " + name + "; " + 
				  ae.getMessage());
		    }			
		} catch (NoSuchElementException nsee) {
		    log.error(this, "line " + lineNo + 
			      " of  datalist " + 
			      listFile.getAbsolutePath() +
			      " skipped due to bad formatting");
		}
	    }
	    return returnVal;

	} catch (AnagramException ae) {
	    log.error(this, "can't import datalist" + listFile +
		      "; " + ae.getMessage());
	    return null;
	} catch (IOException ioe) {
	    log.error(this, "skipping datalist" + listFile.getAbsolutePath() +
			     " due to problem while reading; " +
			     ioe.getMessage());
	    return null;
	}	    
    }

    private List parseMetadataFilters(NodeList tags) 
	throws AnagramException {

	List returnVal = new ArrayList();
	
	// Run through each tag 
	for (int i = 0; i < tags.getLength(); i++) {
	    Element tag = (Element)tags.item(i);

	    boolean globalOnly = (tag.getAttribute("global_only").equals("true"));
	    
	    // Default for sendIfMatch is true. 
	    boolean sendIfMatch = (! tag.getAttribute("send").equals("true"));

	    try {
		MetadataFilter filter = new MetadataFilter
		    (tag.getAttribute("var_prefix"),
		     tag.getAttribute("var_suffix"),
		     tag.getAttribute("var_name"),
		     globalOnly,
		     tag.getAttribute("att_prefix"),
		     tag.getAttribute("att_suffix"),
		     tag.getAttribute("att_name"),
		     sendIfMatch);
		returnVal.add(filter);
		if (debug()) debug("parsed metadata filter:\n" + filter);
	    } catch (AnagramException ae) {
		throw new AnagramException("invalid metadata filter; " + 
					   ae.getMessage() + ":\n" + tag);
	    }

	}

	return returnVal;
    }

    private List parseMetadataAttributes(NodeList tags) 
	throws AnagramException {
	
	List returnVal = new ArrayList();
	
	// Run through each tag 
	for (int i = 0; i < tags.getLength(); i++) {
	    Setting tag = new Setting((Element)tags.item(i));

	    try {
		MetadataAttribute att = new MetadataAttribute
		    (tag.getAttribute("var"),
		     tag.getAttribute("type", "String"),
		     tag.getAttribute("name"),
		     tag.getAttribute("value"));
		returnVal.add(att);
		if (debug()) debug("parsed metadata attribute: " + att);
	    } catch (AnagramException ae) {
		throw new AnagramException("invalid metadata tag; " + 
					   ae.getMessage() + ":\n" + tag);
	    }
	}
	
	return returnVal;
    }



    public DataHandle createHandle(String name, 
				   String gradsArgument, 
				   File userDAS,
				   String docURL,
				   String format,
				   boolean directSubset,
				   File sourceFile,
				   List metadataFilters,
				   List metadataAttributes) 
	throws AnagramException {

	if (debug()) debug("direct_subset = " + directSubset);

	Handle handle = server.getCatalog().getLocked(name);
	if (handle instanceof DirHandle) {
	    throw new AnagramException("a directory exists by that name");
	}

	if (userDAS != null && !userDAS.exists()) {
	    throw new AnagramException("can't locate user DAS");
	}
	
	GradsDataInfo info = null;
	int gradsBinaryType;
	if (format.equals("dods") || format.equals("opendap")) {
	    gradsBinaryType = GradsDataInfo.DODS;
	} else {
	    gradsBinaryType = GradsDataInfo.CLASSIC;
	} 

	if (directSubset && gradsBinaryType != GradsDataInfo.CLASSIC) {
	    throw new AnagramException
		("direct subsetting can only be enabled when format is ctl");
	}

	if (directSubset && sourceFile == null) {
	    throw new AnagramException
		("data file must be specified with 'source' attribute " + 
		 "for direct subsetting");
	}

	File file = FileResolver.resolve(server.getHome(), gradsArgument);
	if (file.exists()) {
	    gradsArgument = file.getAbsolutePath();
	} else {
	    file = null;
	}

	if (handle != null) {
	    handle.getSynch().release();
	    DataHandle oldData = (DataHandle)handle;
	    GradsDataInfo oldInfo = (GradsDataInfo)oldData.getToolInfo();
	    if (oldInfo.getGradsBinaryType() == gradsBinaryType &&
		oldInfo.getGradsArgument().equals(gradsArgument) &&
		oldInfo.getFormat().equals(format) &&  
		oldInfo.isDirectSubset() == directSubset &&
		((oldInfo.getUserDAS() == null && userDAS == null) ||
		 (oldInfo.getUserDAS() != null && 
		  oldInfo.getUserDAS().equals(userDAS))) && 
		((oldInfo.getDocURL() == null && docURL == null) ||
		 (oldInfo.getDocURL() != null && 
		  oldInfo.getDocURL().equals(docURL))) && 
		 oldInfo.getMetadataFilters().equals(metadataFilters) &&
		 oldInfo.getMetadata().equals(metadataAttributes)
		 ) {
		if (debug()) debug("settings for " + 
				   oldData.getCompleteName() + 
				   " are unchanged");
		return oldData;
	    }

	}
	    

	if (format.equals("ctl")) {
	    // Get the required metadata from the data descriptor file 
	    if (file == null) {
		throw new AnagramException("can't find descriptor file " +
					   gradsArgument);
	    }
		
	    info = new GradsDataInfo(name.substring(1),
				     gradsBinaryType,
				     file.getAbsolutePath(),
				     file,
				     (sourceFile == null) ? file : sourceFile,
				     userDAS,
				     docURL,
				     null,
				     directSubset,
				     metadataFilters,
				     metadataAttributes,
				     false,   
				     false,
				     format); 
	} else {
	    // Create a data descriptor file from 'q ctlinfo' output
	    File descriptorFile = 
		makeDescriptorFile(name, gradsArgument, gradsBinaryType);
	    info = new GradsDataInfo(name.substring(1),
				     gradsBinaryType,
				     gradsArgument,
				     descriptorFile,
				     file,
				     userDAS,
				     docURL,
				     null,
				     directSubset,
				     metadataFilters,
				     metadataAttributes,
				     false,   
				     false,
				     format);
	}
	DataHandle newData = new DataHandle(name, 
					   info.getTitle(), 
					   info,
					   System.currentTimeMillis());
	log.info(this, "imported dataset " + name);
	return newData;
    }
	

    public File makeDescriptorFile(String name, 
				   String gradsArgument,
				   int gradsBinaryType) 
	throws ModuleException {

	String entryName = name + ".dummy.ctl";

	if (debug()) log.debug(this, "creating descriptor for " + name  + 
			       " from " + gradsArgument);

	File descriptorFile = store.get(this, entryName);

	if (debug()) log.debug(this, "CTL file for " + name + " will be " + 
			       descriptorFile.getAbsolutePath());

	String[] args = new String[] { descriptorFile.getAbsolutePath(), 
				   gradsArgument};

	Task task = tool.getTask().task(gradsBinaryType, "ctlinfo", args);

	try {
	    task.run();
	} catch (AnagramException ae) {
	    throw new ModuleException
		(this, "metadata extraction failed for " + gradsArgument, 
		 ae.getMessage());
	}
	if (!descriptorFile.exists() || descriptorFile.length() == 0) {
	    throw new ModuleException
		(this, "metadata extraction failed for " + gradsArgument);
	}
	return descriptorFile;
    }
    
    protected Store store;

    

}
