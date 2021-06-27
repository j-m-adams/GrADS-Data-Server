/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.server.dap;

import dods.dap.Server.*;
import dods.dap.*;
import java.io.*;
import java.util.*;

import org.iges.anagram.AnagramException;

/** An implementation of the DODS Sequence data type. 
 * @see org.iges.grads.server.GradsStnSubsetter
 */
public class GradsSequence
    extends SDSequence 
    implements GradsServerMethods {

    public final static int START_OF_INSTANCE = 0x5a000000;
    public final static int END_OF_SEQUENCE = 0xa5000000;

    public final static boolean NO_MORE_DATA = true; 
    public final static boolean GOT_DATA = false; 

    
    /** Constructs a new GradsSequence. */
    public GradsSequence() { 
	super(); 
    }
    
    /**
     * Constructs a new GradsSequence with name n.
     * @param n the name of the variable.
     */
    public GradsSequence(String n) { 
	super(n); 
    }

    /** Dummy implementation */
    public boolean read(String datasetName, Object specialO) {
	return true;
    }

    /** Recursively tries to read the next row from the subsequence.
     * If the current sequence hits EOS, NO_MORE_DATA is returned.
     * If the subsequence returns NO_MORE_DATA, a new row for this 
     * sequence is read, and then the subsequence is read again. 
     * Rows containing empty subsequences will be skipped.
     * If GOT_DATA is returned, there is valid data for this sequence
     * and all subsequences, which differs from the data stored
     * previous to the call.
     * Used by sendASCII(). 
     */
    private boolean readRow(DataInputStream records)
	throws IOException, EOFException, AnagramException {
	
	// bottoming out of recursion 
	if (subSequence == null) {
	    return readVars(records);
	}

	// recursion
	if (newSequence) { // first time through
	    newSequence = false;
	    return readCompleteRow(records);
	} else { // from then on
	    // we should already have data for this level, see 
	    // if we can get more data for the subsequence
	    if (subSequence.readRow(records) == GOT_DATA) {
		return GOT_DATA;
	    } else {
		System.err.println("newRecord = true");
		return readCompleteRow(records);
	    }
	}
    }
 
    /** Reads a new value for all variables in this sequence and its
     * subsequences. Returns NO_MORE_DATA if there are no more
     * rows of this sequence with valid (non-empty) subsequences.
     */
    private boolean readCompleteRow(DataInputStream records)
	throws IOException, EOFException, AnagramException {
	// Keep reading new rows until the subsequence call is successful
	while (readVars(records) == GOT_DATA) {
	    // If there is a subsequence, recursively read its initial row
	    if (subSequence == null || 
		subSequence.readCompleteRow(records) == GOT_DATA) {
		return GOT_DATA;
	    }
	}
	return NO_MORE_DATA;
    }
    
    /** Deserializes data for each variable in the current sequence
     * (but not any subsequences) using the stream given.
     * Returns NO_MORE_DATA if an EOS marker is encountered.
     */
    public boolean readVars(DataInputStream records)
	throws IOException, EOFException {
	//	System.err.print("reading [ ");
	ServerVersion version = new ServerVersion("3.1");

	try {
	    int header = records.readInt();
	    if (header == END_OF_SEQUENCE) {
		//		System.err.println("EOS ]");
		return NO_MORE_DATA;
	    } 
	    if (header != START_OF_INSTANCE) {
		throw new IOException("expected record start");
	    }
	    // Read values into all variables at this level
	    for (int i = 0; i < projectedVars.size(); i++) {
		BaseType var = (BaseType)projectedVars.get(i);
		//		System.err.print(var.getName() + " ");
		((ClientIO)var).deserialize(records, 
					    version, 
					    null);
	    }
	} catch (DataReadException dre) {
	    throw new IOException(dre.getMessage());
	}
	//	System.err.println("]");
	return GOT_DATA;
    }


  /** Writes the entire contents of the Sequence to the 
   *  output stream provided, in either binary or ASCII format. 
   */
    public void serialize(String datasetName, 
			  DataOutputStream sink,
			  CEEvaluator ce,
			  Object specialO,
			  boolean useASCII) 
	throws AnagramException {
       
	// If the variable was not part of the constraint expression,
	// don't send it.  
	if (!isProject()) {
	    return;
	}

	// Set up various convenience and state variables used
	// by the recursive send methods
	initSend();

	try {
	    // Open input stream
	    DataInputStream records =
		new DataInputStream
		    (new BufferedInputStream
			(new FileInputStream
			    ((File)specialO)));

	    // Send
	    if (useASCII) {
		sendASCII(datasetName, sink, ce, records);
	    } else {
		sendBinary(datasetName, sink, ce, records);
	    }


	} catch (SDODSException sdods) {
	    throw new AnagramException("dods message: " + sdods.getMessage());
	} catch (NoSuchVariableException nsve) {
	    throw new AnagramException("dods message: " + nsve.getMessage());
	} catch (FileNotFoundException fnfe) {
	} catch (EOFException e) {
	    throw new AnagramException("unexpected eof in subset data");
	} catch (IOException ioe) {
	    throw new AnagramException("io problems while reading subset; " +
				     "message: " + ioe.getMessage());
	}
    }

    /** Read values from the stream given, and write those
     *  that pass the constraint expression to the stream given,
     *  in ASCII format.
     */
    private void sendASCII(String datasetName, 
			   DataOutputStream sink,
			   CEEvaluator ce, 
			   DataInputStream records)
	throws AnagramException, IOException, SDODSException, 
	       NoSuchVariableException {

	int counter = 0;
	PrintWriter w = new PrintWriter(new OutputStreamWriter(sink));
	// Write column names
	printASCIIHeadings(w);

	// Read new data
	while (readRow(records) == GOT_DATA) {
	    counter++;
	    System.err.print("record # " + counter + " ");
	    // Check that data satisfies constraint
	    if (ce.evalClauses(null)) {
		// Send
		printASCIIRow(w);
		System.err.println("sent");
	    } else {
		System.err.println("skipped");
	    }
	}
	w.flush();
    }


    /** Recursively prints the names of each variable in this sequence and its
     *  subsequences, separated by commas and followed by a CR
     */
    private void printASCIIHeadings(PrintWriter w) {

	for (int i = 0; i < projectedVars.size(); i++) {
	    // print variable name
	    BaseType var = (BaseType)projectedVars.get(i);
	    w.print(var.getName());
	    // punctuation
	    if (i == projectedVars.size() - 1 && 
		subSequence == null) {
		w.println(); // finished
	    } else {
		w.print(", ");
	    } 
	}
	if (subSequence != null) {
	    subSequence.printASCIIHeadings(w); // recurse
	}
    }


    /** Recursively prints the current value of each variable in this 
     *  sequence and its subsequences, separated by commas and followed by a CR
     */
    private void printASCIIRow(PrintWriter w)
	throws IOException, SDODSException, NoSuchVariableException {
	
	for (int i = 0; i < projectedVars.size(); i++) {
	    // print value
	    BaseType var = (BaseType)projectedVars.get(i);
	    var.printVal(w, "", false);
	    // punctuation
	    if (i == projectedVars.size() - 1 && 
		subSequence == null) {
		w.println(); // finished
	    } else {
		w.print(", ");
	    } 
	}
	if (subSequence != null) {
	    subSequence.printASCIIRow(w); // recurse
	}
    }

    /** Recursively read values from the stream given, and write those
     *  that pass the constraint expression to the stream given,
     *  in binary format.
     */
    private void sendBinary(String datasetName,
			    DataOutputStream sink, 
			    CEEvaluator ce,
			    DataInputStream records)
	throws IOException, SDODSException, NoSuchVariableException {

	empty = true;
	// Read data for this level
	while (readVars(records) == GOT_DATA) {
	    // Wait until some subsequence data passes constraint before
	    // writing data for this level. Just flag that it hasn't
	    // been done yet.
	    written = false;
	    if (subSequence != null) {
		// Get subsequence data
		subSequence.sendBinary(datasetName, sink, ce, records);
	    } else {
		// We are at the bottom level, so we have a complete
		// row of the sequence. Evaluate constraint, and
		// send data if it passes.
		if (ce.evalClauses(null)) {
		    writeVars(datasetName, sink, ce);
		}
	    }
	}
	if (!empty) {
	    // Finished with this level
	    System.err.println("EOS");
	    sink.writeInt(END_OF_SEQUENCE);
	}
    }

    /** Writes data for all variables in the current level, first calling
     *  writeVars() for its supersequence if there is one and it hasn't
     *  written its data. Used by sendBinary().
     */
    private void writeVars(String datasetName,
			   DataOutputStream sink, 
			   CEEvaluator ce)
	throws IOException, SDODSException, NoSuchVariableException {

	// Check if supersequences have written their data yet
	// i.e., is this the first row of data that passed the constraint
	if (superSequence != null && !superSequence.written) {
	    superSequence.writeVars(datasetName, sink, ce);
	}
	
	// Write out a sequence row.
	System.err.print("SOI ");
	sink.writeInt(START_OF_INSTANCE);
	for (int i = 0; i < projectedVars.size(); i++) {
	    Object var = projectedVars.get(i);
	    System.err.print(((BaseType)var).getName() + " ");
	    ((ServerMethods)var).serialize(datasetName, sink, ce, null);
	}
	System.err.println();
	// Flag that data has been written for the current row of this level
	written = true;
	empty = false;
    }
  

    /** Recursively sets up convenience and state variables used by the 
     *  other recursive methods.
     */
    private void initSend() {
	// Flag special behavior for readRow()
	newSequence = true;

	// Sort out the vars that are projected for this transmission.
	// Don't deal with the last one yet, since it might be a sequence.
	projectedVars = new ArrayList();
	for (int i = 0; i < varTemplate.size() - 1; i ++) {
	    Object current = varTemplate.elementAt(i);
	    if (((ServerMethods)current).isProject()) {
		projectedVars.add(current);
	    }
	}
	// If the last variable is normal, add it to projectedVars.
	// If it is a subsequence, set up the subSequence and superSequence
	// members, and recursively initialize the subsequence.
	Object last = varTemplate.lastElement();
	if (((ServerMethods)last).isProject()) {
	    if (last instanceof GradsSequence) {
		subSequence = ((GradsSequence)last);
		subSequence.superSequence = this;
		subSequence.initSend();
	    } else {
		projectedVars.add(last); // bottom out recursion
		subSequence = null;
	    }
	}
    }	

    /** Used during transmission. Lists all the non-sequence variables
     *  that are projected by the current CE.
     */
    private List projectedVars;

    /** Flag set for readRow() the first time it is called. */
    private boolean newSequence;

    /** Flag for writeVars() indicated whether or not this level contains
     *  unwritten data. 
     */
    private boolean empty;
    private boolean written;

    /** The subsequence of this sequence, if it exists and is projected. */
    private GradsSequence subSequence;

    /** The supersequence of this sequence, if any. */
    private GradsSequence superSequence;

}
