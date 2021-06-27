/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.service;

import java.util.*;
import java.io.*;
import javax.servlet.http.*;

import dods.dap.*;
import dods.dap.Server.*;

import org.iges.util.Spooler;

import org.iges.anagram.*;

/** Provides an HTML summary of a data object 
 */
public class InfoService 
    extends Service {

    public String getServiceName() {
	return "info";
    }

    public void configure(Setting setting) {
    }

    public void handle(ClientRequest clientRequest)
	throws ModuleException {

	HttpServletRequest request = clientRequest.getHttpRequest();
	HttpServletResponse response = clientRequest.getHttpResponse();
	
	DataHandle data = getDataFromPath(clientRequest);
	
	response.setContentType("text/html");
	response.setHeader("XDODS-Server", "dods/3.2");
	response.setHeader("XDAP", "3.2");
	response.setHeader("Content-Description", "dods_info");
	response.setDateHeader("Last-Modified", data.getCreateTime());
	
	File cache = server.getStore().get(this, 
					   data.getCompleteName(), 
					   data.getCreateTime());
	synchronized (data) {
	    if (!cache.exists()) {
		writeToCache(clientRequest, data, cache);
	    }
	}

	try {
	    InputStream in = 
		new BufferedInputStream
		    (new FileInputStream
		    (cache));
	    Spooler.spool(in, response.getOutputStream());
	    in.close();
	} catch (IOException ioe) {}

    }

    protected void writeToCache(ClientRequest clientRequest, 
				DataHandle data, 
				File cacheFile) 
    throws ModuleException {

	ServerDDS    myDDS = null;
	DAS          myDAS = null;
        
	// Get the DDS and the DAS (if one exists) for the dataSet.
	myDDS= server.getTool().getDDS(data, "");
	myDAS = server.getTool().getDAS(data);
	
	// Build the HTML* documents.
	
	String global_attrs = buildGlobalAttributes(myDAS, myDDS);
	
	String variable_sum = buildVariableSummaries(myDAS, myDDS);
	
	PrintStream pw = null;
	try {
	    pw = 
		new PrintStream
		    (new FileOutputStream(cacheFile));
	} catch (IOException ioe) { 
	    throw new ModuleException(this, "saving web info for " + data + 
				      " failed");
	}
	String baseURL = getBaseURL(clientRequest);
	
	String windowTitle = "info for " + data.getCompleteName();
	String pageTitle = windowTitle + " : " + 
	    "<a href=\"" + baseURL + data.getCompleteName() + 
	    ".dds\">dds</a>&nbsp;&nbsp;<a href=\"" + 
	    baseURL + data.getCompleteName() + ".das\">das</a>";

	printHeader(pw, windowTitle, pageTitle, data, baseURL);

	
	pw.print("<table><tbody><tr>\n");
	pw.print("<td valign=\"bottom\">");
	pw.print("<b>OPeNDAP/DODS Data URL:</b>\n");
	pw.print("</td>");
	pw.print("<td valign=\"bottom\">");
	pw.print(baseURL + data.getCompleteName());
	pw.print("</td></tr></tbody></table></p>\n");
	
	pw.flush();

	server.getTool().writeWebInfo(data, pw);

	pw.print("<br><a href=\""); 
	pw.print(baseURL);
	pw.print(server.getCatalog().getParent(data).getCompleteName());
	pw.print("\">back to parent directory</a><br><br>");
	
	pw.print("<hr><h2>complete metadata listing:</h2>\n");
	pw.print("<b>Global attributes:</b><p>");
	pw.print(global_attrs);
	pw.print("<hr>\n");
	
	pw.println(variable_sum);
	
	pw.print("<a href=\"#\">back to top</a><br>\n");
	
	printFooter(pw, null, data.getCreateTime(), baseURL);

	// Flush the output buffer.	    
	pw.flush();
	pw.close();
    }
    
    
    private String buildGlobalAttributes(DAS das, ServerDDS dds){
    
        boolean found = false;

	StringBuffer ga =  new StringBuffer("<table>\n");
	//	    ("<h3>Dataset Information</h3>\n<table>\n");

	// Run through each component of the DAS
	String name;
        Enumeration edas = das.getNames();
	while(edas.hasMoreElements()){
	    name = (String)edas.nextElement();
	    if((nameIsGlobal(name) || !nameInDDS(name, dds))){
	        // If this is not associated with a variable, 
		// run through each element and print it:
                AttributeTable attr = das.getAttributeTable(name);
		Enumeration e = attr.getNames();
		while(e.hasMoreElements()){
		    String aName = (String)e.nextElement();
		    Attribute a = attr.getAttribute(aName);
		    
		    found = true;
		    
		    ga.append("\n<tr><td align=right valign=top><b>");
		    ga.append(aName);
		    ga.append("</b>:</td>\n");
		    ga.append("<td align=left>");
		    
		    Enumeration es = a.getValues();
		    while(es.hasMoreElements()){
			String val = (String)es.nextElement();
			ga.append(val);
			ga.append("<br>");
		    }
		    ga.append("</td></tr>\n");
		    
		}
	    }
	}
	ga.append("</table>\n<p>\n");

        if(!found) {
	    return "";
	} else {
	    return ga.toString();
	}
    }	
    


    private String buildVariableSummaries(DAS das, ServerDDS dds){
        StringBuffer vs = new StringBuffer
	    ("<b>Variables</b><p>\n<table>\n");
        Enumeration e = dds.getVariables();
        while( e.hasMoreElements() ) {
	    BaseType bt     = (BaseType)e.nextElement();
	    vs.append("<tr>");	
	    vs.append(summarizeVariable(bt, das));
	    vs.append("</tr>");	
        }
        vs.append("</table>\n<p>\n");
        return vs.toString();
    }
    
    
    
    private String summarizeVariable(BaseType bt, DAS das){
    
        StringBuffer vOut = new StringBuffer
	    ("<td align=right valign=top><b>" + bt.getName());
        vOut.append("</b>:</td>\n");
        vOut.append("<td align=left valign=top>" + fancyTypeName(bt));
        vOut.append("<br>\n");    
        AttributeTable attr = das.getAttributeTable(bt.getName());
        if(attr != null){
	    Enumeration e = attr.getNames();
	    while(e.hasMoreElements()){
	        String name = (String)e.nextElement();
		Attribute a = attr.getAttribute(name);
	        vOut.append(name + ": ");
                Enumeration es = a.getValues();
		while(es.hasMoreElements()){
		    String val = (String)es.nextElement();
		    vOut.append(val);
		    if(es.hasMoreElements())
		        vOut.append(", ");
		}
		vOut.append("\n<br>\n");
	
	    }
	}



	if(bt instanceof DConstructor){
	    vOut.append("<table>\n");
	    DConstructor dc = (DConstructor)bt;
	    Enumeration e = dc.getVariables();
	    while(e.hasMoreElements()){
	        BaseType bt2  = (BaseType)e.nextElement();
		vOut.append("<tr>\n");
		vOut.append(summarizeVariable(bt2,das));
		vOut.append("</tr>\n");
	    }
	    vOut.append("</table>\n");
	
	
	} else if(bt instanceof DVector){
	    DVector da = (DVector)bt;	    
	    PrimitiveVector pv = da.getPrimitiveVector();
	    
	    if(pv instanceof BaseTypePrimitiveVector){
	        BaseType bt2 = pv.getTemplate();
		
		if(bt2 instanceof DArray || bt2 instanceof DString){
		} else {
	            vOut.append("<table>\n");
	            vOut.append("<tr>\n");
	            vOut.append(summarizeVariable(bt2,das));
	            vOut.append("</tr>\n");
	            vOut.append("</table>\n");
		}
	    }
	
	
	}
	return vOut.toString();
    
    }


    public static boolean nameInDDS(String name, DDS dds) {
        try { 
	    dds.getVariable(name);
	} catch (NoSuchVariableException e) {
	    return false;
	}
        return true;
    }




    public static boolean nameIsGlobal(String name) {
    
        String lcName = name.toLowerCase();
	if(lcName.indexOf("global") >= 0) {
	    return true;
	} else if(lcName.indexOf("dods") >= 0) {
	    return true;
	} else {
	    return false;
	}
	
    }


    public static String fancyTypeName(BaseType bt){
    
        String fancy;
	
	if(bt instanceof DByte)
	    return("8 bit Byte");

	if(bt instanceof DUInt16)
	    return("16 bit Unsigned Integer");
		
	if(bt instanceof DInt16)
	    return("16 bit Integer");
		
	if(bt instanceof DUInt32)
	    return("32 bit Unsigned Integer");
	
	if(bt instanceof DInt32)
	    return("32 bit Integer");
		
	if(bt instanceof DFloat32)
	    return("32 bit Real");
		
	if(bt instanceof DFloat64)
	    return("64 bit Real");
		
	if(bt instanceof DURL)
	    return("URL");
	    
	if(bt instanceof DString)
	    return("String");
		
		
	if(bt instanceof DArray){
	
	    DArray a = (DArray) bt;
	    String type = "Array of " + 
	                  fancyTypeName(a.getPrimitiveVector().getTemplate()) + 
			  "s ";
            
	    Enumeration e = a.getDimensions();
	    while(e.hasMoreElements()){
	        DArrayDimension dad = (DArrayDimension)e.nextElement();
	
	        type += "[" + dad.getName() + " = 0.." + (dad.getSize()-1) +"]";
	
	    }
	    type += "\n";
	    return(type);
	}

	if(bt instanceof DList){
	    DList a = (DList) bt;
	    String type = "List of " + 
	                  fancyTypeName(a.getPrimitiveVector().getTemplate()) + 
			  "s\n";
            
	   return(type);
	}
	
	if(bt instanceof DStructure)
	    return("Structure");
		
	if(bt instanceof DSequence)
	    return("Sequence");
		
	if(bt instanceof DGrid)
	    return("Grid");

	return("UNKNOWN");
  

    }
	
}
