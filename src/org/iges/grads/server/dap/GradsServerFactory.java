/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server.dap;
import dods.dap.*;

/** A factory for GrADS Data server-side data objects 
 *  The only supported types are Float32, Float64, String, Sequence, 
 *  Grid, and Array.
*/
public class GradsServerFactory 
    extends DefaultFactory {

    /** Returns a unique instance of this class */
    public static GradsServerFactory getFactory() {
	if (factory == null) {
	    factory = new GradsServerFactory();
	}
	return factory;
    }

    private static GradsServerFactory factory;

    /** 
     * Construct a new DFloat32.
     * @return the new DFloat32
     */
    public DFloat32 newDFloat32() {
	return new GenericFloat32();
    }

    /**
     * Construct a new DFloat32 with name n.
     * @param n the variable name
     * @return the new DFloat32
     */
    public DFloat32 newDFloat32(String n) {
	return new GenericFloat32(n);
    }

    /** 
     * Construct a new DFloat64.
     * @return the new DFloat64
     */
    public DFloat64 newDFloat64() {
	return new GenericFloat64();
    }

    /**
     * Construct a new DFloat64 with name n.
     * @param n the variable name
     * @return the new DFloat64
     */
    public DFloat64 newDFloat64(String n) {
	return new GenericFloat64();
    }


    /** 
     * Construct a new DString.
     * @return the new DString
     */
    public DString newDString() {
	return new GenericString();
    }

    /**
     * Construct a new DString with name n.
     * @param n the variable name
     * @return the new DString
     */
    public DString newDString(String n) {
	return new GenericString();
    }

    /** 
     * Construct a new DInt32.
     * @return the new DInt32
     */
    public DInt32 newDInt32() {
	return new GenericInt32();
    }

    /**
     * Construct a new DInt32 with name n.
     * @param n the variable name
     * @return the new DInt32
     */
    public DInt32 newDInt32(String n) {
	return new GenericInt32(n);
    }

    /** 
     * Construct a new DArray.
     * @return the new DArray
     */
    public DArray newDArray() {
	return new GradsArray();
    }

    /**
     * Construct a new DArray with name n.
     * @param n the variable name
     * @return the new DArray
     */
    public DArray newDArray(String n) {
	return new GradsArray(n);
    }

    /** 
     * Construct a new DGrid.
     * @return the new DGrid
     */
    public DGrid newDGrid() {
	return new GradsGrid();
    }

    /**
     * Construct a new DGrid with name n.
     * @param n the variable name
     * @return the new DGrid
     */
    public DGrid newDGrid(String n) {
	return new GradsGrid(n);
    }

    /** 
     * Construct a new DSequence.
     * @return the new DSequence
     */
    public DSequence newDSequence() {
	return new GradsSequence();
    }

    /**
     * Construct a new DSequence with name n.
     * @param n the variable name
     * @return the new DSequence
     */
    public DSequence newDSequence(String n) {
	return new GradsSequence(n);
    }
}

