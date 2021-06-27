/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.util;

import java.io.*;

/** A collection of methods that efficiently 
 *  spool data from a finite-length source to a sink. 
 */ 
public class Spooler {

    /** Spools character data from a Reader to a Writer 
     *  until the Reader is exhausted. <p>
     *  Warning: if called on a stream with unlimited length 
     *  (such as a network socket) this method will not return until the
     *  stream is closed.
     *  A buffer of 1024 characters is used for read and write operations.
     * @throws IOException If an I/O error occurs.
     */
    public static long spool(Reader r, Writer w) 
	throws IOException {
	return spool(r, w, new char[1024]);
    }

    /** Spools character data from a Reader to a Writer
     *  until the Reader is exhausted.<p>
     *  Warning: if called on a stream with unlimited length 
     *  (such as a network socket) this method will not return until the
     *  stream is closed.
     * @param bufferSize The buffer size to use for read and write operations.
     * @throws IOException If an I/O error occurs.
     */
    public static long spool(Reader r, Writer w, char[] buf) 
	throws IOException {
	
	// Allocate a buffer to read bytes into and out of
	// char[] buf = new char[bufferSize];

	// Write data
	int bytesRead = 0;
	long totalBytesRead = 0;

	while (true) {
	    // Read the next chunk
	    bytesRead = r.read(buf);
	    // Check for eof
	    if (bytesRead < 0) {
		break;
	    }
	    // Write it out
	    w.write(buf, 0, bytesRead);
	    totalBytesRead += bytesRead;
	}
	return totalBytesRead;
    }


    /** Spools byte data from an InputStream to an OutputStream
     *  until the InputStream is exhausted.<p>
     * A buffer of 1024 bytes is used for read and write operations.<p>
     *  Warning: if called on a stream with unlimited length 
     *  (such as a network socket) this method will not return until the
     *  stream is closed.
     * @throws IOException If an I/O error occurs.
     */
    public static long spool(InputStream in, OutputStream out) 
	throws IOException {
	return spool(in, out, new byte[1024]);
    }

    /** Spools byte data from an InputStream to an OutputStream
     *  until the InputStream is exhausted.<p>
     *  Warning: if called on a stream with unlimited length 
     *  (such as a network socket) this method will not return until the
     *  stream is closed.
     * @param bufferSize The buffer size to use for read and write operations.
     * @throws IOException If an I/O error occurs.
     * @throws IllegalArgumentException If bufferSize is <= 0.
     */
    public static long spool(InputStream in, 
			     OutputStream out, 
			     byte[] buf) 
	throws IOException {

	// Allocate a buffer to read bytes into and out of
	// byte[] buf = new byte[bufferSize];

	// Write data
	int bytesRead = 0;
	long totalBytesWritten = 0;

	while (true) {
	    // Read the next chunk
	    if (DEBUG) System.err.print("reading...");
	    bytesRead = in.read(buf);
	    // Check for eof
	    if (bytesRead < 0) {
	      if (DEBUG) System.err.println("eof");
		break;
	    }
	    if (DEBUG) System.err.print("got " + bytesRead + 
					" bytes. writing...");
	    out.write(buf, 0, bytesRead);
	    if (DEBUG) System.err.println("done.");
	    totalBytesWritten += bytesRead;
	}
	return totalBytesWritten;
    }

    /** Spools a fixed quantity of byte data from an 
     *  InputStream to an OutputStream.
     * @param totalBytesToWrite The number of bytes that should be read. 
     * @param bufferSize The buffer size to use for read and write operations.
     * @throws IOException If an I/O error occurs, or the InputStream returns
     * EOF before <code>totalBytesToWrite</code> bytes have been read.
     */
    public static long spool(long totalBytesToWrite, 
			     InputStream in, 
			     OutputStream out, 
			     byte[] buf) 
	throws IOException {
	
	// Allocate a buffer to read bytes into and out of
	// byte[] buf = new byte[bufferSize];

	// Write data
	int bytesRead = 0;
	long totalBytesWritten = 0;

	while (true) {
	    // Read the next chunk
	    if (DEBUG) System.err.println("reading...");
	    bytesRead = in.read(buf);
	    if (DEBUG) System.err.print("read " + bytesRead + " bytes...");
	    if (bytesRead < 0) {
		throw new IOException("ran out of input while spooling " + 
				      totalBytesToWrite + " bytes");
	    }
	    long bytesLeft = totalBytesToWrite - totalBytesWritten;
	    if (DEBUG) System.err.println("writing...");
	    if (bytesLeft <= bytesRead) {
		out.write(buf, 0, (int)bytesLeft);
		if (DEBUG) System.err.print("wrote " + 
					    bytesLeft + " bytes...");
		totalBytesWritten += bytesLeft;
		break;
	    } else {
		out.write(buf, 0, bytesRead);
		totalBytesWritten += bytesRead;
		if (DEBUG) System.err.print("wrote " + 
					    bytesRead + " bytes...");
	    }
	    if (DEBUG) System.err.println("bytes left = " + bytesLeft + 
					  " of " + totalBytesToWrite);
	}
	if (DEBUG) System.err.println("done");
	return totalBytesWritten;
    }

    private final static boolean DEBUG = false;

}
