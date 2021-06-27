/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server.dap;

import java.io.*;
import dods.dap.Server.CEEvaluator;
import org.iges.anagram.AnagramException;

/** A holdover from the previous GDS version.
 *
 * Last modified: $Date: 2008/07/22 17:22:28 $ 
 * Revision for this file: $Revision: 1.5 $
 * Release name: $Name: v2_0 $
 * Original for this file: $Source: /homes/cvsroot/gds/gds/src/org/iges/grads/server/dap/GradsServerMethods.java,v $
 */
public interface GradsServerMethods {
    
    /** Works similarly to the serialize() method in the 
     *  dods.dap.Server.ServerMethods interface, but can send
     *  either ASCII data or a binary stream.
     */
    public void serialize(String datasetName, 
			  DataOutputStream sink,
			  CEEvaluator ce,
			  Object specialO,
			  boolean useASCII)
	throws AnagramException;

}
