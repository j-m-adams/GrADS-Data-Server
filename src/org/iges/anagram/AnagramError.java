/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/

package org.iges.anagram;

/** Thrown if an unrecoverable error occurs, that normal server code should
 *  not try to anticipate.<p>
 *
 *  This class should be used extremely sparingly, and no code should
 *  ever attempt to catch it except the top-level error handler in 
 *  AnagramServlet.<p>
 * 
 *  It should not be used for problems which are recoverable,
 *  or caused by administrator or user mistakes. These should be handled using
 *  AnagramException and its subclasses.<p>
 *  
 *  Throwing it should be considered the equivalent of a failed "assert"
 *  statement. (In fact once Java 1.4 is widely adopted it may be 
 *  desirable to eliminate this class). <p>
 *  
 *  For instance, appropriate uses might be things like mysteriously 
 *  missing classes and 
 *  libraries, or Java core methods which declare an exception, but 
 *  which should never throw under the circumstances the server is 
 *  calling them. <p>
 *
 *  Careful use of this class eliminates many unnecessary throws declarations
 *  and try blocks for conditions that are not anticipated to occur.
 *
 * @see AnagramException
*/
public class AnagramError 
    extends Error {

    /** Creates an AnagramException with no message */
    public AnagramError() {
	super();
    }
    
    /** Creates an AnagramException with the message given */
    public AnagramError(String message) {
	super(message);
    }
    
}
