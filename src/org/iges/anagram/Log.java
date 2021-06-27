/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram;

import java.io.*;
import java.text.*;
import java.util.*;

import org.w3c.dom.Element;

import org.iges.util.FileResolver;

/** Provides logging services for the server.
 */
public class Log 
    implements Module {
    
    // module interface
  
    public Log() {
	this.defaultLevel = INFO;
	moduleSettings = new HashMap();
	consoleMode();
	pos = new FieldPosition(Calendar.MONTH); // arbitrary
    }
    
    public final String getModuleID() {
	return "log";
    }
    
    public final String getModuleName() {
	return moduleName;
    }
    
    public final void init(Server server, Module parent) {
	this.server = server;
	moduleName = parent.getModuleName() + "/" + getModuleID();
    }
    
    public void configure(Setting setting)
	throws ConfigException {

	synchronized (logWriter) {
	    synchronized(moduleSettings) {
		parseLevel(setting);
		parseMode(setting);
		parseModuleSettings(setting);
	    }
	}	
    }
        
    // log interface

    /** Writes a message to the current log output. <p>Critical error
     *  messages are prefaced with the string "CRITICAL: ". Error messages
     *  are prefaced with the string "error: ". Debug messages are 
     *  prefaced with the string "*dbg* ". All other messages are
     *  written as is.
     * @param level The level of message - debug, verbose, info,
     error, or critical
     * @param module The module that is generating the message
     * @param message The message itself.
     */
    public void log(int level, Module module, String message) {
	if (!enabled(level, module)) {
	    return;
	}

	if (mode == ROTATE_MODE) {
	    rotater.check();
	}

	StringBuffer msg = new StringBuffer();
	entryFormat.format(new Date(), msg, pos);
	if (printMem) {
	    msg.append(" ");
	    msg.append(String.valueOf(Runtime.getRuntime().freeMemory() 
				      / 1024));
	    msg.append("KB");
	} 
	if (printModule) {
	    msg.append(" [ ");
	    msg.append(module.getModuleName());
	    msg.append(" ]");
	}
	msg.append(" ");
	if (level == Log.CRITICAL) {
	    msg.append("CRITICAL: ");
	} else if (level == Log.ERROR) {
	    msg.append("error: ");
	} else if (level == Log.DEBUG) {
	    msg.append("*dbg* ");
	}
	msg.append(message);

	synchronized(logWriter) {
	    logWriter.println(msg);
	    logWriter.flush();
	}
    }


    /** Writes a critical error message to the current log output.
     * @param module The module that is generating the message
     * @param message The message itself.
     */
    public void critical(Module module, String message) {
	log(CRITICAL, module, message);
    }

    /** Writes an error message to the current log output.
     * @param module The module that is generating the message
     * @param message The message itself.
     */
    public void error(Module module, String message) {
	log(ERROR, module, message);
    }

    /** Writes an info message to the current log output.
     * @param module The module that is generating the message
     * @param message The message itself.
     */
    public void info(Module module, String message) {
	log(INFO, module, message);
    }
	
    /** Writes a verbose message to the current log output.
     * @param module The module that is generating the message
     * @param message The message itself.
     */
    public void verbose(Module module, String message) {
	log(VERBOSE, module, message);
    }
    
    /** Writes a debug message to the current log output.
     * @param module The module that is generating the message
     * @param message The message itself.
     */
    public void debug(Module module, String message) {
	log(DEBUG, module, message);
    }

    /** Returns true if logging detail is set to the given level or 
     *  higher for the specified module.
     */
    public boolean enabled(int level, Module module) {
	synchronized (moduleSettings) {
	    Integer moduleLevel = 
		(Integer)moduleSettings.get(module.getModuleName());
	    if (moduleLevel == null) {
		return level >= defaultLevel;
	    } else {
		return level >= moduleLevel.intValue();
	    }
	}
    }


    // implementation

    /** Sets flags for what messages should be printed */
    protected void parseLevel(Setting setting) {
	String printMemAttr = setting.getAttribute("print_mem", "false");
	printMem = Boolean.valueOf(printMemAttr).booleanValue();;

	String printModuleAttr = setting.getAttribute("print_module", "false");
	printModule = Boolean.valueOf(printModuleAttr).booleanValue();;

	dateTemplate = setting.getAttribute("date_format");

	defaultLevel = getLevel(setting.getAttribute("level", "info"));
	if (defaultLevel < 0) {
	    defaultLevel = Log.INFO;
	    error(this, "invalid 'level' attribute: " + setting);
	}
	verbose(this, "default log level is " + 
		Log.LEVEL_NAME[defaultLevel]);
    }

    /** Sets the log mode to console, file, or rotating. */
    protected void parseMode(Setting setting) 
	throws ConfigException {

	String modeName = setting.getAttribute("mode", "file");
	String defaultLogFile = "log/" + server.getModuleName() + ".log";
	logFileName = setting.getAttribute("file", defaultLogFile);

	if (getMode(modeName) != mode) {
	    switch (getMode(modeName)) {
	    case FILE_MODE:
		info(this, "directing output to " + logFileName);
		if (dateTemplate.equals("")) {
		    entryFormat = new SimpleDateFormat("MMM dd HH:mm:ss");
		} else {
		    entryFormat = new SimpleDateFormat(dateTemplate);
		}
		open(logFileName);
		mode = FILE_MODE;
		break;
	    
	    case ROTATE_MODE:
		try {
		    rotater = 
			new Rotater(setting.getAttribute("frequency", ""));
		} catch (AnagramException ae) {
		    throw new ConfigException(this, ae.getMessage(), setting);
		}
		info(this, "directing output to " + 
		     rotater.getCurrentFileName());
		open(rotater.getCurrentFileName());
		mode = ROTATE_MODE;
		break;

	    case -1:
		error(this, "invalid 'mode' attribute: " + setting);
	    case CONSOLE_MODE: 
		info(this, "directing output to console");
		consoleMode();
	    }
	}
    }

    /** Sets up per-module logging configuration */
    protected void parseModuleSettings(Setting setting) {
	moduleSettings.clear();
	List modules = setting.getSubSettings("log_override");
	Iterator it = modules.iterator(); 
	while(it.hasNext()) {
	    Setting moduleSetting = (Setting)it.next();
	    String moduleName = server.getModuleName() + "/" + 
		moduleSetting.getAttribute("module");
	    String levelName = moduleSetting.getAttribute("level");
	    if (moduleName == "") {
		error(this, "missing 'name' attribute: " + moduleSetting);
		continue;
	    }
	    if (levelName == "") {
		error(this, "missing 'level' attribute: " + moduleSetting);
		continue;
	    }
	    int level = Log.getLevel(levelName);
	    if (level < 0) {
		error(this, "invalid 'level' attribute: " + moduleSetting);
		continue;
	    }
	    verbose(this, "log level set to " + levelName + 
			" for " + moduleName);
	    moduleSettings.put(moduleName, new Integer(level));
	}
    }
		

    /** Directs log output to the filename given. */
    protected void open(String filename) {
	if (logWriter != null && mode != CONSOLE_MODE) {
	    logWriter.close();
	}
	File logFile = 
	    FileResolver.resolve(server.getBase(), filename);
	File parent = logFile.getParentFile();
	if (!parent.exists()) {
	    parent.mkdirs();
	}
	try {
	    logWriter = new PrintWriter
		(new BufferedWriter
		    (new FileWriter
			(logFile.getAbsolutePath(), true)));
	} catch (IOException ioe) {
	    consoleMode();
	    error(this, "couldn't open " + filename + " for logging " + 
		  ioe.getMessage());
	}
    }


    /** Directs log output to the console (stderr). */
    protected void consoleMode() {
	if (logWriter != null && mode != CONSOLE_MODE) {
	    logWriter.close();
	}
	if (dateTemplate == null || dateTemplate.equals("")) {
	    entryFormat = new SimpleDateFormat("MMM dd HH:mm:ss");
	} else {
	    entryFormat = new SimpleDateFormat(dateTemplate);
	}
	mode = CONSOLE_MODE;
	logWriter = new PrintWriter(new OutputStreamWriter(System.err));
	
    }

    protected Server server;

    protected int defaultLevel;
    protected int mode;
    protected boolean printMem;
    protected boolean printModule;
    protected String dateTemplate;

    protected DateFormat entryFormat;
    protected FieldPosition pos;
    protected String logFileName;
    protected Rotater rotater;
    protected PrintWriter logWriter;

    protected String moduleName;

    protected Map moduleSettings;

    /** Translates a given log level name into an integer constant. */
    public final static int getLevel(String levelName) {
	for (int i = 0; i < LEVEL_NAME.length; i++) {
	    if (LEVEL_NAME[i].equals(levelName)) {
		return i;
	    }
	}
	return -1;
    }

    /** Level of log output */
    public final static int DEBUG = 0;
    /** Level of log output */
    public final static int VERBOSE = 1;
    /** Level of log output */
    public final static int INFO = 2;
    /** Level of log output */
    public final static int ERROR = 3;
    /** Level of log output */
    public final static int CRITICAL = 4;
    /** Level of log output */
    public final static int SILENT = 5;
    public final static int NUM_LEVELS = 6;

    /** Name associated with each level. Matches integer constant list above. */  
    public final static String[] LEVEL_NAME = {
	"debug",
	"verbose",
	"info",
	"error",
	"critical",
	"silent"
    };

    /** Translates a given log level name into an integer constant. */
    protected final static int getMode(String modeName) {
	for (int i = 0; i < MODE_NAME.length; i++) {
	    if (MODE_NAME[i].equals(modeName)) {
		return i;
	    }
	}
	return -1;
    }

    /** Mode for log output */
    protected final static int CONSOLE_MODE = 0;
    /** Mode for log output */
    protected final static int FILE_MODE = 1;
    /** Mode for log output */
    protected final static int ROTATE_MODE = 2;

    /** Name associated with each log mode. Matches integer constant
     * list above. */  
    protected final static String[] MODE_NAME = new String[] {
	"console",
	"file",
	"rotate"
    };

    /** Produces rotating filenames either monthly, weekly, or daily. */ 
    protected class Rotater {

	/** Accepts either "month", "week", or "day". */
	protected Rotater(String frequencyString) 
	    throws AnagramException {

	    cal = new GregorianCalendar();

	    if (!dateTemplate.equals("")) {
		entryFormat = new SimpleDateFormat(dateTemplate);

	    } else if (frequencyString.equals("month")) {
		frequency = Calendar.MONTH;
		fileFormat = new SimpleDateFormat(".MM-yyyy");
		entryFormat = new SimpleDateFormat("MMM dd HH:mm:ss");

	    } else if (frequencyString.equals("week")) {
		frequency = Calendar.WEEK_OF_YEAR;
		fileFormat = new SimpleDateFormat(".MM-dd-yyyy");
		entryFormat = new SimpleDateFormat("MMM dd HH:mm:ss");

	    } else if (frequencyString.equals("day")) {
		frequency = Calendar.DAY_OF_WEEK;
		fileFormat = new SimpleDateFormat(".MM-dd-yyyy");
		entryFormat = new SimpleDateFormat("HH:mm:ss");

	    } else {
		throw new AnagramException("invalid rotation frequency " + 
					   frequencyString);
	    }

	    value = computeValue();
	}

	/** Re-opens logfile if the name has rotated. */
	protected void check() {    
	    synchronized (logWriter) {
		if (!(value == computeValue())) {
		    open(getCurrentFileName());
		}
	    }
	}

	/** Calculates the filename for the current time */
	protected int computeValue() {
	    cal.setTime(new Date());
	    return cal.get(frequency);
	}

	/** Returns the most recently calculated filename */
	protected String getCurrentFileName() {
	    String formatSuffix = fileFormat.format(new Date());
	    return logFileName + formatSuffix;
	}


	protected DateFormat fileFormat;
	protected Calendar cal;
	protected int value;
	protected int frequency;
	
    }
    
}
