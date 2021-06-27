/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram;

/** Thrown when a module cannot complete the task it is attempting. */
public class ModuleException 
    extends AnagramException {
    
    /** Creates a ModuleException associated with the given module, 
     *  with the given message.
     */
    public ModuleException(Module module, String message) {
	super(message);
	this.module = module;
    }

    /** Creates a ModuleException associated with the given module, 
     *  with the given message and cause.
     */
    public ModuleException(Module module, String message, Throwable cause) {
	super(message, cause);
	this.module = module;
    }

    /** Creates a ModuleException associated with the given module, 
     *  with the given message, plus a different message that should
     *  be returned to the client. This can be used to avoid
     *  revealing sensitive information to the client, or to provide
     *  a detailed message for the client while including a shorter
     *  one in the log file.
     */
    public ModuleException(Module module, String clientMessage, 
			   String message) {
	super(message);
	this.clientMessage = clientMessage;
	this.module = module;
    }

    /** Returns the module that generated this exception */
    public Module getModule() {
	return module;
    }

    /** Returns the client message, if any. */
    public String getClientMessage() {
	return clientMessage;
    }

    protected String clientMessage;
    protected Module module;

}
