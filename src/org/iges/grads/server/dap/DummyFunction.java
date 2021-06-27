/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server.dap;

import java.io.*;
import java.util.*;


import dods.dap.*;
import dods.dap.Server.*;

/** A hack for the pre-JavaDODS 1.1 implementation of constraint expression
 *  parsing. When evaluated, the DummyFunction just prints its name and
 *  arguments. This is the only way to get this information from the JavaDODS
 *  1.0 API. 
 */
public class DummyFunction
    implements BoolFunction {

    public DummyFunction(String name) {
	this.name = name;
    }

    public String getName() {
	return name;
    }

    /** Get the value of this function. If the values of
	all the arguments are guaranteed not to change, the value may be
	computed only once. If the values might change (i.e., they depend on
	values of sequences or remote variables) the function will be rerun
	for each call to this method.
	@return The value of the clause.
	@exception InvalidOperatorException is thrown if the function cannot 
	be evaluated on the given clauses
    */
    public String printArgs(List children)
	throws InvalidOperatorException, RegExpException,
	       NoSuchVariableException, SBHException, IOException,
	       SDODSException { 
	StringWriter value = new StringWriter();
	PrintWriter w = new PrintWriter(value);
	w.print(name);
	w.print(":");
	for (int i = 0; i < children.size(); i++) {
	    w.print(" ");
	    BaseType result = ((SubClause)children.get(i)).evaluate();
	    if (result instanceof DString) {
		w.print(((DString)result).getValue());
	    } else if (result instanceof DFloat64) {
		w.print(((DFloat64)result).getValue());
	    } else if (result instanceof DInt32) {
		w.print(((DInt32)result).getValue());
	    } 
	}
	w.close();
	return value.toString();
    }

   /** Get the value of this function. If the values of
	all the arguments are guaranteed not to change, the value may be
	computed only once. If the values might change (i.e., they depend on
	values of sequences or remote variables) the function will be rerun
	for each call to this method.
	@return The value of the clause.
	@exception InvalidOperatorException is thrown if the function cannot 
	be evaluated on the given clauses
   */
    public boolean evaluate(List args) { 
	return true;
    }

    public void checkArgs(List args) {
    }

    String name;
}
