/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.filter;

import java.util.*;

import org.iges.anagram.*;
import org.iges.anagram.service.*;

/** Dispatches a client request to its designated service. This
 *  filter does not pass requests onwards.
 */

public class DispatchFilter 
    extends Filter {

    public String getFilterName() {
	return "dispatch";
    }

    protected void doFilter(ClientRequest clientRequest) 
	throws ModuleException {

	if (clientRequest.getService() == null || 
	    !clientRequest.getService().isEnabled()) {

	    throw new ModuleException(this, 
				      clientRequest.getServiceName() +
				      " is not an available service");
	}
	if (debug()) debug(clientRequest + "dispatching request");

	clientRequest.getService().handle(clientRequest);

	if (debug()) debug(clientRequest + "request handled succesfully");
    }
    
}
