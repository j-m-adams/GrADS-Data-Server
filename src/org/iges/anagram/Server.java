/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram;

import java.io.*;
import java.util.*;

import javax.servlet.*;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.*;

import org.iges.util.*;
import org.iges.anagram.service.*;
import org.iges.anagram.filter.*;

/** The top-level module of the Anagram framework. 
 *  All other modules share a reference to a single Server instance.<p>  
 *
 *  The Server class performs four basic functions. It obtains Anagram
 *  properties from the enviroment, creates the modules
 *  that will make up the server, initiates the configuration process,
 *  and provides each module with access to the others.<p>
 *  
 *  Anagram properties are mostly intended to be set by the 
 *  developer, not the user, and are guaranteed not to change during the life
 *  of the server (unlike configuration settings from the configuration
 *  file). They include basic things such as the name of the Anagram
 *  implementation, and its support home page.<p>
 *  
 *  The modules that will make up the Anagram implementation are created 
 *  by the Server module at startup. Dynamic module creation may be supported
 *  in the future, but for now, the module structure of the server does
 *  not change over the lifetime of the Server.<p>
 *
 *  In contrast, the server's configuration can change multiple times during
 *  the life of the server. Configuration is performed once when the server 
 *  is created,
 *  but may also be triggered again by an administrative request. This allows
 *  the server's settings and data holdings to be updated without restarting
 *  the servlet or its container.<p>
 *  
 *  Configuration consists of parsing the XML configuration
 *  file specified by the Anagram property "anagram.config",
 *  and passing the appropriate portions of it to the configure() methods 
 *  of the various modules. <p>
 *  
 *  Finally, each module can access other modules using its reference to the Server.
 *  Since modules remain constant for the lifetime of the server, 
 *  it is safe for a 
 *  module to save the references it obtains from the server.<p>
 */
