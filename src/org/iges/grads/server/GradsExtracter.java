/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;
import java.text.*;
import java.util.*;

import org.iges.util.Range;
import org.iges.util.Strings;
import org.iges.anagram.*;

import dods.dap.*;

/** Shared interface for modules that extract and cache metadata for
 * GrADS datasets. I am bending my own rules here - this class is
 * inherits from Module, but it is not a reusable component. A new
 * extracter is created for each extraction operation. This wouldn't
 * be hard to fix by just putting all the parsing data into an inner
 * class. Also, the construction of the DAS and DDS documents could
 * be mostly abstracted out into this top-level class. But it would
 * only be worth bothering if another basic dataset "shape" (presumably swath?)
 * were to be added on top of grid and stn. 
 */
public abstract class GradsExtracter 
    extends AbstractModule {

    public String getModuleID() {
	return "extract";
    }

    public void configure(Setting setting) {
    }

    /** Extracts metadata for the dataset given, and stores it in a set
     *  of temporary files under the prefix given, for use by the 
     * request handling modules. 
     */
    public void parse(DataHandle data, 
		      GradsTaskModule tasker,
		      String storagePrefix) 
	throws AnagramException {

	this.tasker = tasker;
	this.storagePrefix = storagePrefix;
	this.data = data;
	this.gradsInfo = (GradsDataInfo)data.getToolInfo();
	this.dimValues = new HashMap();
	this.minValues = new Hashtable();
	this.maxValues = new Hashtable();
	this.resValues = new Hashtable();

	load();

	writeDDS();
	writeDAS();
	writeWebSummary();
	writeSubsetInfo();
	// Disabled for the time being until THREDDS 1.0 is ready
	//	writeTHREDDSTag();

    }

    /** Reads CTL file for dataset into internal structures */
    protected abstract void load()
	throws AnagramException;

    /** Takes the parsed metadata and writes a DAS  */
    protected abstract void writeDDS()
	throws AnagramException;

    /** Takes the parsed metadata and writes a ".info" page  */
    protected abstract void writeWebSummary()
	throws AnagramException;

    /** Caches any precalculated values for subsetting */
    protected abstract void writeSubsetInfo()
	throws AnagramException;

    /** Caches THREDDS catalog information for this dataset */
    /* (not fully implemented)
    protected abstract void writeTHREDDSTag()
	throws AnagramException;
    */


    protected void loadDatasetMetadata() {

	ensMetadata = new ArrayList();
	ctlMetadata = new ArrayList();
	sdfMetadata = new ArrayList();

	// Decode and read data into array
	File tempFile = new File(storagePrefix + ".attributes");
	BufferedReader dataStream = null;
	String line = null;
	try {

	    Task task = tasker.task(gradsInfo.getGradsBinaryType(),
			"attributes", new String[] {
			    tempFile.getAbsolutePath(),
			    gradsInfo.getGradsArgument()
			});
	    task.run();

	    dataStream = new BufferedReader(new FileReader(tempFile));

	    // Always should be at least one line of output
	    line = dataStream.readLine();
	    if (line.startsWith("Invalid")) {
		throw new AnagramException
		    ("can't extract metadata attributes; " +
		     "using pre-2.0.a2 binary");
	    }

	    if (line.startsWith("Ensemble")) {
		// Loop through following lines for Ensemble Attributes
		while (dataStream.ready()) {
		    line = dataStream.readLine().trim(); 
		    if ((line == null) || line.equals("")) {
			break;
		    }
		    StringTokenizer st = new StringTokenizer(line);
		    String var = st.nextToken();
		    String type = st.nextToken();
		    String name = st.nextToken();
		    String val = "";
		    if (st.hasMoreTokens()) {
			val = st.nextToken("").trim(); // rest of line
			// with initial blank removed
		    }
		    MetadataAttribute parsedAtt = 
			new MetadataAttribute(var, type, name, val);
		    if (debug()) debug("parsed ensemble attribute: " + 
				       parsedAtt);
		    
		    ensMetadata.add(parsedAtt);
		}
	    }
	    // Skip over blank line after Ensemble Attributess
	    while ((line != null) && line.trim().equals("")) {
		line = dataStream.readLine();
	    }

	    if (line.startsWith("No")) {
		if (debug()) debug("no descriptor attributes in dataset");

	    } else {
		
		// Loop through remaining lines if no error
		
		while (dataStream.ready()) {
		    line = dataStream.readLine().trim(); 
		    if ((line == null) || line.equals("")) {
			break;
		    }
		    
		    StringTokenizer st = new StringTokenizer(line);
		    String var = st.nextToken();
		    String type = st.nextToken();
		    String name = st.nextToken();
		    String val = "";
		    if (st.hasMoreTokens()) {
			val = st.nextToken("").trim(); // rest of line
			// with initial blank removed
		    }
		    
		    // handle multi-line continuations
		    while (val.endsWith(" \\")) {
			// the newline was already eaten by readLine()
			// check the first two characters for 
			// continuation symbol "> "
			dataStream.mark(2);
			if ((char) dataStream.read() == '>' && 
			    (char) dataStream.read() == ' ') {
			    val = val.substring(0, val.length() - 2);
			    val += "\n" + dataStream.readLine();
			} else {
			    // no "> " on next line, so " \" must have been 
			    // part of the original attribute value
			    dataStream.reset();
			    break;
			}
		    }	    
		    
		    // parens have been dropped since 1.9b4
		    if (var.equals("(global)") || var.equals("global")) {
			var = "";
		    }
		    
		    MetadataAttribute parsedAtt = 
			new MetadataAttribute(var, type, name, val);
		    if (debug()) debug("parsed descriptor attribute: " + 
				       parsedAtt);
		    
		    ctlMetadata.add(parsedAtt);
		}
	    }

    
	    while ((line != null) && line.trim().equals("")) {
		line = dataStream.readLine();
	    }

	    // parse native attributes (grads 1.9b4 and later, so 
	    // don't need some obsolete code from above

	    if (line == null || line.startsWith("No")) {
		if (debug()) debug("no native attributes in dataset");

	    } else {
		
		// Loop through remaining lines if no error

		List datasetFilters = gradsInfo.getMetadataFilters();

		while (dataStream.ready()) {
		    line = dataStream.readLine().trim(); 
		    if ((line == null) || line.equals("")) {
			break;
		    }
		    
		    StringTokenizer st = new StringTokenizer(line);
		    String var = st.nextToken();
		    String type = st.nextToken();
		    String name = st.nextToken();
		    String val = "";
		    if (st.hasMoreTokens()) {
			val = st.nextToken("").trim(); // rest of line
			// with initial blank removed
		    }
		    
		    if (var.equals("global")) {
			var = "";
		    }
		    
		    MetadataAttribute parsedAtt = 
			new MetadataAttribute(var, type, name, val);

		    if (debug()) debug("parsed native attribute: " + parsedAtt);
		    if (MetadataFilter.attributePassesFilters(parsedAtt, 
							      datasetFilters, 
							      false)) {
			  if (debug()) debug("native attribute approved: " + 
					     parsedAtt);
			  sdfMetadata.add(parsedAtt);
		      } else {
			  if (debug()) debug("native attribute rejected: " + 
					     parsedAtt);
		      } 
		}
	    }


	} catch (AnagramException ae) { 
	    error("attribute script failed: " + ae.getMessage());
	} catch (IOException ioe) {
	    error("no data in attribute list " + 
		  tempFile.getAbsolutePath());
	} catch (NoSuchElementException nsee) {
	    error("bad format in attribute list " + 
		  tempFile.getAbsolutePath() + ":\n>>>" + line);
	} finally {
	    // Clean up
	    try {
		dataStream.close();
	    } catch (Exception e) {}
	    // tempFile.delete();
	}
	
    }


    protected abstract void loadServerMetadata() throws AnagramException;

    protected void smAtt(String var, String type, String name, String val) 
	throws AnagramException {
	serverMetadata.add(new MetadataAttribute(var, type, name, val));
    }

    
    /** Takes the parsed metadata and writes a DAS file.  */
    protected void writeDAS() throws AnagramException {

	xmlMetadata = gradsInfo.getMetadata();
	loadDatasetMetadata();
	loadServerMetadata();

	DAS das = new DAS();
	// puts the global table first for cosmetic appeal
	AttributeTable global = new AttributeTable();
	das.addAttributeTable("NC_GLOBAL", global);

	if (debug()) debug("building das: adding " + xmlMetadata.size() + 
			   " attributes from config file");
	addMetadataToDAS(das, xmlMetadata);
	if (debug()) debug("building das: adding " + ensMetadata.size() + 
			   " ensemble attributes ");
	addMetadataToDAS(das, ensMetadata);
	if (debug()) debug("building das: adding " + ctlMetadata.size() + 
			   " attributes from descriptor");
	addMetadataToDAS(das, ctlMetadata);
	if (debug()) debug("building das: adding " + sdfMetadata.size() + 
			   " attributes from data file");
	addMetadataToDAS(das, sdfMetadata);
	if (debug()) debug("building das: adding " + serverMetadata.size() + 
			   " attributes from server");
	addMetadataToDAS(das, serverMetadata);

	try {
	    /* 
	    if (debug()) debug("building das: appending server attribute");
	    global.appendAttribute("dataServer", 
				   MetadataAttribute.parseType("String"),
				   "\"" + server.getImplName() +
				   " " + server.getImplVersion() + "\"");
	    */
	    if (debug()) debug("building das: appending history attribute");
	    global.appendAttribute("history", 
				   MetadataAttribute.parseType("String"),
				   "\"" + getHistoryString() + "\"");
	    if (debug()) debug("building das: appending Conventions attribute");
	    global.appendAttribute("Conventions", 
				   MetadataAttribute.parseType("String"),
				   "\"GrADS\"");
	} catch (AttributeExistsException aee) {
	} catch (AttributeBadValueException aee) {}


	String dasStorage = storagePrefix + ".das";
	if (debug()) log.debug(this, "writing new das to " + dasStorage);

	PrintWriter out;
	try {
	    out = new PrintWriter(new FileWriter(dasStorage));
	    das.print(out);
	    out.close();
	} catch (IOException ioe) {
	    throw new AnagramException("error writing das for " + 
				       data.getName());
	}

	File userDASFile = 
	    ((GradsDataInfo)data.getToolInfo()).getUserDAS();
	if (userDASFile != null && userDASFile.exists()) {
	    try {
		DAS finalDAS = new DAS();
		try {
			finalDAS.parse(new BufferedInputStream
			    (new FileInputStream(dasStorage)));
			finalDAS.parse(new BufferedInputStream
			    (new FileInputStream(userDASFile)));
		} catch (FileNotFoundException e) {
		    throw new AnagramException("user DAS not found " +  
					       userDASFile);
		    
		} catch (Exception e) {
		    throw new AnagramException("error parsing user DAS", 
					       e);
		    
		}
		out = new PrintWriter(new FileWriter(dasStorage));
		finalDAS.print(out);
		out.close();
	    } catch (IOException ioe) {
		throw new AnagramException("error writing das for " + 
					   data.getName());
	    } finally {
		out.close();
	    }
	}
    }

    protected void addMetadataToDAS(DAS das, List metadata) {
	Iterator it = metadata.iterator();
	while (it.hasNext()) {
	    MetadataAttribute att = (MetadataAttribute)it.next();
	    AttributeTable table = getTableForVar(das, att.var);
	    if (table.getAttribute(att.name) != null) {
		if (debug()) debug("building das: " + 
				   att.var + "." + att.name + 
				   " already present");
		it.remove();
	    }
	}

	it = metadata.iterator();
	while (it.hasNext()) {
	    MetadataAttribute att = (MetadataAttribute)it.next();
	    AttributeTable table = getTableForVar(das, att.var);
	    try {	    
		table.appendAttribute(att.name, att.intType, att.val);
	    } catch (AttributeExistsException aee) {
	    } catch (AttributeBadValueException aee) {}
	}
    }
	
	protected AttributeTable getTableForVar(DAS das, String var) {
	    String tableName = var;
	    if (tableName.equals("")) {
		tableName = "NC_GLOBAL";
	    }
	    AttributeTable table = das.getAttributeTable(tableName);
	    if (table == null) {
		table = new AttributeTable();
		das.addAttributeTable(tableName, table);
	    }
	    return table;
	}


    protected void printDODSAttributes(PrintWriter out, 
				       String var) {
	for (int i = 0; i < metadata.size(); i++) {
	    MetadataAttribute att = (MetadataAttribute) metadata.get(i);
	    if (att.var.equals(var)) {
		out.print("        " + att.type + " " + 
			  att.name + " " + att.val + ";\n");
	    }
	}
	
    }

    protected String getHistoryString() {
	return new Date(data.getCreateTime()) + " : imported by " + server.getImplName() + " " + server.getImplVersion();
    }

    /** Helper for writeWebInfo() */
    protected void printDim(PrintWriter info, 
			    String dim, 
			    String longName, 
			    String units,
			    String resUnits, 
			    int size) {

	info.print("     <tr>\n");
	info.print("       <td valign=\"Bottom\" colspan=\"2\">");
	info.print("<b>");
	info.print(longName);
	info.print(":</b></td>\n");
	info.print("       <td>       \n");
	info.print("      <div align=\"Center\">");
	info.print(((String)minValues.get(dim)).toUpperCase());
	info.print(units);
	info.print(" to ");
	info.print(((String)maxValues.get(dim)).toUpperCase());
	info.print(units);
	info.print("</div>\n");
	info.print("       </td>\n");
	info.print("       <td>&nbsp;(");
	info.print(size);
	info.print(" points");
	if (size > 1 && resUnits != null) {
	    info.print(", avg. res. ");
	    info.print(doubleFormat.format(resValues.get(dim)));
	    info.print(resUnits);
	}
	info.print(")\n");
	info.print("       </td>\n");
	info.print("     </tr>\n");
    }

    /** Invokes GrADS to print a complete list of values for a given
     *  dimension, then parses the values into an array
     * @param dim one of "lat, "lon", "lev", "time", "ens"
     */ 
    protected double[] loadDimValues(String dim, int size) 
	throws AnagramException {
	
	if (debug()) log.debug(this, "loading " + size + " values for " + 
			       dim + " in " + data);

	// This patches together the shell command to fire up GrADS and run dimension.gs
	double[] values = new double[size];
	File tempFile = new File(storagePrefix + "." + dim + ".output");
	Task task = tasker.task(gradsInfo.getGradsBinaryType(),
				"dimension", new String[] {
				    tempFile.getAbsolutePath(),
				    gradsInfo.getGradsArgument(),
				    dim,
				    "1",
				    String.valueOf(size) 
				});

	task.run();
	    
	// Decode and read data into array
	BufferedReader dataStream = null;
	try {
	    dataStream = new BufferedReader(new FileReader(tempFile));
	    String line; // Each line contains one value
	    for (int i = 0; i < size; i++) {
		line = dataStream.readLine(); 
		if (dim.equals("time")) {
		    values[i] = convertGradsDateToCOARDS(line);
		} else {
		    values[i] = Double.valueOf(line).doubleValue();
		}
		if (i == 0) {
		    minValues.put(dim, line.toLowerCase());
		} 
		if (i == (size - 1)) {
		    maxValues.put(dim, line.toLowerCase());
		}
	    }

	    // Calculate resolution 
	    double min = values[0];
	    double max = values[values.length - 1];
	    double res = Math.abs((max - min) / (values.length - 1));

	    // Used by printDim().
	    // The normalized value tNormRes and tNormResUnits never get used
	    this.tNormRes = String.valueOf(normalTime(res));
	    this.tNormResUnits = normalTimeUnit(res);
	    this.resValues.put(dim, new Float(res));
	    
	} catch (IOException ioe) {
	    throw new AnagramException("not enough " + dim + " data in " + 
				       tempFile.getAbsolutePath());
	} catch (NumberFormatException nfe) {
	    throw new AnagramException("bad data for " + dim + " in " + 
				       tempFile.getAbsolutePath());
	} finally {
	    // Clean up
	    try {
		dataStream.close();
	    } catch (Exception e) {}
	    tempFile.delete();
	}

	return values;
    }


    /** Writes an array of coordinate data to a temporary storage
     *  file for use by the subsetting modules
     * @param dim one of "lat, "lon", "lev", "time", "ens"
     */ 
    protected void writeDim(String dim)
	throws AnagramException {
	
	File dimStorage = new File(storagePrefix + "." + dim);

	if (debug()) log.debug(this, "writing values for " + 
			       dim + " to " + dimStorage.getAbsolutePath());

	try {
	    DataOutputStream out = new DataOutputStream
		(new BufferedOutputStream
		    (new FileOutputStream
			(dimStorage)));
	    
	    double[] values = (double[])dimValues.get(dim);

	    for (int i = 0; i < values.length; i++) {
		out.writeDouble(values[i]);
	    }

	    out.close();

	} catch (IOException ioe) {
	    throw new AnagramException("error writing data for " + dim);
	}
	
    }

    /* Choose best unit for time (supplied in days), and return value */
    protected double normalTime(double t) {
	return t / tUnitFactors[normalTimeUnitIndex(t)];
    }

    /* Choose best unit for time (supplied in days), and return units */
    protected String normalTimeUnit(double t) {
	return tUnits[normalTimeUnitIndex(t)];
    }

    protected int normalTimeUnitIndex(double tDays) {
	int index = 0;
	double t;
	for (int i = 0; i < tUnitFactors.length; i++) { 
	    if (tDays / tUnitFactors[i] > 1) {
		index =  i;
	    }
	}
	return index;
    }

    protected final static String[] tUnits = {
	"days", "hours", "minutes"
    };

    /* multiplier to convert <units> to days */
    protected final static double[] tUnitFactors = {
	1.0, 
	24.0,
	24.0 * 60.0
    };    
    
    protected final static NumberFormat doubleFormat =
	NumberFormat.getInstance(); 
    { 
	doubleFormat.setMinimumFractionDigits(1);
	doubleFormat.setMaximumFractionDigits(3);
    }



    /** Converts a GrADS date, a string with format yyyy:M:d:H or 
     * yyyy:M:d:H:m, to a udunits-compatible COARDS date, which is a 
     * floating point number 
     * in units of days since 1970-01-01T00:00:00.000Z
     */
    protected double convertGradsDateToCOARDS(String dateString) {
    
	Date parsedDate = Range.parseGradsFormat(dateString);

	// Set origin date to 01/01/1970, 12am GMT
	Calendar origin = new GregorianCalendar(1970, // year
						0, // month, 0-based
						1  // day
						);
	origin.setTimeZone(TimeZone.getTimeZone("GMT"));
	// Calculate difference in milliseconds 
	long difference = parsedDate.getTime() - origin.getTime().getTime();

	// Convert milliseconds to days
	double coardsDate = (double)difference / (double)(1000 * 60 * 60 * 24);
	   
	return coardsDate;
    }

    String title;

    double missingData;

    Map dimValues;
    
    /** minimum values, in string form, for each dimension; 
     *  indexed by dimension name */
    Hashtable minValues;
    /** maximum values, in string form, for each dimension; 
     *  indexed by dimension name */
    Hashtable maxValues;

    /** grid resolutions, in string form, for each dimension;
     *  indexed by dimension name */
    Hashtable resValues;

    String tMapping;
    int tSize;
    String tStep;
    String tNormRes;
    String tNormResUnits;

    protected GradsTaskModule tasker;
    protected String storagePrefix;
    protected DataHandle data;
    protected GradsDataInfo gradsInfo;
    protected List metadata;
    protected List xmlMetadata;
    protected List ensMetadata;
    protected List ctlMetadata;
    protected List sdfMetadata;
    protected List serverMetadata;
    protected DAS das;
    
}
