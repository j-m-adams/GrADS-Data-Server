/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server.dap;

import dods.dap.Server.*;
import dods.dap.*;
import java.io.*;
import java.util.*;

/**
 * Represents a server-side Grid object for the GrADS Data server.
 * All datatypes that appear in DDS'es for GrADS datasets must have
 * server-side implementations, even if its just a shell.
 */
public class GradsGrid 
    extends SDGrid {
    
  /** Constructs a new GradsGrid. */
  public GradsGrid() { 
    super(); 
  }

  /**
   * Constructs a new GradsGrid with name n.
   * @param n the name of the variable.
   */
  public GradsGrid(String n) { 
    super(n); 
  }
 
    /** Read a value from the named dataset for this variable. 
        @param datasetName String identifying the file or other data store
        from which to read a vaue for this variable.
        @param specialO not used in this implementation
        @return always false in this implementation
    */
    public boolean read(String datasetName, Object specialO)
	throws NoSuchVariableException, IOException, EOFException {
	
	// First read the contents of the grid
        SDArray contents = (SDArray)getVar(0);
        if(contents.isProject())
            contents.read(datasetName, specialO);
	
	// Then read the map arrays for each dimension
        for(int i = 0; i < contents.numDimensions(); i++){
            SDArray map = (SDArray)getVar(i+1);
            if(map.isProject())
                map.read(datasetName,specialO);
        }
   	    
	// Flag read operation as complete.
	setRead(true);
	
	// False means no more data to read.
	return false;
    }


    /** Returns a list of dimension vectors for this grid.
     *  Why this isn't in the DODS API, I don't know..
     */
    public Vector getDimensions() {
	return new Vector(mapVars);
    }



    
}











