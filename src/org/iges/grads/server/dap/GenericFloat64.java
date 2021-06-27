/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server.dap;

import dods.dap.Server.*;
import dods.dap.*;
import java.io.*;


/** Implements the server-side version of Float64 
 * All datatypes that appear in DDS'es for GrADS datasets must have
 * server-side implementations, even if its just a shell.
 */
public class GenericFloat64 
    extends SDFloat64 {
    
    /** Constructs a new <code>GenericFloat64</code>. */
    public GenericFloat64() { 
	super(); 
    }
    
    /**
     * Constructs a new <code>GenericFloat64</code> with name <code>n</code>.
     * @param n the name of the variable.
     */
    public GenericFloat64(String n) { 
	super(n); 
    }
    
    
    /** Dummy procedure
     */
    public boolean read(String datasetName, Object specialO)
	throws NoSuchVariableException, IOException, EOFException {
        setRead(true);
        return false;
    }
}
