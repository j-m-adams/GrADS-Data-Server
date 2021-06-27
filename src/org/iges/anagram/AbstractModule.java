/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/

package org.iges.anagram;

import org.w3c.dom.Element;

/** Provides default implementations of most of the Module interface,
 *  plus some convenience methods for logging and throwing exceptions. <p>
 *  
 *  The only methods that remain to be implemented by subclasses 
 *  are getModuleID() and configure(Setting setting).
*/
public abstract class AbstractModule
    implements Module {

    /** Returns the full name of this module, including parent modules */
    public String getModuleName() {
	return moduleName;
    }

    public abstract String getModuleID();


    /** Configures the module according to the settings provided. The 
     *  server supports dynamic reconfiguration. Thus this method may be 
     *  called at any time in the life of the module. If the module
     *  cannot be reconfigured, it should throw an exception.
     * @param settings The settings to be used in configuring the module.
     * @param server A handle to the server in which this module is running.
     * @throws SettingException if invalid settings are provided; 
     * if this module has previously been configured, and does not support 
     * dynamic reconfiguration; or if the destroy() method has already been
     * called */
    public abstract void configure(Setting setting)
	throws ConfigException;


    /** Convenience method - extracts the appropriate sub-setting from 
     *  the parent setting, and uses it to configure the module given
     * @param module Module to be configured
     * @param setting Setting that is the <i>parent</i> of the settings 
     * for the module to be configured.
     */
    protected void configModule(Module module, Setting parent)
	throws ConfigException {

	try {
	    Setting moduleSetting = 
		parent.getUniqueSubSetting(module.getModuleID());
	    module.configure(moduleSetting);
	} catch (ConfigException ce) {
	    throw ce;
	} catch (AnagramException ae) {
	    throw new ConfigException(this, ae.getMessage(), parent);
	}
    }


    /** Performs default configuration using a blank Setting */
    /*
    public void configure() 
	throws ConfigException {
	
	try {
	    if (debug()) debug("got no config info; using default settings");
	    configure(new Setting());
	} catch (AnagramException ae) {
	    throw new ConfigException(this, ae.getMessage());
	}
    }
    */

    /** Saves references to the server, log module, and parent module,
     *  and constructs the complete module name. Subclasses which
     *  override this method should call super.init().
     */
    public void init(Server server, Module parent) {
	this.server = server;
	this.log = server.getLog();
	this.parent = parent;
	this.moduleName = parent.getModuleName() + "/" + getModuleID();
    }

    /** Convenience method - returns true if debugging output is
     *   enabled for this module.
     *  All debug messages should be enclosed in a conditional that 
     *  tests this function. Otherwise, even when debugging is off, 
     *  the server will be forced to construct all the debug message strings,
     *  resulting in a huge number of unnecessary String object creations.
     */
    protected final boolean debug() {
	return log.enabled(Log.DEBUG, this);
    }

    /** Convenience method - returns true if verbose output is enabled 
     *  for this module.
     *  All verbose messages should be enclosed in a conditional that 
     *  tests this function. Otherwise, even when verbose output is off, 
     *  the server will be forced to construct all the verbose message strings,
     *  resulting in a huge number of unnecessary String object creations.
     */
    protected final boolean verbose() {
	return log.enabled(Log.VERBOSE, this);
    }

    /** Convenience method - sends a debug message to the logger */
    protected final void debug(String msg) {
	log.log(Log.DEBUG, this, msg);
    }

    /** Convenience method - sends a verbose message to the logger */
    protected final void verbose(String msg) {
	log.log(Log.VERBOSE, this, msg);
    }

    /** Convenience method - sends an info message to the logger */
    protected final void info(String msg) {
	log.log(Log.INFO, this, msg);
    }

    /** Convenience method - sends an error message to the logger */
    protected final void error(String msg) {
	log.log(Log.ERROR, this, msg);
    }

    /** Convenience method - equivalent to throwing a ModuleException  */
    protected final void fail(String msg) 
	throws ModuleException {
	log.error(this, msg);
	throw new ModuleException(this, msg);
    }

    /** Convenience method - equivalent to throwing a ModuleException  */
    protected final void fail(String msg, Throwable cause) 
	throws ModuleException {
	ModuleException me = new ModuleException(this, msg, cause);
	throw me;
    }

    /** Convenience method - equivalent to throwing a ModuleException  */
    protected final void fail(String clientMsg, String serverMsg) 
	throws ModuleException {
	throw new ModuleException(this, clientMsg);
    }

    /** Parent of this module */
    protected Module parent;

    /** Complete name of this module, including parents */
    protected String moduleName;

    /** Reference to the top-level module */
    protected Server server;

    /** Reference to the log module */
    protected Log log;

    /** Shuts down the module. This method should put the module
     *  in a state such that it is ready to be garbage collected.
     public void destroy();
    */

    public String toString() {
	return getModuleName();
    }

}
