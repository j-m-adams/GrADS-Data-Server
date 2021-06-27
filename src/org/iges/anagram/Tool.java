/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram;

import java.io.*;

import dods.dap.*;
import dods.dap.Server.*;

/** A tool for accessing and analyzing data.<p>
 *
 *  This class encapsulates all operations that are specific to  
 *  the particular data format and/or access mechanism being served
 *  by the Anagram server.
 *  
 *  Thus, implementing the Anagram framework simply means
 *  implementing this class, plus possibly TempDataHandle 
 *  (see doAnalysis() and doUpload()).
 *  
 */
public abstract class Tool 
    extends AbstractModule {

    /** Creates usable handles for data objects specified by tags in
     *  the configuration file.<p> If possible, the import method
     *  should skip data objects that are already loaded, and whose
     *  attributes have not changed. <p>
     *
     *  This method does not need to be threadsafe.
     *
     *	@param setting The tag tree specifying the data objects. This will be 
     *  the tree rooted at the <data> subtag of the <catalog> tag in the 
     *  configuration file.
     *  The combination of tag names and attributes to be used for specifying 
     *  data objects is up to the implementor of this method. <p>
     */
    public abstract DataHandle[] doImport(Setting setting);


    /** Performs an analysis task.<p>
     *  If analysis is not supported, this method
     *  should throw an exception. <p>
     *
     * This method must be threadsafe.<p>
     *
     *
     * @param ae The analysis expression to be evaluated. The format
     *  of this expression is up to the implementor of this method.
     *
     * @return A handle to the results of the analysis. TempDataHandle
     *  is an abstract interface. Thus, this method is responsible for
     *  supplying an object which implements TempDataHandle properly.
     *
     * @throws ModuleException If the analysis fails for any reason
     */
    public abstract TempDataHandle doAnalysis(String name,
					      String ae, 
					      Privilege privilege) 
	throws ModuleException;

    /** Accepts an uploaded data object.<p>
     *  If uploads are not supported, this method
     *  should throw an exception. <p>
     *
     * This method must be threadsafe.<p>
     *
     * @param input The stream of data to be stored

     * @return A handle to the uploaded data object. TempDataHandle
     *  is an abstract interface. Thus, this method is responsible for
     *  supplying an object which implements TempDataHandle properly.
     *
     * @throws ModuleException if the upload fails for any reason
     */
    public abstract TempDataHandle doUpload(String name,
					    InputStream input,
					    long size,
					    Privilege privilege)
	throws ModuleException;

    /** Brings the data handle provided up to date with respect to the 
     *  data source. <p>
     * 
     * This method must be threadsafe.<p>
     *
     * @throws ModuleException If the data source has become unusable.
     * @return True if the data handle was modified. 
     */
    public abstract boolean doUpdate(DataHandle data) 
	throws ModuleException;

    /** Provides an object representation of the DODS Data Descriptor
     *  Structure for the specified data object.
     *
     * It is guaranteed that the calling thread will already have a
     * non-exclusive lock on the <code>data</code> parameter before
     * this method is called. Other than that, this method must
     * guarantee its own thread-safety. <p>
     * @see Handle#getSynch
     *
     * @param data The data object to be accessed
     * @param ce The DODS constraint to be applied to the DDS. Null
     * indicates that the DDS should not be constrained.
     * @return an object representing the DDS 
     * @throws ModuleException if the request fails for any reason
     */
    public abstract ServerDDS getDDS(DataHandle data, String ce)
	throws ModuleException;
	

    /** Provides an object representation of the DODS Data Attribute
     *  Structure for the specified data object.
     *
     * It is guaranteed that the calling thread will already have a
     * non-exclusive lock on the <code>data</code> parameter before
     * this method is called. Other than that, this method must
     * guarantee its own thread-safety. <p>
     *
     * @see Handle#getSynch
     *
     * @param data The data object to be accessed
     * @return an object representing the DAS 
     * @throws ModuleException if the request fails for any reason
     */
    public abstract DAS getDAS(DataHandle data)
	throws ModuleException;

    /** Writes the DODS Data Descriptor Structure for 
     *  the specified data object to the specified stream.
     *
     * It is guaranteed that the calling thread will already have a
     * non-exclusive lock on the <code>data</code> parameter before
     * this method is called. Other than that, this method must
     * guarantee its own thread-safety. <p>
     *
     * This method has a default implementation, which creates a DDS object
     * using getDDS(), and serializes it to the stream. For optimal performance
     * it is recommended to override this default implementation with a faster
     * approach, such as streaming the DDS text directly from a cached disk 
     * file.
     *
     * @see Handle#getSynch
     *
     * @param data The data object to be accessed
     * @param ce The DODS constraint to be applied to the DDS. Null
     * indicates that the DDS should not be constrained.
     *  @param out A stream to which to write the DDS 
     * @throws ModuleException if the request fails for any reason
     */
    public void writeDDS(DataHandle data, String ce, OutputStream out)
	throws ModuleException {
	
	ServerDDS dds = getDDS(data, ce);
	dds.print(out);

    }

    /** Writes the DODS Data Attribute Structure for 
     *  the specified data object to the specified stream.
     *
     * It is guaranteed that the calling thread will already have a
     * non-exclusive lock on the <code>data</code> parameter before
     * this method is called. Other than that, this method must
     * guarantee its own thread-safety. <p>
     *
     * This method has a default implementation, which creates a DAS object
     * using getDAS(), and serializes it to the stream. For optimal performance
     * it is recommended to override this default implementation with a faster
     * approach, such as streaming the DAS text directly from a cached disk 
     * file.
     * @see Handle#getSynch
     *
     * @param data The data object to be accessed
     *  @param out A stream to which to write the DAS 
     * @throws ModuleException if the request fails for any reason
     */
    public void writeDAS(DataHandle data, OutputStream out)
	throws ModuleException {

	DAS das = getDAS(data);
	das.print(out);

    }

    /** Writes a customized summary of the dataset, in the form of an
     *  HTML fragment, to the specified stream.
     *
     * It is guaranteed that the calling thread will already have a
     * non-exclusive lock on the <code>data</code> parameter before
     * this method is called. Other than that, this method must
     * guarantee its own thread-safety. <p>
     *
     * @see Handle#getSynch
     *
     * @param data The data object to be accessed
     *  @param out A stream to which to write the DAS 
     * @throws ModuleException if the request fails for any reason
     */
    public abstract void writeWebInfo(DataHandle data, OutputStream out)
	throws ModuleException;

    
    /** Writes customized THREDDS metadata for the dataset, in the
     *  form of an XML fragment, to the specified stream.
     *
     * It is guaranteed that the calling thread will already have a
     * non-exclusive lock on the <code>data</code> parameter before
     * this method is called. Other than that, this method must
     * guarantee its own thread-safety. <p>
     *
     * @see Handle#getSynch
     *
     * @param data The data object to be accessed
     *  @param out A stream to which to write the DAS 
     * @throws ModuleException if the request fails for any reason
     */
    public abstract void writeTHREDDSTag(DataHandle data, OutputStream out)
	throws ModuleException;

    /** Writes a subset of the specified data object to the specified stream,
     *  in DODS binary format.<p>
     *  
     * It is guaranteed that the calling thread will already have a
     * non-exclusive lock on the <code>data</code> parameter before
     * this method is called. Other than that, this method must
     * guarantee its own thread-safety. <p>
     *
     * @see Handle#getSynch
     *
     * @param data The data object to be accessed
     *  @param ce The DODS constraint expression specifying the subset
     *  to be sent
     *  @param out The stream to which to write the subset
     * @throws ModuleException if the request fails for any reason
     */
    public abstract void writeBinaryData(DataHandle data, 
					 String ce, 
					 Privilege privilege,
					 OutputStream out)
	throws ModuleException;

    /** Writes a data subset to a stream as a text table.<p>
     *
     * It is guaranteed that the calling thread will already have a
     * non-exclusive lock on the <code>data</code> parameter before
     * this method is called. Other than that, this method must
     * guarantee its own thread-safety. <p>
     *
     * @see Handle#getSynch
     *
     * @param data The data object to be accessed
     *  @param ce The DODS constraint expression specifying the subset
     *  to be sent
     *  @param out The stream to which to write the subset
     * @throws ModuleException if the request fails for any reason
     */
    public abstract void writeASCIIData(DataHandle data, 
					String ce, 
					Privilege privilege,
					OutputStream out)
	throws ModuleException;



}
