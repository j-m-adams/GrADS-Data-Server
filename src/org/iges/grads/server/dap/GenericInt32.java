/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server.dap;

import dods.dap.Server.*;
import dods.dap.*;
import java.io.*;


/** Implements the server-side version of Int32 
 * All datatypes that appear in DDS'es for GrADS datasets must have
 * server-side implementations, even if its just a shell.
 *
 * Last modified: $Date: 2008/07/22 17:22:28 $ 
 * Revision for this file: $Revision: 1.5 $
 * Release name: $Name: v2_0 $
 * Original for this file: $Source: /homes/cvsroot/gds/gds/src/org/iges/grads/server/dap/GenericInt32.java,v $
 */
public class GenericInt32 extends SDInt32 {
    
    private static int rCount = 0;        
	
    /** Constructs a new <code>GenericInt32</code>. */
    public GenericInt32() { 
	super(); 
    }
    
    /**
     * Constructs a new <code>GenericInt32</code> with name <code>n</code>.
     * @param n the name of the variable.
     */
    public GenericInt32(String n) { 
	super(n); 
    }
    
    
    public static void resetCount(){
    	rCount = 0;
    }
    
    /** Dummy procedure
     */
    public boolean read(String datasetName, Object specialO)
	throws NoSuchVariableException, IOException, EOFException {
        setRead(true);
        return (false);
    }
}


