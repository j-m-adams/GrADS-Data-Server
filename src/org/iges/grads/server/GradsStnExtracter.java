/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;
import java.util.*;
import org.iges.util.*;
import org.iges.anagram.*;

/** Extracts and caches metadata for GrADS station datasets.
 */
public class GradsStnExtracter 
    extends GradsExtracter{

    public void writeSubsetInfo() throws AnagramException {
       	writeDim("time");

    }


    /** Reads CTL file for dataset into internal structures */
    protected void load() 
	throws AnagramException {
	
	GradsDataInfo gradsInfo = (GradsDataInfo)data.getToolInfo();

	if (debug()) log.debug(this, "loading " + 
			       gradsInfo.getDescriptorFile());

	String name = data.getName();
	title = gradsInfo.getTitle();
	
	this.dependentVars = new TreeMap();
	this.independentVars = new TreeMap();


	// open descriptor file
	
	BufferedReader r = null;
	try {
	    r = new BufferedReader
		(new FileReader
		    (gradsInfo.getDescriptorFile()));

	    if (debug()) log.debug(this, "parsing " + 
				   gradsInfo.getDescriptorFile());

	    boolean inVarSection = false;
	    String line;
	    String original;
	   
	    // read descriptor file records

	    while ((original = r.readLine()) != null) {
		
		original = original.trim();
		line = original.toLowerCase();
		
		// tokenize line
		StringTokenizer st = new StringTokenizer(line, " ");
		
		try {
		    if (inVarSection) {
			if (line.startsWith("endvars")) {
			    break; // done parsing
			} 
			
			// parse a variable
			String variable = st.nextToken();

			// handle new "LongVarName=>gradsname" syntax
			// might be used in dtype bufr
			int arrowIndex = variable.indexOf("=>");
			if (arrowIndex > 0) {
			    variable = variable.substring(arrowIndex + 2);
			}

			Long levelCount = Long.valueOf(st.nextToken());
			st.nextToken(); // skip useless info
			// parse remainder of line as description
			// note - case of description is lost. 
			String description = "";
			while (st.hasMoreTokens()) {
			    description += st.nextToken() + " ";
			}
			if (levelCount.longValue() > 0) {
			    this.dependentVars.put(variable, description);
			} else {
			    this.independentVars.put(variable, description);
			}
			
		    } else { // not in var section
			// look for general metadata
			String label = st.nextToken();

			if (label.equals("undef")) {
			    this.missingData = Double.valueOf
				(st.nextToken()).doubleValue();
			    
			} else if (label.equals("tdef")) {
			    this.tSize = Integer.valueOf
				(st.nextToken()).intValue();
			    // we can read the following directly
			    // since tdef never uses "levels"
			    tMapping = st.nextToken(); // linear
			    st.nextToken(); // start time - redundant
					    // due to loadDimValues() 
			    tStep = st.nextToken();
			    dimValues.put("time", loadDimValues("time", tSize));

			    			    
			} else if (label.equals("vars")) {
			    inVarSection = true;
			}
		    } 
		} catch (NoSuchElementException nsee) {
		} 
		
	    } // end parsing loop	    	    
	    
	} catch (IOException ioe) {
	    throw new AnagramException("error parsing metadata for " + name);
	} finally {
	    try {
		r.close();
	    } catch (Exception e) {}
	}
    }
    
    protected void loadServerMetadata() throws AnagramException {
	serverMetadata = new ArrayList();

	// global
	smAtt("", "String", "title", Strings.escape(title, '\"'));
	smAtt("", "String", "dataType", "Station");
	if (gradsInfo.getDocURL() != null) {
	    smAtt("", "String", "documentation", gradsInfo.getDocURL());
	}
	
	// longitude
	smAtt("lon", "String", "grads_dim", "x");
	smAtt("lon", "String", "units", "degrees_east");
	smAtt("lon", "String", "long_name", "longitude");
	
	// latitude
	smAtt("lat", "String", "grads_dim", "y");
	smAtt("lat", "String", "units", "degrees_north");
	smAtt("lat", "String", "long_name", "latitude");

	// time
	smAtt("time", "String", "grads_dim", "t");
	smAtt("time", "String", "grads_size", 
	      String.valueOf(this.tSize));
	smAtt("time", "String", "grads_min", 
	      minValues.get("time").toString());
	smAtt("time", "String", "grads_step", 
	      String.valueOf(this.tStep));
	smAtt("time", "String", "units", "days since 1-1-1 00:00:0.0");
	smAtt("time", "String", "long_name", "time");
	smAtt("time", "String", "minimum", minValues.get("time").toString());
	smAtt("time", "String", "maximum", maxValues.get("time").toString());
 	if (tSize > 1) {
	    smAtt("time", "Float32", "resolution", 
		resValues.get("time").toString());
	}

	// write metadata for each variable
	Iterator it = this.independentVars.keySet().iterator();
	while (it.hasNext()) {
	    String name = (String)it.next();
	    String longName = 
		Strings.escape((String)this.independentVars.get(name), '\"');
	    smAtt(name, "Float32", "_FillValue",
		  String.valueOf(this.missingData));
	    smAtt(name, "Float32", "missing_value",
		  String.valueOf(this.missingData));
	    smAtt(name, "String", "long_name", longName);
	}

	// altitude
	if (this.dependentVars.keySet().size() > 0) {
	    smAtt("lev", "String", "grads_dim", "z");
	    smAtt("lev", "String", "units", "millibar");
	    smAtt("lev", "String", "long_name", "altitude");

	    it = this.dependentVars.keySet().iterator();
	    while (it.hasNext()) {
		String name = (String)it.next();
		String longName = 
		    Strings.escape((String)this.dependentVars.get(name), '\"');
		smAtt(name, "Float32", "_FillValue",
		      String.valueOf(this.missingData));
		smAtt(name, "Float32", "missing_value",
		      String.valueOf(this.missingData));
		smAtt(name, "String", "long_name", longName);
	    }

	}
	

    }




    /** Takes the parsed metadata and writes a DDS  */
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
	dds.print("  Sequence {\n" + 
		  "    String stid;\n" +
		  "    Float32 lon;\n" +
		  "    Float32 lat;\n" +
		  "    Float64 time;\n");
	Iterator it = this.independentVars.keySet().iterator();
	while (it.hasNext()) {
	    String name = (String)it.next();
	    dds.print("    Float32 " + name + ";\n");
	}
	
	if (this.dependentVars.keySet().size() > 0) {
	    dds.print("    Sequence {\n" +
		      "      Float32 lev;\n");
	    it = this.dependentVars.keySet().iterator();
	    while (it.hasNext()) {
		String name = (String)it.next();
		dds.print("      Float32 " + name + ";\n");
	    }
	    dds.print("    } levels;\n");
	}
	dds.print("  } reports;\n" + "} " + 
		  ((GradsDataInfo)(data.getToolInfo())).getDODSName() + ";\n");
	dds.close();
	
    }



    /** Takes the parsed metadata and writes a summary HTML fragment
     * for the ".info" page  */
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
	info.print("  <tbody>\n");
	info.print("    <tr>\n");
	info.print("      <td valign=\"Bottom\" colspan=\"2\"><b>Description:</b><br>\n");
	info.print("      </td>\n");
	info.print("      <td valign=\"Bottom\" colspan=\"2\">");
	info.print(data.getDescription());
	//	info.print("   <br>\n");
	info.print("      </td>\n");
	info.print("    </tr>\n");
	info.print("    <tr>\n");
	info.print("      <td valign=\"Bottom\" colspan=\"2\"><b>Type of dataset:</b><br>\n");
	info.print("      </td>\n");
	info.print("      <td valign=\"Bottom\" colspan=\"2\">");
	info.print("      station data\n");
	info.print("      </td>\n");
	info.print("    </tr>\n");
	printDim(info, "time", "Time", "", " days", tSize);
	    info.print("    <tr>\n");
	    info.print("      <td valign=\"Bottom\" colspan=\"2\"><b>Documentation:</b></td><td valign=\"Bottom\" colspan=\"2\">");
	if (gradsInfo.getDocURL() == null) {
	    info.print("(none provided)");
	} else {
	    info.print("<a href=\"");
	    info.print(gradsInfo.getDocURL());
	    info.print("\">\n");
	    info.print(gradsInfo.getDocURL());
	    info.print("      </a>\n");
	}
	    info.print("      </td>\n");
	    info.print("    </tr>\n");
	info.print("</tbody></table>");

	info.close();
    }
 
    Map independentVars;
    Map dependentVars;


    /** Takes the parsed metadata and writes an XML fragment
     * for the THREDDS catalog  */
    /* (incomplete code)
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
	thredds.print("         dataType=\"Station\" serviceName=\"" + 
		      server.getModuleName() + "\">\n");
	thredds.print("  <timeCoverage>\n");
	thredds.print("    <start>" +  
		      Range.parseGradsFormat(this.minTime) + 
		      "    </start>\n");
	thredds.print("    <end>" +  
		      Range.parseGradsFormat(this.maxTime) + 
		      "    </end>\n");
	thredds.print("  </timeCoverage>\n");
	thredds.print("</dataset>\n");

	thredds.close();
    }
    */

   
}

