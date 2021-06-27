/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;
import java.util.*;

import org.iges.anagram.AnagramException;
import org.iges.anagram.AnagramError;
import org.iges.anagram.DataHandle;

/** Information used by the GradsTool to provide access to a given
 *  data object, stored as the ToolInfo object in DataHandles. */
public class GradsDataInfo
    implements Serializable {

    // Basic dataset types
    public static final int GRID = 0;
    public static final int STN = 1;

    // constants to represent the GrADS binaries
    public static final int CLASSIC = 0;
    public static final int DODS = 1;

    /** Creates an object containing the necessary info to 
     *  access and serve a GrADS dataset.
     */
    public GradsDataInfo(String dodsName,
			 int gradsBinaryType,
			 String gradsArgument,
			 File descriptorFile,
			 File sourceFile,
			 File userDAS,
			 String docURL,
			 String overrideTitle,
			 boolean directSubset,
			 List metadataFilterList,
			 List metadataList,
                         boolean levels,
                         boolean ensemble,
			 String format)
	throws AnagramException {

	this.dodsName = dodsName;
	this.gradsBinaryType = gradsBinaryType;
	this.gradsArgument = gradsArgument;
	this.descriptorFile = descriptorFile;
	this.sourceFile = sourceFile;
	this.userDAS = userDAS;
	this.title = overrideTitle;
	this.docURL = docURL;
	this.directSubset = directSubset;
	this.metadataFilterList = Collections.unmodifiableList(metadataFilterList);
	this.metadataList = Collections.unmodifiableList(metadataList);
	this.levels = levels;
	this.ensemble = ensemble;
	this.format = format;
	readDescriptor();

	if (title == null || title.equals("")) {
	    title = dodsName + " (untitled)"; 
	    // want to always have some kind of title field, 
	    // because THREDDS uses it as the dataset name
	}

    }

    /** Location on disk of the supplemental DAS, or null if none */
    public File getUserDAS() {
	return userDAS;
    }

    /** A URL pointing to additional documentation for this dataset,
     *  or null if none */
    public String getDocURL() {
	return docURL;
    }

    /** The identifier to be used in generating the DDS for this data
     *  object. Usually identical to the complete name of the data handle
     *  that owns this info object. But for analysis results, 
     *  the actual dataset name may not be 
     *  a legal DODS identifier, so this field contains a usable
     *  substitute. */
    public String getDODSName() {
	return dodsName;
    }

    /** The argument passed to GrADS to open this dataset (a filename or
     *  url).
     */
    public String getGradsArgument() {
	return gradsArgument;
    }

    /** Location on disk of a GrADS descriptor file (which may be only a 
     *  dummy) describing this dataset.
     */
    public File getDescriptorFile() {
	return descriptorFile;
    }

    /** Location of the dataset, if stored on disk */
    public File getSourceFile() {
	return sourceFile;
    }

    /** The type of GrADS binary that should be used to open this dataset.
     */
    public int getGradsBinaryType() {
	return gradsBinaryType;
    }

    /** The format of this dataset, either 'ctl', 'nc', 'hdf', or 'dods'
     */
    public String getFormat() {
	return format;
    }

    /** Whether this is a station or gridded dataset. */
    public int getDataType() {
	return dataType;
    }

    /** The title of the dataset. Equivalent to DataHandle.getDescription(). 
     *  Normally this is extracted from the 
     *  CTL file, but for analysis results
     *  the title is generated by the GDS. 
     */
    public String getTitle() {
	return title;
    }

    /** Indicates whether server should attempt to read data directly
     *  from the datafile, without invoking GrADS
     */
    public boolean isDirectSubset() {
	return directSubset;
    }

    public List getMetadataFilters() {
	return metadataFilterList;
    }

    public List getMetadata() {
	return metadataList;
    }

    /** Whether the dataset has a vertical dimension. Needed to
     *  properly understand grid requests during subsetting.
     */
    public boolean hasLevels() {
	return levels;
    }

    /** Whether the dataset has an ensemble dimension. Needed to
     *  properly understand grid requests during subsetting.
     */
    public boolean hasEnsemble() {
	return ensemble;
    }

    /** Stores information needed to properly understand grid requests
     *  during subsetting. This information is determined at extract
     *  time rather than at import time, so cannot be passed th the
     *  constructor.
     */
    public void setLevels(boolean levels) {
	this.levels = levels;
    }

    /** Stores information needed to properly understand grid requests
     *  during subsetting. This information is determined at extract
     *  time rather than at import time, so cannot be passed to the
     *  constructor.
     */
    public void setEnsemble(boolean ensemble) {
	this.ensemble = ensemble;
    }

    /** Stores the information needed for the direct subset 
     *  mechanism. This information is determined at extract time rather 
     *  than at import time, so cannot be passed to the constructor.
     */
    public void setCTL(int xSize,
		       int ySize,
		       int zSize,
		       int tSize,
		       int eSize,
		       List vars,
		       List levels) {
	ctl = new CTL(xSize, ySize, zSize, tSize, eSize, vars, levels);
    }

    /** Returns an object containing the information needed 
     *  for the direct subset mechanism.
     */
    public CTL getCTL() {
	return ctl;
    }
    
    /** Parses the descriptor file for the dataset to determine
     *  whether it is a station or gridded dataset, and extract the
     *  title.  The request handler must know whether the dataset is
     *  station or gridded before the metadata is officially
     *  extracted, in order to call the correct extracter module. The title
     *  must also be read ahead of time in order to produce directory
     *  listings. 
     */
    protected void readDescriptor() throws AnagramException {

	try { 
	    BufferedReader r = 
		new BufferedReader
		    (new FileReader
			(descriptorFile));

	    dataType = GRID;
	    String line;
	    while ((line = r.readLine()) != null) {
		String lineNoCase = line.toLowerCase().trim();
		if (title == null && lineNoCase.startsWith("title ")) {
		    title = line.substring(6).trim();
		}
		if (lineNoCase.startsWith("dtype") && 
		    ((lineNoCase.indexOf("station") >= 0) ||
		     (lineNoCase.indexOf("bufr")) >= 0)){
		    dataType = STN;
		} 
	    }

	    r.close();
	    
	} catch (FileNotFoundException fnfe) {
	    throw new AnagramException
		("nonexistent descriptor file " + 
		 descriptorFile.getAbsolutePath());
	    
	} catch (IOException ioe) {
	    throw new AnagramException
		("io error creating reading " + 
		 descriptorFile.getAbsolutePath(), ioe);
	}
    }
	
    protected String dodsName;
    protected int dataType;
    protected int gradsBinaryType;
    protected String gradsArgument;
    protected File userDAS;
    protected File descriptorFile;
    protected String docURL;
    protected File sourceFile;
    protected long createTime;
    protected String title;
    protected boolean directSubset;
    protected List metadataFilterList;
    protected List metadataList;
    protected boolean levels;
    protected boolean ensemble;
    protected CTL ctl;
    protected String format;

    /** Holds the information needed for the direct subset mechanism.
     */
    public class CTL 
	implements Serializable {

	private CTL(int xSize,
		    int ySize,
		    int zSize,
		    int tSize,
		    int eSize,
		    List vars,
		    List levels) {
	    this.xSize = xSize;
	    this.ySize = ySize;
	    this.zSize = zSize;
	    this.tSize = tSize;
	    this.eSize = eSize;
	    this.vars = vars;
	    this.levels = levels;
	    this.xySize = (long)xSize * (long)ySize;
	    this.xyzSize = (long)xySize * getLevelsUpTo(levels.size());
	    this.xyztSize = (long)tSize * (long)xyzSize; 
	}

	/** Returns the index of a given variable in the dataset */
	public int getVarIndex(String varName) {
	    for (int offset = 0; offset < vars.size(); offset++) {
		if (vars.get(offset).equals(varName)) {
		    return offset;
		}
	    }
	    throw new AnagramError("variable " + varName + 
			       " not found in parsed CTL info");
	}

	/** Returns the number of levels for a 
	 * given variable in the dataset. Returns 1 for level-independent
	 * variables since the purpose is to calculate storage used
	 * in the actual disk file. 
	 */
	public long getVarLevelCount(int varIndex) {
	    long levelCount = ((Long)levels.get(varIndex)).longValue();
	    if (levelCount == 0) {
		return 1;
	    } else {
		return levelCount;
	    }
	}

	/** Returns true if the specified variable has vertical levels */
	public boolean isVarVertical(int varIndex) {
	    long levelCount = ((Long)levels.get(varIndex)).longValue();
	    return (levelCount > 0);
	}	    

	/** Returns the size of the longitude dimension
	 */
	public int getXSize() {
	    return xSize;
	}
	/** Returns the size of the latitude dimension
	 */
	public int getYSize() {
	    return ySize;
	}
	/** Returns the size of the vertical dimension
	 */
	public int getZSize() {
	    return zSize;
	}
	/** Returns the size of the time dimension
	 */
	public int getTSize() {
	    return tSize;
	}
	/** Returns the size of the ensemble dimension
	 */
	public int getESize() {
	    return eSize;
	}

	/** Returns the sum of the vertical levels for variables
	 *  prior to the specified index 
	 */
	public long getLevelsUpTo(int varIndex) {
	    long sum = 0;
	    for (int i = 0; i < varIndex; i++) {
		sum += getVarLevelCount(i);
	    }
	    return sum;
	}

	int xSize;
	int ySize;
	int zSize;
	int tSize;
	int eSize;
	/** The number of elements in a single XY grid for this dataset */
	long xySize;
	/** The number of elements in a single XYZ grid for this dataset */
	long xyzSize;
	/** The number of elements in a single XYZT grid for this dataset */
	long xyztSize;
	List vars;
	List levels;	
	boolean hasLevels;
	boolean hasEnsembles;
    }
	
}
