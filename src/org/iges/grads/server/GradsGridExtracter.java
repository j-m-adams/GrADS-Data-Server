/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;
import java.util.*;
import org.iges.util.Range;
import org.iges.util.Strings;
import org.iges.anagram.*;

/** Extracts and caches metadata for a gridded GrADS dataset. */
public class GradsGridExtracter
    extends GradsExtracter {

    protected void writeSubsetInfo() 
	throws AnagramException {
	
	writeDim("lon");
	writeDim("lat");
	writeDim("lev");
	writeDim("time");
	writeDim("ens");
	if (gradsInfo.isDirectSubset()) {
	    saveDimsForDirectSubset();
	}
    }

    protected void load()
	throws AnagramException {

	if (debug()) log.debug(this, "loading " + 
			       gradsInfo.getDescriptorFile());
	
	String name = data.getName();
	title = gradsInfo.getTitle();

	BufferedReader r;
	try {
	    r = new BufferedReader
		(new FileReader
		    (gradsInfo.getDescriptorFile()));
	} catch (FileNotFoundException fnfe) {
	    throw new AnagramException("extraction failed for " + 
				       data.getName(), 
				       fnfe);
	}

	if (debug()) log.debug(this, "parsing " + gradsInfo.getDescriptorFile());

	
	// parsing code for CTL files
	this.variableList = new ArrayList();
	this.levelCountList = new ArrayList();
	this.descriptionList = new ArrayList();

	boolean bigEndian = false;	
	if (gradsInfo.isDirectSubset()) {
	    this.unsortedVariableList = new ArrayList();
	    this.unsortedLevelCountList = new ArrayList();
	}

	// begin parsing
	try {
	    boolean inVarSection = false;
	    boolean inEnsSection = false;
	    String line;
	    String original;
	    
	    while ((original = r.readLine()) != null) {
		
		original = original.trim();
		line = original.toLowerCase();
		
		// tokenize line
		StringTokenizer st = new StringTokenizer(line, " ");
		
		try {
		    // ignore attribute metadata and comments 
		    if (line.startsWith("@") || line.startsWith("*")) 
			continue;
		    if (gradsInfo.isDirectSubset() &&
			(line.startsWith("dtype") 
			 || line.startsWith("fileheader")
			 || line.startsWith("xyheader")
			 || line.startsWith("theader"))) {
			useDirect = false;
			throw new AnagramException
			    ("direct subsetting cannot be enabled if " +
			     "DTYPE, FILEHEADER, XYHEADER, or THEADER " + 
			     "are present");
		    }				
			
		    if (inEnsSection) {
			if (line.startsWith("endedef")) {
			    inEnsSection = false; 
			    continue; // done skipping ensemble definitions
			} 
		    }
		    if (inVarSection) {
			if (line.startsWith("endvars")) {
			    inVarSection = false; 
			    continue; // done parsing variables
			} 
			
			// parse a variable
			String variable = st.nextToken();

			// handle new "LongVarName=>gradsname" syntax
			// used in dtype netcdf
			int arrowIndex = variable.indexOf("=>");
			if (arrowIndex > 0) {
			    variable = variable.substring(arrowIndex + 2);
			}

			// levels field may have comma-delimited numbers
			String levelsField = st.nextToken();
			int commaIndex = levelsField.indexOf(",");
			if (commaIndex > 0) {
			    String levelsNum = levelsField.substring(0,commaIndex);
			    levelsField = levelsNum;
			}
			Long levelCount = Long.valueOf(levelsField);

			// ignore units info except for direct subset
			String units = st.nextToken(); 
			if (gradsInfo.isDirectSubset()
			    && !units.equals("99")) {
			    useDirect = false;
			    throw new AnagramException
				("units set to " + units + " for " + 
				 variable + "; must be '99' to enable " + 
				 " direct subsetting");
			}
			    

			// parse remainder of line as description
			// note - case of description is lost. 
			String description = "";
			while (st.hasMoreTokens()) {
			    description += st.nextToken() + " ";
			}
			// if this is the first variable with multiple levels
			// put it first in the list. 
			// the GrADS client needs to have a multi-level
			// variable be the first one if there is a mixture
			if (levelCount.longValue() > 0 && 
			    !this.gotLevels) {
			    this.variableList.add(0, variable);
			    this.levelCountList.add(0, levelCount);
			    this.descriptionList.add(0, description);
			    this.gotLevels = true;
			} else {
			    this.variableList.add(variable);
			    this.levelCountList.add(levelCount);
			    this.descriptionList.add(description);
			}
		
			if (gradsInfo.isDirectSubset()) {
			    useDirect = false;
			    this.unsortedVariableList.add(variable);
			    this.unsortedLevelCountList.add(levelCount);
			}
	
		    } else { 
			// not in var section or edef section, look for general metadata
			String label = st.nextToken();

			if (gradsInfo.isDirectSubset() && 
			    label.equals("options")) {
			    while (st.hasMoreTokens()) {
				if (! st.nextToken().equals("big_endian")) {
				    useDirect = false;
				    throw new AnagramException
					("only 'big_endian' is allowed on "
					 + "OPTIONS line when direct " +
					 "subsetting is enabled");
				} else {
				    bigEndian = true;
				}
			    }
			}
			
			if (label.equals("undef")) {
			    this.missingData = 
				Double.valueOf(st.nextToken()).doubleValue();
			    
			} else if (label.equals("xdef")) {
			    this.xSize = 
				Integer.valueOf(st.nextToken()).intValue();
			    this.xMapping = st.nextToken();
			    dimValues.put("lon", loadDimValues("lon", xSize));
			    
			} else if (label.equals("ydef")) {
			    this.ySize = 
				Integer.valueOf(st.nextToken()).intValue();
			    this.yMapping = st.nextToken();
			    dimValues.put("lat", loadDimValues("lat", ySize));
			    
			} else if (label.equals("zdef")) {
			    this.zSize = 
				Integer.valueOf(st.nextToken()).intValue();
			    this.zMapping = st.nextToken();
			    dimValues.put("lev", loadDimValues("lev", zSize));
			    
			} else if (label.equals("tdef")) {
			    this.tSize = 
				Integer.valueOf(st.nextToken()).intValue();
			    // we can read the following directly
			    // since tdef never uses "levels"
			    this.tMapping = st.nextToken();
			    st.nextToken(); // start time - redundant
					    // due to loadDimValues() 
			    tStep = st.nextToken();
			    dimValues.put("time", loadDimValues("time", tSize));
			    
			} else if (label.equals("edef")) {
			    this.eSize = 
				Integer.valueOf(st.nextToken()).intValue();
			    dimValues.put("ens", loadDimValues("ens", eSize));
			    minValues.put("ens", "1");
			    maxValues.put("ens", String.valueOf(eSize));
			    gotEnsemble = true;
			    // Check if EDEF entry is the short or extended version 
			    if (st.nextToken().equals("names")) 
				inEnsSection = false;
			    else 
				inEnsSection = true;
			} else if (label.equals("vars")) {
			    inVarSection = true;
			}
		    } 
		} catch (NoSuchElementException nsee) {
		} 
		
	    } // end parsing loop
	    
	    // add default ensemble axis info 
	    if (!gotEnsemble) {
		this.eSize = 1;
		dimValues.put("ens", loadDimValues("ens", eSize));
		minValues.put("ens", "1");
		maxValues.put("ens", String.valueOf(eSize));
	    }


	    if (gradsInfo.isDirectSubset() && !bigEndian) {
		useDirect = false;
		throw new AnagramException
		    ("direct subsetting can only be enabled for " +
		     "big-endian data; 'OPTIONS big_endian' not found " +
		     "in CTL file");
	    }
	    
	    
	} catch (IOException ioe) {
	    throw new AnagramException("error parsing metadata for " + name);
	} finally {
	    try {
		r.close();
	    } catch (IOException ioe) {}
	}
    }
    

    
    /** Creates an in-memory object for use by the direct-subsetting 
     *  feature, which reads directly from IEEE binary datafiles instead
     *  of invoking GrADS. To make this possible it is necessary to 
     *  save the dimension sizes, and an ordered list of variable names
     *  with the number of vertical levels for each variable, so that
     *  byte offsets can be calculated properly. 
     */
    protected void saveDimsForDirectSubset() {
	if (debug()) log.debug(this, "putting dims in GradsDataInfo: \n" +
			       "\tx=" + xSize + ", y=" + ySize +
			       ", z=" + zSize + ", t=" + tSize +
			       ", e=" + eSize + "\n" +
			       "\tvars=" + unsortedVariableList + "\n" +
			       "\tlevs=" + unsortedLevelCountList);
	gradsInfo.setCTL(xSize, ySize, zSize, tSize, eSize, 
			 unsortedVariableList,
			 unsortedLevelCountList);
	if (eSize > 1) gradsInfo.setEnsemble(true);
	if (zSize > 1) gradsInfo.setLevels(true);
	server.getCatalog().saveCatalogToStore();
    }


    protected void loadServerMetadata() throws AnagramException {
	serverMetadata = new ArrayList();

	// global
	smAtt("", "String", "title", title);
	smAtt("", "String", "Conventions", "COARDS");
	smAtt("", "String", "dataType", "Grid");
	if (gradsInfo.getDocURL() != null) {
	    smAtt("", "String", "documentation", gradsInfo.getDocURL());
	}
	
	// longitude
	smAtt("lon", "String", "grads_dim", "x");
	smAtt("lon", "String", "grads_mapping", xMapping);
	smAtt("lon", "String", "grads_size", String.valueOf(xSize));
	smAtt("lon", "String", "units", "degrees_east");
	smAtt("lon", "String", "long_name", "longitude");
	smAtt("lon", "Float64", "minimum", minValues.get("lon").toString());
	smAtt("lon", "Float64", "maximum", maxValues.get("lon").toString());
	if (xSize > 1) {
	    smAtt("lon", "Float32", "resolution", 
		  resValues.get("lon").toString());
	}
	
	// latitude
	smAtt("lat", "String", "grads_dim", "y");
	smAtt("lat", "String", "grads_mapping", yMapping);
	smAtt("lat", "String", "grads_size", String.valueOf(ySize));
	smAtt("lat", "String", "units", "degrees_north");
	smAtt("lat", "String", "long_name", "latitude");
	smAtt("lat", "Float64", "minimum", minValues.get("lat").toString());
	smAtt("lat", "Float64", "maximum", maxValues.get("lat").toString());
 	if (ySize > 1) {
	    smAtt("lat", "Float32", "resolution", 
		  resValues.get("lat").toString());
	}

	// time
	smAtt("time", "String", "grads_dim", "t");
	smAtt("time", "String", "grads_mapping", tMapping);
	smAtt("time", "String", "grads_size", String.valueOf(tSize));
	smAtt("time", "String", "grads_min", 
	      minValues.get("time").toString());
	smAtt("time", "String", "grads_step", 
	      String.valueOf(this.tStep));
	smAtt("time", "String", "units", "days since 1970-1-1 00:00:0.0");
	smAtt("time", "String", "long_name", "time");
	smAtt("time", "String", "minimum", minValues.get("time").toString());
	smAtt("time", "String", "maximum", maxValues.get("time").toString());
 	if (tSize > 1) {
	    smAtt("time", "Float32", "resolution", 
		resValues.get("time").toString());
	}

	// altitude
	if (this.gotLevels) {
	    smAtt("lev", "String", "grads_dim", "z");
	    smAtt("lev", "String", "grads_mapping", zMapping);
	    smAtt("lev", "String", "units", "millibar");
	    smAtt("lev", "String", "long_name", "altitude");
	    smAtt("lev", "Float64", "minimum", minValues.get("lev").toString());
	    smAtt("lev", "Float64", "maximum", maxValues.get("lev").toString());
	    if (zSize > 1) {
		smAtt("lev", "Float32", "resolution", 
		    resValues.get("lev").toString());
	    }
	}
	
	//ensemble
	if (this.gotEnsemble) {
	    smAtt("ens", "String", "grads_dim", "e");
	    smAtt("ens", "String", "grads_size", String.valueOf(eSize));
	    smAtt("ens", "String", "grads_mapping", eMapping);
	    smAtt("ens", "String", "units", "count");
	    smAtt("ens", "String", "long_name", "ensemble member");
	    // ensemble metadata containing lists of
	    // names, lengths, and start times are added
	    // at the same time as descriptor and native attributes
	}


	// write metadata for each variable
	for (int i = 0; i < this.variableList.size(); i++) {
	    String name = (String)this.variableList.get(i);
	    String longName = 
		Strings.escape((String)this.descriptionList.get(i), '\"');
	    smAtt(name, "Float32", "_FillValue",
		  String.valueOf(this.missingData));
	    smAtt(name, "Float32", "missing_value",
		  String.valueOf(this.missingData));
	    smAtt(name, "String", "long_name", longName);
	}

    }

    
    
    public void writeDDS() 
	throws AnagramException {
	
	String ddsStorage = storagePrefix + ".dds";
	if (debug()) log.debug(this, "writing dds to " + ddsStorage);

	FileWriter output;
	try {
	    output = new FileWriter(ddsStorage);
	} catch (IOException ioe) {
	    throw new AnagramException("error writing dds for " + 
				       data.getName());
	}
	
	PrintWriter dds = new PrintWriter(output);
	
	dds.println("Dataset {");
	// write metadata for each variable
	for (int i = 0; i < this.variableList.size(); i++) { 
	    boolean varHasLevels = (((Long)this.levelCountList.get(i)).longValue() > 0);
	    dds.print("  Grid {\n" + 
		      "    ARRAY:\n" + 
		      "      Float32 " + this.variableList.get(i));
	    if (gotEnsemble) {
		dds.print("[ens = " + this.eSize + "]");
	    }
	    dds.print("[time = "+ this.tSize);
	    if (varHasLevels) {
		dds.print("][lev = " + this.zSize);
	    }
	    dds.println("][lat = " + this.ySize + 
			"][lon = " + this.xSize + "];\n" + 
			"    MAPS:");
	    if (gotEnsemble) {
		dds.println("      Float64 ens[ens = " + this.eSize + "];");
	    }
	    dds.println("      Float64 time[time = " + this.tSize + "];");
	    if (varHasLevels) {
		dds.println("      Float64 lev[lev = " + this.zSize + "];");
	    }
	    dds.println("      Float64 lat[lat = " + this.ySize + "];\n" + 
			"      Float64 lon[lon = " + this.xSize + "];\n" + 
			"  } " + this.variableList.get(i) + ";");
	}
	
	
	// write global dimension information. needed for netCDF client. 
	if (this.gotEnsemble) {
	    dds.println("  Float64 ens[ens = " + this.eSize + "];");
	}
	dds.println("  Float64 time[time = " + this.tSize + "];");
	if (this.gotLevels) {
	    dds.println("  Float64 lev[lev = " + this.zSize + "];");
	}
	dds.println("  Float64 lat[lat = " + this.ySize + "];\n" + 
		    "  Float64 lon[lon = " + this.xSize + "];"); 
	
	dds.println("} " + gradsInfo.getDODSName() + ";");
	dds.close();
	
    }

    public void writeWebSummary()
	throws AnagramException {

	String infoStorage = storagePrefix + ".info";
	if (debug()) log.debug(this, "writing web info to " + infoStorage);

	FileWriter output;
	try {
	    output = new FileWriter(infoStorage);
	} catch (IOException ioe) {
	    throw new AnagramException("error writing web info for " + 
				       data.getName());
	}

	PrintWriter info = new PrintWriter(output);

	info.print("<table>\n");
	info.print("   <tbody>\n");
	info.print("     <tr>\n");
	info.print("       <td valign=\"Bottom\" colspan=\"2\">\n");
	info.print("<b>Description:</b><br>\n");
	info.print("       </td>\n");
	info.print("       <td valign=\"Bottom\" colspan=\"2\">\n");
	info.print(data.getDescription());
	info.print("<br>\n");
	info.print("       </td>\n");
	info.print("     </tr>\n");
	info.print("     <tr>\n");
	info.print("       <td valign=\"Bottom\" colspan=\"2\">\n");
	info.print("<b>Documentation:</b>\n");
	info.print("       </td>\n");
	info.print("       <td valign=\"Bottom\" colspan=\"2\">");
	if (gradsInfo.getDocURL() != null) {
	    info.print("<a href=\"\n");
	    info.print(gradsInfo.getDocURL());
	    info.print("\">\n");
	    info.print(gradsInfo.getDocURL());
	} else {
	    info.print("(none provided)");
	}
	info.print("</a>\n");
	info.print("       <br>\n");
	info.print("       </td>\n");
	info.print("     </tr>\n");

	printDim(info, "lon", "Longitude", "&deg;E", "&deg;", xSize);
	printDim(info, "lat", "Latitude", "&deg;N", "&deg;", ySize);
	if (this.gotLevels) {
	    printDim(info, "lev", "Altitude", "", "", zSize);
	}
	printDim(info, "time", "Time", "", " days", tSize);
	if (this.gotEnsemble) {
	    //	    printDim(info, "ens", "Ensemble", "members", null, eSize);
	    printDim(info, "ens", "Ensemble", "", null, eSize);
	}

	info.print("     <tr>\n");
	info.print("       <td valign=\"Bottom\" colspan=\"2\"><b>Variables:</b><br>\n");
	info.print("       </td>\n");
	info.print("       <td colspan=\"4\"><b>(total of ");
	info.print(variableList.size());
	info.print(")</b><br>\n");
	info.print("       </td>\n");
	info.print("     </tr>\n");


	Iterator varIt = variableList.iterator();
	Iterator descIt = descriptionList.iterator();
	while (varIt.hasNext()) {
	    info.print("     <tr>\n");
	    info.print("       <td valign=\"Bottom\">&nbsp;<br>\n");
	    info.print("       </td>\n");
	    info.print("       <td><b>");
	    info.print(varIt.next());
	    info.print("</b><br>\n");
	    info.print("       </td>\n");
	    info.print("       <td colspan=\"4\">");
	    info.print(descIt.next());
	    info.print("<br>\n");
	    info.print("       </td>\n");
	    info.print("     </tr>\n");
	}

	info.print("   \n");
	info.print("  </tbody> \n");
	info.print("</table>\n");


	info.close();
	
    }


    /** size of grid dimension for this dataset */
    int xSize;
    /** size of grid dimension for this dataset */
    int ySize;
    /** size of grid dimension for this dataset */
    int zSize;
    /** size of ensemble dimension for this dataset */
    int eSize;

    String xMapping;
    String yMapping;
    String zMapping;
    String eMapping;

    boolean gotLevels;
    boolean gotEnsemble;
    boolean useDirect;
    
    ArrayList variableList;
    ArrayList levelCountList;
    ArrayList descriptionList;

    ArrayList unsortedVariableList;
    ArrayList unsortedLevelCountList;


    /** Takes the parsed metadata and writes an XML fragment
     * for the THREDDS catalog  */
    /* (incomplete code 
    public void writeTHREDDSTag()
	throws AnagramException {

	String threddsStorage = storagePrefix + ".thredds";
	if (debug()) log.debug(this, "writing thredds tag to " + 
			       threddsStorage);
	FileWriter output;
	try {
	    output = new FileWriter(threddsStorage);
	} catch (IOException ioe) {
	    throw new AnagramException("error writing thredds tag for " + 
				       data.getName());
	}

	PrintWriter thredds = new PrintWriter(output);

	thredds.print("<dataset name=\"" + data.getDescription() + "\"\n");
	thredds.print("         urlPath=\"" + data.getCompleteName() + "\"\n");
	thredds.print("         dataType=\"Grid\" serviceName=\"" + 
		      server.getModuleName() + "\">\n");
	thredds.print("  <geospatialCoverage>\n");
	thredds.print("    <northlimit>" +  this.maxValues.get("lat") 
		      + "</northlimit>\n");
	thredds.print("    <southlimit>" +  this.minValues.get("lat") 
		      + "</southlimit>\n");
	thredds.print("    <eastlimit>" +  this.maxValues.get("lon") 
		      + "</eastlimit>\n");
	thredds.print("    <westlimit>" +  this.minValues.get("lon") 
		      + "</westlimit>\n");
	thredds.print("  </geospatialCoverage>\n");
	thredds.print("  <timeCoverage>\n");
	thredds.print("    <start>" +  
		      Range.parseGradsFormat((String)this.minValues.get("time")) + 
		      "</start>\n");
	thredds.print("    <end>" +  
		      Range.parseGradsFormat((String)this.maxValues.get("time")) + 
		      "</end>\n");
	thredds.print("  </timeCoverage>\n");
	thredds.print("</dataset>\nb");

	thredds.close();
    }
    */
    

}

