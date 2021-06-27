/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram;

import org.w3c.dom.Element;

/** The fundamental software component in the Anagram framework.<p>
 * 
 *  The Anagram server is a hierarchical collection of modules 
 *  that communicate with each other and exchange various temporary objects
 *  in order to handle servlet requests.
 *  Each module encapsulates a portion of the server's functionality.<p>
 *  
 *  Modules are distinct from other Anagram classes in that they:
 *  <list>
 *    <li>have an ID, and a complete name based on 
 *        their parents in the module hierarchy </li>
 *    <li>support a standard initialization and configuration interface</li>
 *    <li>are provided with a reference to to the top-level module 
 *        (the Server object) and thus can access all other public
 *        modules in the hierarchy</li>
 *    <li>must be threadsafe (with the exception of the init() and 
 *       configure() methods)</li>
 *  </list>
 */
public interface Module {

    /** Returns the complete name of the module, including parents.
     *  The syntax is:<code>
     *  complete_name :  [parents] module_id<br>
     *  parents : [parents] parent '/' </code>
     *  The complete module name is used as an identifier in log messages.
     */
    public String getModuleName();

    /** Returns an ID for this module. This ID should be a legal XML
     *  tag name. It has two uses:  as a tag name in the server's 
     *  configuration file, and as the final element of the complete module
     *  name.
     */
    public abstract String getModuleID();

    /** Initializes the module.<p>
     *  This method should copy the server and parent references provided
     *  to internal fields, so that the module has  access to the rest of 
     *  the module hierarchy, and then call the init() method of any
     *  sub-modules. 
     *  It can also be used to perform any one-time initialization that 
     *  requires access to other modules. <p>
     *  This method will only be called once, immediately after the module is 
     *  created, and before it is configured for the first time. 
     *  Thus it does not need to be thread-safe.
     */
    public void init(Server server, Module parent);


    /** Configures the module according to the settings provided. <p>
     * 
     *  This method is guaranteed to be called at least once before
     *  any requests are sent to the module.
     * 
     *  The server supports dynamic reconfiguration, and thus this method 
     *  may be called any number of times during the life of the module. 
     *  However, it is guaranteed that this method will never be called
     *  while a servlet request is being processed. Thus  
     *  it does not need to be thread-safe.<p>
     *  
     * If this module contains other modules, it is responsible for 
     * configuring them using the appropriate sub-settings. 
     * Each module should receive the sub-setting that matches its module
     * ID, so that the XML tags in the configuration file match up with
     * the module names in the log file.<p>
     *
     * @param settings The settings to be used in configuring the module.
     * @throws ConfigException If the module is unable to operate using
     *  the settings provided. This will halt the server's operation, and
     *  thus should only be thrown if there is no reasonable default 
     *  that can be used in place of a missing or invalid setting. 
     */
    public void configure(Setting setting) 
	throws ConfigException;


}
