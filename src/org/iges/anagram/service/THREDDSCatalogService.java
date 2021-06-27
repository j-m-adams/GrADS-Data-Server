/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.service;

import java.io.*;
import java.util.*;
import java.text.*;
import javax.servlet.http.*;

import org.iges.util.*;
import org.iges.anagram.*;

/** Sends a complete listing of the server's contents in THREDDS XML format.
 */
public class THREDDSCatalogService
    extends Service {

    public String getServiceName() {
	return "thredds";
    }

    public void configure(Setting setting) {
    }

    public void handle(ClientRequest clientRequest)
	throws ModuleException {
	
	HttpServletRequest request = clientRequest.getHttpRequest();
	HttpServletResponse response = clientRequest.getHttpResponse();


	Hashtable queryParams = getQueryParams(clientRequest);
	String[] clientVersion = (String[])queryParams.get("version");

	String[] recurseString = (String[])queryParams.get("recurse");
	boolean recurse = (recurseString != null 
			   &&  recurseString[0].equals("true"));

	Handle handle = clientRequest.getHandle();
	if (handle == null || 
	    !clientRequest.getPrivilege().allows(handle.getCompleteName())) {
	    throw new ModuleException(this, clientRequest.getDataPath() +
				      " is not an available dataset");
	}

	THREDDSPrinter catalog;

	if (clientVersion == null || clientVersion[0].equals("1.0")) {
	    catalog = v10Printer;
	} else if (clientVersion[0].equals("0.6")){
	    catalog = v06Printer;
	} else {
	    throw new ModuleException
		(this, "unsupported THREDDS version: " + clientVersion[0] + 
		 " (must be 0.6 or 1.0)");
	}	    
	  
	response.setContentType("text/xml");
	response.setDateHeader("Last-Modified", server.getLastConfigTime());

	PrintStream page;
	try {
	    page = 
		new PrintStream
		(response.getOutputStream());

	    String catalogName = handle.getName();
	    if (catalogName.equals("")) {
		catalogName = server.getServerName();
	    }
	    catalog.printHeader(page, 
				getBaseURL(clientRequest), 
				catalogName);

	    InputStream cache = catalog.load(handle, recurse);
	    Spooler.spool(cache, page);
	    cache.close();
	} catch (IOException ioe) {
	} finally {
	    handle.getSynch().release();
	}
    }

    protected abstract class THREDDSPrinter {
	
	protected InputStream load(Handle handle, boolean recurse) 
	    throws ModuleException {

	    File cache = server.getStore().get
		(THREDDSCatalogService.this, 
		 handle.getCompleteName().replace('/','_') + 
		 "-catalog-" + getVersion() + "-recurse-" + recurse + ".xml", 
		 server.getLastConfigTime());
	    try {
		if (!cache.exists()) {
	    if (debug()) debug("generating new THREDDS catalog (version " + 
			     getVersion() + ")");

		    PrintStream print;
		    print = new PrintStream(new FileOutputStream(cache));
		    printCatalog(print, handle, recurse);
		    print.close();
		} else {
		    if (debug()) debug("cached THREDDS catalog (version " + 
				     getVersion() + "): " +
				     cache.getAbsolutePath());
		}		
		InputStream in = 
		    new BufferedInputStream
		    (new FileInputStream
		     (cache));
		return in;
		
	    } catch (IOException ioe) { 
		throw new ModuleException
		    (THREDDSCatalogService.this, 
		     "saving THREDDS catalog version " + 
		     getVersion() + " failed");
	    }
	}

	protected void printCatalog(PrintStream page, Handle handle, 
				    boolean recurse) 
	    throws ModuleException {

	    
	    try {
		if (handle instanceof DirHandle) {
		    printDir(page, (DirHandle)handle, "  ", recurse);
		} else {
		    printDataset(page, (DataHandle)handle, "  ");
		}
	    } catch (ModuleException me) {
		throw me;
	    } 

	    printFooter(page);
	    page.flush();
	}


	protected abstract String getVersion();
	protected abstract void printHeader(PrintStream page, String baseURL,
					    String catalogName);
	protected abstract void printFooter(PrintStream page);

	protected abstract void printDataset(PrintStream page, 
					     DataHandle dataset,
					     String indent);
	protected void printDir(PrintStream page, 
				DirHandle dir, String indent, boolean recurse) 
	    throws ModuleException{

	    Collection datasets = dir.getEntries(false).values();
	
	    Iterator it = datasets.iterator();
	    while (it.hasNext()) {
		Handle handle = (Handle)it.next();
		handle.getSynch().lock();
		if (handle instanceof DirHandle) {
		    DirHandle subdir = (DirHandle)handle;
		    if (recurse) {
			page.print(indent + "<dataset name=\"");
			page.print(subdir.getName() + "\" >\n");

			printDir(page, subdir, indent + "  ", recurse);

			page.print(indent + "</dataset>\n");
		    } else {
			page.print(indent + 
				   "<catalogRef xlink:title=\"" + 
				   subdir.getName() +
				   "\" xlink:href=\"" + 
				   subdir.getCompleteName().substring(1) + "." + 
				   getServiceName() +
				   "\" />");
		    }
		} else {
		    printDataset(page, (DataHandle)handle, indent);
		}
		handle.getSynch().release();
	    }
	}	

    }

    protected class THREDDSv06Printer
	extends THREDDSPrinter {

	protected String getVersion() {
	    return "0.6";
	}

	protected void printHeader(PrintStream page, 
				   String baseURL, 
				   String catalogName) {
    
	    page.print
		("<?xml version=\"1.0\"?>\n");
	    page.print
		("<!DOCTYPE catalog SYSTEM \"http://www.unidata.ucar.edu/projects/THREDDS/xml/InvCatalog.0.6.dtd\">\n");
	    page.print
		 ("<catalog" +
		  " xmlns=\"http://www.unidata.ucar.edu/thredds\"" +
		  " xmlns:xlink=\"http://www.w3.org/1999/xlink\"" +
		  " version=\"0.6\" name=\"" + server.getServerName() + 
		  "\" >\n");
	    page.print
		("<dataset name=\"" + catalogName + "\" >\n" +
		"  <service name=\"" + server.getModuleName() + 
		"\" serviceType=\"DODS\" base=\"" + baseURL + "\" />\n");
	}

	protected void printDataset(PrintStream page, 
				    DataHandle dataset, 
				    String indent) {
	    String desc = 
		Strings.escapeXMLSpecialChars(dataset.getDescription());
	    String name = 
		Strings.escapeXMLSpecialChars(dataset.getCompleteName());
	    
	    page.print(indent);
	    page.print("<dataset name=\"");
	    page.print(desc + "\"\n");
	    page.print(indent + "         urlPath=\"");
	    page.print(name + "\"\n");
	    page.print(indent + "         serviceName=\"" + 
		       server.getModuleName() + "\" />\n");
	}

	protected void printFooter(PrintStream page) {
	    page.print("</dataset>\n" +
		       "</catalog>\n");
	}

    }

    protected class THREDDSv10Printer
	extends THREDDSPrinter {

	protected String getVersion() {
	    return "1.0";
	}

	protected void printHeader(PrintStream page, String baseURL,
				   String catalogName) {
	
	    page.print
		("<?xml version=\"1.0\"?>\n");
	    page.print
		 ("<catalog" + 
		  " xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"" +
		 " xmlns:xlink=\"http://www.w3.org/1999/xlink\"" +
		 " version=\"1.0\" name=\"" + server.getServerName() + 
		  "\" >\n");
	    page.print
		("<dataset name=\"" + catalogName + "\" >\n" +
		"  <service name=\"" + server.getModuleName() + 
		"\" serviceType=\"DODS\" base=\"" + baseURL + "\" />\n");

	}

	protected void printDataset(PrintStream page, 
				    DataHandle dataset,
				    String indent) {

	    String desc = 
		Strings.escapeXMLSpecialChars(dataset.getDescription());
	    String name = 
		Strings.escapeXMLSpecialChars(dataset.getCompleteName());
	    
		page.print(indent + "<dataset name=\"");
		page.print(desc + "\"\n");
		page.print(indent + "         urlPath=\"");
		page.print(name + "\"\n");
		page.print(indent + "         serviceName=\"" + 
			   server.getModuleName() + "\" />\n");
		
	}

	protected void printFooter(PrintStream page) {
	    page.print("</dataset>\n");
	    page.print("</catalog>\n");
	}

    }

    protected THREDDSPrinter v06Printer = new THREDDSv06Printer();
    protected THREDDSPrinter v10Printer = new THREDDSv10Printer();

       
}
