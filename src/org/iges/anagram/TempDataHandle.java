/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram;

import java.io.*;
import java.util.*;

/** A handle used by the catalog module to manage temporary data. */
public interface TempDataHandle
    extends Serializable {

    /** @return Returns handles for accessing the data. Multiple handles
     *          may be returned if the data can be accessed by multiple
     *          names (for instance, via an analysis expression or using
     *          a short name), or if a single operation generated multiple
     *          data objects
     */
    public DataHandle[] getDataHandles();

    /** @return The number of bytes of storage being used by this data.
     *          This is dependent on the storage format of the data and
     *          thus does not directly indicate the number of data values
     *          being stored. 
     */
    public long getStorageSize();

    /** @return The time at which the data was initially stored
     */
    public long getCreateTime();

    /** A set of data handle names upon which were used to generate this
     *  temporary data. The catalog module uses this to check if a 
     *  result is made out-of-date by the modification of another
     *  dataset.
     */
    public Set getDependencies();

    /** Deletes the temporary data. 
     */
    public void deleteStorage();

}