public class Server 
    implements Module {

    /** Used to synchronize operations on the entire server. 
     *  Each client request holds a non-exclusive lock on the server while it
     *  is being processed.
     *  The configuration process obtains an exclusive lock on the 
     *  server, thus guaranteeing that no requests will be in-process
     *  while the server is re-configuring itself.
     */
    public ExclusiveLock getSynch() {
	return synch;
    }

    /** The module ID of the server is determined by the Anagram property
     *  "anagram.impl". This should be a short alpha-numeric abbrevation
     *  of the implementation name. For instance My Anagram Implementation
     *  might be condensed to "my_agm", or "mai". It must be a legal
     *  XML tag name.
     */
    public String getModuleID() {
	return implName;
    }

    /** Because the server is the top-level module, its module name
     *  is identical to its module ID. 
     */
    public String getModuleName() {
	return implName;
    }

    /** Returns the descriptive name of this server instance. This
     *  is a configuration property, not an Anagram property.
     */
    public String getServerName() {
	return serverName;
    }

    /** Returns the home page for this server instance. This
     *  is a configuration property, not an Anagram property.
     */
    public String getSiteHomePage(String defaultHome) {
	if (siteHome.equals("")) {
	    return defaultHome;
	} else {
	    return siteHome;
	}
    }

    /** Returns the home directory of the server installation. 
     *  This should be the base for resolving read-only files.
     *  This should not be confused
     *  with  getSiteHomePage() and getImplHomePage(), which are
     *  URLs. This is the Anagram property "anagram.home".
     *  @see org.iges.util.FileResolver
     */
    public String getHome() {
	return serverHome;
    }

    /** Returns the base directory of the server installation. 
     *  This should be the base for resolving all configuration-specific 
     *  or writeable files,  such as
     *  log and temp directories.
     *  This is the Anagram property "anagram.base".
     *  @see org.iges.util.FileResolver
     */
    public String getBase() {
	return serverBase;
    }

    /** Returns the descriptive name of this Anagram implementation,
     *  so that it can be included in web pages and user messages.
     *  This is Anagram property "anagram.impl_long".
     */
    public String getImplName() {
	return implLongName;
    }

    /** Returns the version number for this Anagram implementation,
     *  so that it can be included in web pages and user messages.
     *  This is Anagram property "anagram.impl_version".
     */
    public String getImplVersion() {
	return implVersion;
    }

    /** Returns a URL where more information can be obtained about 
     *  this Anagram implementation, so that it can be included in 
     *  web pages and user messages.
     *  This is Anagram property "anagram.impl_home".
     */
    public String getImplHomePage() {
	return implHome;
    }


    /** Creates a new instance of the Anagram framework connected
     *  to the servlet specified . This method
     *  is called by AnagramServlet.init().
     */
    public Server(AnagramServlet servlet, ServletConfig servletConfig) 
	throws ServletException {

	this.implName = "anagram";
	this.outputLevel = Log.ERROR;
	this.servlet = servlet;
	this.synch = new ExclusiveLock();
	
	String outputSetting = getProperty(servletConfig,
					   "anagram.output",
					   "info");
	for (int i = 0; i < Log.NUM_LEVELS; i++) {
	    if (Log.LEVEL_NAME[i].equals(outputSetting)) {
		outputLevel = i;
	    }
	}
	log(Log.VERBOSE, "output level is " + 
	    Log.LEVEL_NAME[outputLevel]);

	implName = getProperty(servletConfig,
				 "anagram.impl",
				 "anagram");

	implLongName = getProperty(servletConfig,
				   "anagram.impl_long",
				   "Anagram data server framework");

	implVersion = getProperty(servletConfig,
				  "anagram.impl_version",
				  "1.0");

	implClass = getProperty(servletConfig,
				"anagram.impl_class",
				"");

	implHome = getProperty(servletConfig,
			       "anagram.impl_home",
			       "http://cola.gmu.edu/grads/gds");

	serverHome = getProperty(servletConfig, 
				 "anagram.home", 
				 System.getProperty("user.dir"));

	serverBase = getProperty(servletConfig, 
				 "anagram.base", 
				 serverHome);


	String configFilename = getProperty(servletConfig,
					    "anagram.config", 
					    implName + ".xml");

	configFile = FileResolver.resolve(serverBase, configFilename);

	try {
	    Setting serverSetting = loadConfig();
	    startLog(serverSetting);
	    createModules();
	    configure(serverSetting);
	    String msg = "started ok";
	    if (log != null) log(Log.INFO, msg);
	    System.err.println("anagram: " + msg);
	    
	} catch (ConfigException ce) {
	    String msg = "invalid configuration: " + ce.getMessage();
	    fail(msg, ce);

	} catch (Throwable t) {
	    StringWriter debugInfo = new StringWriter();
	    PrintWriter p = new PrintWriter(debugInfo);
	    t.printStackTrace(p);
	    String msg = "oops, exception " + t.getClass() + 
		" was not caught.\n" +
		"please report this as a bug, along with " +
		"the following debug info:\n" +
		debugInfo.toString();

	    fail(msg, t);
	}

	File startedFile = store.get(this, "started");
	try {
	    if (!startedFile.createNewFile()) {
		startedFile.setLastModified(System.currentTimeMillis());
	    }
	} catch (IOException ioe) {
	    if (log != null) {
		log(Log.ERROR, "couldn't touch start file " + 
				 startedFile.getAbsolutePath() + 
				 "; check permissions.");
		log(Log.INFO, "startup script may not function properly");
	    }
	}
	    
    }

    protected void fail(String msg, Throwable t) 
	throws ServletException {

	    if (log != null) log(Log.ERROR, msg);
	    System.err.println("anagram: " + msg);

	    String failMsg = "startup failed. " + 
		"you must run 'stopserver' to shut down Tomcat.";
	    if (log != null) log(Log.CRITICAL, failMsg + "\n\n");
	    System.err.println("anagram: " + failMsg);
	    File failedPath = new File(System.getProperty("java.io.tmpdir")
				       + "/" + implName);
	    failedPath.mkdirs();
	    File failedFile = new File(failedPath, "failed");
	    try {

		if (!failedFile.createNewFile()) {
		    failedFile.setLastModified(System.currentTimeMillis());
		}
	    } catch (Exception e) {
		if (log != null) {
		    log(Log.ERROR, "couldn't touch fail file " + 
			failedFile.getAbsolutePath() + 
			"; check permissions.");
		    log(Log.INFO, "startup script may not function properly");
		} else {
		    System.err.println("anagram: couldn't touch fail file " + 
			failedFile.getAbsolutePath() + 
			"; check permissions.");
		}
	    }

	    throw new ServletException("");
    }

    /** Does nothing for Server */
    public void init(Server server, Module parent) {
    }


    /** Checks the configuration file, and if it has been modified,
     *  parses it and reconfigures the entire server to the new settings.
     *  This operation should only be performed
     *  by a thread that has an exclusive lock on the Server module.
     */
    public void reconfigure() 
	throws ModuleException {

	try {
	    Setting serverSetting = loadConfig();
	    configModule(log, serverSetting);
	    configure(serverSetting);
	    String msg = "reconfigured ok";
	    if (log != null) log(Log.INFO, msg);
	    System.err.println("anagram: " + msg);
	    
	} catch (ConfigException ce) {
	    String msg = "invalid configuration: " + ce.getMessage();
	    System.err.println("anagram: " + msg);
	    throw new ModuleException(this, msg, ce);
	} 
    }

    /** This method should not be called from outside Server.
     *  Configures all the server modules except the log module, 
     *  which is handled separately so that it can be initialized
     *  immediately on startup.
     */
    public void configure(Setting setting) 
	throws ConfigException {
	
	// Disable this because it might be desirable to reconfigure
	// even if the config file itself is unchanged
	/*
	if (configFile.lastModified() < lastConfigTime) {
	    log(Log.INFO, 
		"config file is unmodified, skipping configuration");
	    return;
	}
	*/
	
	lastConfigTime = startTime = System.currentTimeMillis();

	log(Log.VERBOSE, "starting configuration");

	serverName = setting.getAttribute("name", implLongName);
	siteHome = setting.getAttribute("home");

	// log configuration is handled separately
	configModule(store, setting);
	configModule(tool, setting);
	configModule(mapper, setting);
	configModule(privilegeMgr, setting);
	configModule(servlet, setting);
	configModule(catalog, setting);
	
	log(Log.VERBOSE, "finished configuration");
    }
    
    /** Returns the last time that the server's configuration changed,
     *  or else the time that the server was created. */
    public long getLastConfigTime() {
	return lastConfigTime;
    }

    /** Returns a handle to the service mapper */
    public Mapper getMapper() {
	return mapper;
    }

    /** Returns a handle to the servlet */
    public AnagramServlet getServlet() {
	return servlet;
    }

    /** Returns a handle to the data tool */
    public Tool getTool() {
	return tool;
    }
    
    /** Returns a handle to the logger */
    public Log getLog() {
	return log;
    }
    
    /** Returns a handle to the temporary storage interface */
    public Store getStore() {
	return store;
    }
    
    /** Returns a handle to the data object catalog */
    public Catalog getCatalog() {
	return catalog;
    }
    
    /** Returns a handle to the privilege manager */
    public PrivilegeMgr getPrivilegeMgr() {
	return privilegeMgr;
    }
    
    /** Shuts down the server */
    public void destroy() {
	File stoppedFile = store.get(this, "stopped");
	try {
	    if (!stoppedFile.createNewFile()) {
		stoppedFile.setLastModified(System.currentTimeMillis());
	    }
	} catch (IOException ioe) {
	    if (log != null) {
		log(Log.ERROR, "couldn't touch stop file " + 
				 stoppedFile.getAbsolutePath() + 
				 "; check permissions.");
		log(Log.INFO, "startup script may not function properly");
	    }
	}
	log(Log.INFO, "shutting down\n\n");
    }

    
    // implementation
    
    protected String getProperty(ServletConfig config, 
				 String name, 
				 String defaultValue) {
	String value = config.getInitParameter(name);
	if (value != null) {
	    log(Log.VERBOSE, "property " + name + 
		" found as servlet param ");
	    log(Log.INFO, name + " = " + value);
	    return value;
	}


	value = config.getServletContext().getInitParameter(name);
	if (value != null) {
	    log(Log.VERBOSE, "property " + name + 
		 " found as servlet context param ");
	    log(Log.INFO, name + " = " + value);
	    return value;
	}

	value = System.getProperty(name);
	if (value != null) {
	    log(Log.VERBOSE, "property " + name + 
		 " found as system property ");
	    log(Log.INFO, name + " = " + value);
	    return value;
	}

	log(Log.VERBOSE, "property " + name + 
	    " not found; using default");
	value = defaultValue;
	log(Log.INFO, name + " = " + value);
	return value;
    }
    
    protected Setting loadConfig() 
	throws ConfigException {

	try {
	    DocumentBuilder builder = 	
		DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    Document xmlConfig = builder.parse(configFile);
	    log(Log.INFO, "loaded config file " + 
			      configFile.getAbsolutePath());
	    return new Setting(implName, xmlConfig);

	} catch (SAXParseException spe) {
	    throw new ConfigException(this, "couldn't parse config file " + 
				     "at line " + spe.getLineNumber() + 
				     ": " + spe.getMessage());
	} catch (Exception e) {
	    throw new ConfigException(this, "error parsing config file: " + 
				     e.getMessage());
	}
		
    }

    protected void startLog(Setting setting)
	throws ConfigException {

	Log newLog = new Log();
	newLog.init(this, this);
	log(Log.VERBOSE, "created log");
	log = newLog;

	configModule(log, setting);
    }


    /** Creates the components of the server */
    protected void createModules() 
	throws ConfigException {

	log(Log.VERBOSE, "creating tool " + implClass);
	try {
	    tool = (Tool)Class.forName(implClass).newInstance();
	} catch (ClassNotFoundException e) {
	    throw new ConfigException(this, "class not found: " + 
				      implClass);
	} catch (InstantiationException e) {
	    throw new ConfigException(this, "class could not be " + 
				      "instantiated: " + 
				      implClass);
	} catch (IllegalAccessException e) {
	    throw new ConfigException(this, "class could not be " + 
				      "instantiated: " + 
				      implClass);
	} catch (ClassCastException e) {
	    throw new ConfigException(this, "class does not extend " +
				      "org.iges.anagram.tool.Tool: " + 
				      implClass);
	}
	tool.init(this, this);
	
	mapper = new Mapper();
	mapper.init(this, this);
	log(Log.VERBOSE, "created mapper");

	store = new Store();
	store.init(this, this);
	log(Log.VERBOSE, "created store");

	catalog = new Catalog();
	catalog.init(this, this);
	log(Log.VERBOSE, "created catalog");

	privilegeMgr = new PrivilegeMgr();
	privilegeMgr.init(this, this);
	log(Log.VERBOSE, "created privilege mgr");

	servlet.init(this, this);

    }


    protected void configModule(Module module, Setting setting)
	throws ConfigException {

	try {
	    Setting moduleSetting = 
		setting.getUniqueSubSetting(module.getModuleID());
	    module.configure(moduleSetting);
	} catch (ConfigException ce) {
	    throw ce;
	} catch (AnagramException ae) {
	    throw new ConfigException(this, ae.getMessage(), setting);
	}
    }
        
    protected void log(int level, String msg) {
	if (log == null) {
	    if (level >= outputLevel) {
		System.err.print("anagram: ");
		if (level >= Log.ERROR) {
		    System.err.print("error: ");
		}
		System.err.println(msg);
	    }
	} else {
	    log.log(level, this, msg);
	}
    }

    protected ExclusiveLock synch;

    protected int outputLevel;

    protected String implClass;
    protected String implName;
    protected String implLongName;
    protected String implVersion;
    protected String implHome;

    protected String serverName;
    protected String siteHome;

    protected String serverHome;
    protected String serverBase;
    
    protected File configFile;
    protected long lastConfigTime;
    protected long startTime;

    protected Tool tool;
    protected Mapper mapper;
    protected Log log;
    protected Store store;
    protected Catalog catalog;
    protected PrivilegeMgr privilegeMgr;
    protected AnagramServlet servlet;
    
}
