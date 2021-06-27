/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server;

import java.io.*;
import java.util.*;

import dods.dap.*;
import dods.dap.Server.*;

import org.iges.util.Bounds;
import org.iges.util.Range;

import org.iges.util.Spooler;

import org.iges.anagram.*;
import org.iges.grads.server.dap.*;

/** Streams subsets from gridded datasets. */
public class GradsGridSubsetter 
    extends GradsSubsetter {

 public void subset(DataHandle data,
		    CEEvaluator ce, 
		    long subsetLimit, 
		    boolean useASCII, 
		    OutputStream out) 
     throws ModuleException {

     // create a flat list of variables to be subsetted, 
     // by parsing through the nested DDS structure
	ServerDDS dds = ce.getDDS();
	Enumeration e = dds.getVariables();
	List arrays = new ArrayList();
	if (debug()) log.debug(this, "serializing variables for " + data);
	while(e.hasMoreElements()){
	    BaseType var = (BaseType)e.nextElement();
	    if (var instanceof DArray) {
		arrays.add(var);
	    } else {
		Enumeration ensnum = ((DGrid)var).getVariables();
		while (ensnum.hasMoreElements()) {
		    arrays.add(ensnum.nextElement());
		}
	    }
	}

	// iterate through variable list and serialize each one
	DataMethods dataMethods = new DataMethods(data);

	Iterator it = arrays.iterator();
	while (it.hasNext()) {
	    SDArray var = (SDArray)it.next();
	    if (!var.isProject()) {
		continue;
	    }
	    if (debug()) debug("serializing " + var.getName());
	    Serializer serializer = new Serializer(var,
						   out,
						   useASCII,
						   dataMethods,
						   subsetLimit);
	    serializer.serialize();
	}
	
	
    }
    
    /** This is a wrapper class for a DataHandle, which implements
     * operations that are dependent on configuration settings
     * specific to the subset module, e.g., buffer size.  
     */
    public class DataMethods {

	private DataMethods(DataHandle data) {
	    this.data = data;
	    this.info = (GradsDataInfo)data.getToolInfo();
	}

	public boolean hasLevels() {
 	    return info.hasLevels();
        }
	
	public boolean hasEnsemble() {
	    return info.hasEnsemble();
	}

	/** Returns a disk file containing the world coordinates for
	 *  the spatial dimension and grid range specified.  Dimension
	 *  must be "lat", "lon", or "lev". Used by GradsArray.  Note
	 *  that the file contains the entire coordinate array, not a
	 *  subset; GradsArray handles the subsetting.
	 * @see org.iges.grads.server.dap.GradsArray
	 */
	public File getDimData(String dim) 
	    throws ModuleException {
	    
	    File dimFile = 
		server.getStore().get(tool.dods,
				      info.getDODSName() + "." + dim);
	    if (!dimFile.exists()) {
		throw new ModuleException(GradsGridSubsetter.this, 
					  "missing " + dim + " data for " + data);
	    }
	    
	    return dimFile;
	}

	/** Generates a disk file containing a subset of the dataset
	 *   specified by the variable and bounds given, to be streamed by
	 *   GradsArray.
	 * @see org.iges.grads.server.dap.GradsArray
	 */
	public File getSubset(String variable, 
			      Bounds.Grid bounds) 
	    throws ModuleException {

	    File subsetFile = server.getStore().get(GradsGridSubsetter.this,
						    info.getDODSName() + 
						    "." + variable,
						    ".subset");

	    GradsDataInfo info = (GradsDataInfo)data.getToolInfo();
	    Task task = tool.getTask().task(info.getGradsBinaryType(),
					    "subset", new String[] {
						subsetFile.getAbsolutePath(),
						info.getGradsArgument(),
						variable,
						bounds.toGradsString()
					    });
	    
	    try {
		task.run();
	    } catch (AnagramException ae) {
		if (debug()) debug("output from failed script: \n" + 
				   task.getOutput());
		throw new ModuleException(GradsGridSubsetter.this,
					  "subset operation failed", 
					  ae);
	    }

	    if (!subsetFile.exists()) {
		throw new ModuleException
		    (GradsGridSubsetter.this, 
		     "subset script produced no output for " + 
		     variable + " in " + data);
	    }

	    return subsetFile;
	}

	/** Returns the size of the in-memory buffer to be used when
	 * streaming subsets from disk. */
	public int getBufferSize() {
	    return bufferSize;
	}

	protected GradsDataInfo info;
	protected DataHandle data;

    }

    /** A one-time-use class which serializes a particular data
     * variable, with a particular set of bounds, to a particular
     * stream.  The data related to a given serializing operation has
     * to be stored separately from the Module object, which may be
     * handling many operations simultaneously.
     */
    protected class Serializer {
	/** Creates a new Serializer for the variable and stream given. 
	 * @param var Variable to serialize (assumed to have constraints set)
	 * @param out Stream to serialize variable to 
	 * @param useASCII If true, print ASCII text; if false, send
	 * DODS/3.2 binary stream.
	 */
	protected Serializer(DArray var, 
			     OutputStream out, 
			     boolean sendASCII,
			     DataMethods data,
			     long limit) 
	    throws ModuleException {

	    this.sendASCII = sendASCII;
	    this.data = data;
	    this.name = var.getName();

	    if (sendASCII) {
		this.p = new PrintStream(out);
	    } else {
		this.out = new DataOutputStream(out);
	    }

	    // check if we are serializing coordinate or data variable
	    // coordinate vars are double-precision
	    // data vars are single-precision
	    isSubset = (var.numDimensions() > 1);
	    if (isSubset) {
		valueSize = FLOAT_SIZE;
		openSubsetInput(var, data);

	    } else {
		valueSize = DOUBLE_SIZE;
		openDimInput(var, data);
	    }


	    buildDimList(var);

	    if (limit > 0 && 
		(totalOutputSize * valueSize) > limit) {
		throw new ModuleException(GradsGridSubsetter.this,
					  "subset exceeds limit of " + 
					  limit + " bytes");
	    }

	    if (debug()) {
		debug("serializing " + var.getName() + 
		      " as " + ((sendASCII) ? "ASCII" : "binary") +
		      "; value size: " + valueSize +
		      "; in: " + totalInputSize +
		      "; out: " + totalOutputSize);
		for (int i = 0; i < dims.length; i++) {
		    debug("dim " + i + ": " + dims[i]);
		}
	    }
	}	

	/** Prepares an InputStream containing the data to be streamed 
	    for dimension data.*/
	protected void openDimInput(DArray var, DataMethods data) 
	    throws ModuleException {
	    try {
		this.in = new DataInputStream
		    (new BufferedInputStream
			(new FileInputStream
			    (data.getDimData(var.getName()))));
		in.skip(var.getDimension(0).getStart() * valueSize);
	    } catch (IOException ioe) {
		throw new ModuleException
		    (GradsGridSubsetter.this,
		     "io error opening dim data for subset", ioe);
	    } catch (InvalidParameterException ipe) {
		throw new RuntimeException(ipe.getMessage());
		// decided this is a "shouldn't happen" that should get 
		// passed up to the top-level error handler
	    }
	}
	/** Prepares an InputStream containing the data to be streamed
	    for subset data. Generates temporary subset file if
	    necessary. */
	protected void openSubsetInput(DArray var, DataMethods data) 
	    throws ModuleException {

	    try {
		// data variable
		if (data.info.isDirectSubset()) {
		    if (debug()) debug("opening " + 
				       data.info.getSourceFile() + 
				       " for direct subsetting");
			this.inputFile = data.info.getSourceFile();
		} else {
		    Bounds.Grid bounds = 
			((GradsArray)var).calculateBounds();
		    this.inputFile = 
			data.getSubset(var.getName(), bounds);
		}
		
		this.in = new DataInputStream
		    (new BufferedInputStream
		     (new FileInputStream
		      (inputFile)));
		
	    } catch (IOException ioe) {
		    inputFile.delete();
		    throw new ModuleException
			(GradsGridSubsetter.this,
			 "io error opening subset data", ioe);
	    }
	    
	}

	/** Pre-calculates various statistics for the subset grid
	 *  which will be used to support ASCII formatting and/or
	 *  subsampling during the streaming operation
	 */
	protected void buildDimList(DArray var) 
	    throws ModuleException {

	    dims = new Dim[var.numDimensions()];
	    totalInputSize = totalOutputSize = 1;
	    for (int i = dims.length - 1; i >= 0; i--) {
		try {
		    DArrayDimension dodsDim = var.getDimension(i);
		    int rowSize = 1;
		    if (i < dims.length - 1) {
			rowSize = dims[i+1].rowSize * dims[i+1].inputSize;
		    }
		    dims[i] = new Dim(dodsDim, rowSize);
		    totalInputSize *= dims[i].inputSize;
		    totalOutputSize *= dims[i].outputSize;
		} catch (InvalidParameterException ipe) {
		    throw new ModuleException(GradsGridSubsetter.this,
					      "couldn't look up dimension", 
					      ipe);
		}
	    }
	}
	
	/** Writes data from a properly prepared InputStream to the 
	 *  OutputStream associated with this Serializer 
	 */ 
	protected void serialize()
	    throws ModuleException {
	    try {
		writeHeader();	    

		if (!sendASCII && isSubset && data.info.isDirectSubset()) {
		    // special handler to extract the subset 
		    // directly from the datafile, with no intermediate
		    // temporary file
		    writeDirectSubset();

		} else if (!sendASCII && totalInputSize == totalOutputSize) {
		    // the absolute simplest case - binary data, no 
		    // subsampling, subset already extracted from dataset
		    // by GrADS - just write the input stream straight to the
		    // output stream
		    byte[] buffer = new byte[bufferSize];

		    if (debug()) debug("spooling directly");
		    Spooler.spool(totalOutputSize * valueSize, 
				  in, 
				  out, 
				  buffer);
		} else {
		    // in all other cases, data has to be parsed as it's 
		    // read in, and formatted or subsampled data written to 
		    // the output stream 
		    writeProjectedArray(0);
		}
		in.close();

	    } catch (IOException ioe) {
		throw new ModuleException(GradsGridSubsetter.this,
					  "io error during data send: " +
					  ioe.getClass() + ": " + 
					  ioe.getMessage());
	    } finally {
		// clean up temporary subset file
		if (isSubset && !data.info.isDirectSubset()) {
		    inputFile.delete();
		}
	    }
	}

	/** Writes any initial data needed before the actual data streaming */
	protected void writeHeader()
	    throws IOException {

	    if (sendASCII) {
		// print variable name and dimension sizes at the top of 
		// the ASCII output
		p.print(name);
		p.print(", ");
		for (int i = 0; i < dims.length; i++) {
		    p.print("[");
		    p.print(dims[i].outputSize);
		    p.print("]");
		}
		p.println();
	    } else {
		// DODS/3.2 sends arrays in XDR format - 
		// four-byte length followed by data values.  Because
		// both XDR and DODS libraries read the length on the
		// client end, we must write it twice.
		out.writeInt(totalOutputSize);
		out.writeInt(totalOutputSize);
	    }
	}

	/** Recursively writes out a multi-dimensional array with
	 *  strides, in either ASCII or binary format 
	 */
	protected void writeProjectedArray(int i) 
	    throws IOException {

	    Dim dim = dims[i];
	    
	    // Print indices before each row of ASCII output
	    if (sendASCII && dims.length > 1 && i == dims.length - 1) {
		for (int j = 0; j < i; j++) {
		    p.print("[");
		    p.print(dims[j].pos);
		    p.print("]");
		}
		p.print(", ");
	    }

	    // Loop on output values
	    for (dim.pos = 0; dim.pos < dim.outputSize; dim.pos++) {
		// Recurse unless this is the last dimension
		if (i == dims.length - 1) {
		    writeValue(dim);
		} else {
		    writeProjectedArray(i + 1);
		}

		// Number of data values to skip (could I just calculate this once in 
		// the beginning?)
		int skipValues = dim.rowSize * (dim.stride - 1);

		// Number of data values left before this row is over
		int leftInRow = dim.rowSize * 
		    ((dim.inputSize - 1) - (dim.pos * dim.stride));

		// If a full skip would take us past the end of the row, then
		// only skip whatever's left in the row
		if (skipValues > leftInRow) {
		    if (debug()) debug("dim " + i + ": finishing row: " + 
				       leftInRow + " values");
		    skipValues = leftInRow;
		} else {
		    if (debug()) debug("dim " + i + ": skipping " + skipValues + 
				       " values");
		}

		int bytesLeftToSkip = skipValues * valueSize;
		while (bytesLeftToSkip > 0) {
		    bytesLeftToSkip -= in.skip(bytesLeftToSkip);
		}
	    }

	    if (sendASCII) {
		p.println();
	    }
	}

	/** Writes a subset in binary format, directly the original
	 *  datafile.  This avoids the need to invoke GrADS and create
	 *  a temporary subset file, which markedly improves
	 *  performance. However, the datafile must fit a very
	 *  specific set of characterisitics (see documentation for
	 *  'direct_subset' in the &lt;dataset&gt; tag). A lot of 
	 * helper functions in the GradsDataInfo.CTL object are used.
	 * @see org.iges.grads.server.GradsDataInfo.CTL
	 */
	protected void writeDirectSubset() 
	    throws IOException {

	    if (debug()) debug("reading subset data directly...");
	    GradsDataInfo.CTL ctl = data.info.getCTL();
	    if (debug()) 
		if (data.info.hasLevels()) debug("variable has a Z dimension");
	    if (debug()) 
		if (data.info.hasEnsemble()) debug("variable has an E dimension");
	    
	    // we add up all the z-levels for all the variables so we
	    // can easily skip to the variable we're interested in
	    int varIndex = ctl.getVarIndex(name);
	    long zSumUpToVar = ctl.getLevelsUpTo(varIndex);
	    
	    // offsets in dims array depend on whether variable is 
	    // Z-varying or E-varying, so take care of that outside
	    // the for loop
	    int eStart, eStop, eStride;
	    int zStart, zStop, zStride;
	    Dim eDim, tDim, yDim, xDim; 
	    if (data.info.hasEnsemble() & data.info.hasLevels()) {
		// variable has E and Z 
		eStart  = dims[0].start;
		eStop   = dims[0].stop;
		eStride = dims[0].stride;
		tDim = dims[1];
		zStart  = dims[2].start;
		zStop   = dims[2].stop;
		zStride = dims[2].stride;
		yDim = dims[3];
		xDim = dims[4];
	    }
	    else if (data.info.hasEnsemble() & !data.info.hasLevels()) {
		// variable has E but no Z
		eStart  = dims[0].start;
		eStop   = dims[0].stop;
		eStride = dims[0].stride;
		tDim = dims[1];
		zStart  = 0;
		zStop   = 0;
		zStride = 1;
		yDim = dims[3];
		xDim = dims[4];
	    }
	    else if (!data.info.hasEnsemble() & data.info.hasLevels()) {
		// variable has Z but no E
		eStart  = 0;
		eStop   = 0;
		eStride = 1;
		tDim = dims[0];
		zStart  = dims[1].start;
		zStop   = dims[1].stop;
		zStride = dims[1].stride;
		yDim = dims[2];
		xDim = dims[3];
	    } else {
		// variable has no Z and no E
		eStart  = 0;
		eStop   = 0;
		eStride = 1;
		tDim = dims[0];
		zStart  = 0;
		zStop   = 0;
		zStride = 1;
		yDim = dims[2];
		xDim = dims[3];
	    }

	    long fileOffset = 0, oldFileOffset = 0;
	    long skipBytes;
	    
	    if (xDim.stride > 1) {
		// can only read and write one value at a time if x dimension 
		// needs subsampling
		for (int e = eStart; e <= eStop; e += eStride) {
		    for (int t = tDim.start; t <= tDim.stop; t += tDim.stride) {
			for (int z = zStart; z <= zStop; z += zStride) {
			    for (int y = yDim.start; y <= yDim.stop; y += yDim.stride) {
				for (int x = xDim.start; x <= xDim.stop; x += xDim.stride) {

				    fileOffset = (e * ctl.xyztSize) + 
					(t * ctl.xyzSize) + 
					((zSumUpToVar + z) * ctl.xySize) + 
					(y * ctl.xSize) + 
					x;
				    skipBytes = (fileOffset - oldFileOffset) * FLOAT_SIZE;
				    skipFully(in, skipBytes);
				    out.writeFloat(in.readFloat());
				    oldFileOffset = fileOffset + 1; 
				}
			    }
			}
		    }
		}
	    } else {
		// if x dimension doesn't need subsampling, we can read 
		// and write a whole row at a time using a buffer
		byte[] buf = new byte[xDim.inputSize * FLOAT_SIZE];

		for (int e = eStart; e <= eStop; e += eStride) {
		    for (int t = tDim.start; t <= tDim.stop; t += tDim.stride) {
			for (int z = zStart; z <= zStop; z += zStride) {
			    for (int y = yDim.start; y <= yDim.stop; y += yDim.stride) {
				
				fileOffset = ((long)e * ctl.xyztSize) + 
				    ((long)t * ctl.xyzSize) + 
				    ((long)(zSumUpToVar + z) * ctl.xySize) + 
				    ((long)y * ctl.xSize) + 
				    (long)xDim.start;
				if (debug()) debug("y=" + y + 
						   " z=" + z + 
						   " t=" + t + 
						   " e=" + e + 
						   " pos=" + fileOffset +
						   " buf=" + xDim.inputSize);
				skipBytes = (fileOffset - oldFileOffset) * FLOAT_SIZE;
				skipFully(in, skipBytes);
				Spooler.spool(buf.length, in, out, buf);
				oldFileOffset = fileOffset + xDim.inputSize; 
			    }
			}
		    }
		}
	    }	
	}
    

	/** Wrapper for the InputStream.skip() method, that keeps
	 * calling skip() until the requested number of bytes are in
	 * fact skipped.
	 */
	protected void skipFully(InputStream in, long skipBytes) 
	    throws IOException {

	    long skipped = 0;
	    while (skipped < skipBytes) {
		skipped += in.skip(skipBytes - skipped);
	    }
	}


	/** Writes a single data value, in ASCII or binary form as appropriate. */
	protected void writeValue(Dim dim)
	    throws IOException {

	    if (debug()) debug("writing value");
	    if (sendASCII) {
		if (dim.pos > 0) {
		    p.print(", ");
		}
		if (isSubset) {
		    p.print(in.readFloat());
		} else {
		    p.print(in.readDouble());
		}
	    } else {
		if (isSubset) {
		    out.writeFloat(in.readFloat());
		} else {
		    out.writeDouble(in.readDouble());
		}
	    }
	}

	/** Used for calculating file and buffer sizes */ 
	protected static final int FLOAT_SIZE = 4;
	/** Used for calculating file and buffer sizes */ 
	protected static final int DOUBLE_SIZE = 8;
	
	protected DataMethods data;
	protected String name;
	protected File inputFile;
	protected PrintStream p;
	protected DataInputStream in;
	protected DataOutputStream out;
	protected Dim[] dims;
	protected int totalInputSize;
	protected int totalOutputSize;
	protected boolean sendASCII;
	protected int valueSize;
	protected boolean isSubset;

	/** Keeps track of useful numbers for stride calculations */
	protected class Dim {
	    protected Dim(DArrayDimension dodsDim, int rowSize) {
		this.name = dodsDim.getName();
		this.start = dodsDim.getStart();
		this.stop = dodsDim.getStop();
		this.stride = dodsDim.getStride();
		this.inputSize = stop - start + 1;
		this.outputSize = ((inputSize - 1) / stride) + 1;
		this.rowSize = rowSize;
	    }

	    /** Used for "direct subsetting" feature */
	    String name;
	    int start;
	    int stop;

	    /** For stride = n, we send only every nth entry of this dimension */
	    int stride;

	    /** Number of entries in the dimension before striding */
	    int inputSize;

	    /** Number of entries in the dimension after striding */ 
	    int outputSize;

	    /** Number of values contained in each entry of this dimension,
	     *  speaking in recursive terms.
	     *  I.e. if outputSize for dim 0 is 5, then rowSize for dim 1 is 5. 
	     */
	    int rowSize;

	    /** Marks current position during streaming */
	    int pos;
	    
	    public String toString() {
		return "stride: " + stride +
		    " in: " + inputSize +
		    " out: " + outputSize + 
		    " row: " + rowSize;
	    }
	}
    }
}
