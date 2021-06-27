/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.util;

import java.io.*;

/** Provides slightly more flexible handling of paths than the
 *  java.io.File class. 
 */
public class FileResolver {

    /** Resolves a filename relative to a base path, 
     *  similar to the way a URL is resolved relative to the server's
     *  base URL. If the filename is absolute, then it is returned as is.
     *   If it is relative, then it is resolved with respect to the 
     *   <code>path</code> parameter.
     *  @return The File object that the filename resolves to.
     *  @param filename The filename to be resolved.
     *  @param path The path to resolve relative filenames by.
     */
    public static File resolve(String path, String filename) {
	File file = new File(filename);
	if (file.isAbsolute()) {
	    return file;
	} else {
	    return new File(path, filename);
	}
    }

}
