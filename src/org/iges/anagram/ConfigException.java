/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram;

/** Thrown when a module is unable to compensate for 
 *  invalid or missing settings during configuration.
 *  Throwing this method from Module.configure() will cause
 *  server startup or reconfiguration to fail. 
 */
public class ConfigException 
    extends ModuleException {

    /** Creates a ConfigException associated with the given Module,
     *  with the given message. */
    public ConfigException(Module module, String message) {
	super(module, message);
    }

    /** Creates a ConfigException associated with the given Module
     *  and Setting, with the given message. */
    public ConfigException(Module module, String message, Setting setting) {
	super(module, message);
	this.setting = setting;
    }

    public String getMessage() {
	if (setting == null) {
	    return super.getMessage();
	} else {
	    return super.getMessage() + "\ntag: " + setting;
	}
    }

    protected Setting setting;

}
