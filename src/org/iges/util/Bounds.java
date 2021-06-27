/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.util;

import java.util.*;

/** This is actually a container for two similar classes, Bounds.Grid
 *  and Bounds.World.  Both represent a 5-dimensional constraint (a
 *  dimension environment, in GrADS terms). The methods are similar in
 *  function, but take different primitive types as arguments Although
 *  there is no formal relationship between them, they are grouped
 *  together to emphasize their similarity.
 */
public class Bounds {

    /** Represents a 5-dimensional constraint in grid (relative) coordinates.
     *  This class follows the GrADS 1-based indexing convention.
     */
    public static class Grid {
	/** Creates a Bounds.Grid object with the constraints given.
	 * No min < max validation is performed.
	 */
	public Grid(long xMin, long xMax,
		    long yMin, long yMax,
		    long zMin, long zMax,
		    long tMin, long tMax,
		    long eMin, long eMax) {
	    x = new Range.Long(xMin, xMax);
	    y = new Range.Long(yMin, yMax);
	    z = new Range.Long(zMin, zMax);
	    t = new Range.Long(tMin, tMax);
	    e = new Range.Long(eMin, eMax);
	}

	public Grid(Range.Long x,
		    Range.Long y,
		    Range.Long z,
		    Range.Long t,
		    Range.Long e) {
	    this.x = x;
	    this.y = y;
	    this.z = z;
	    this.t = t;
	    this.e = e;
	}


	/** Creates a Bounds.Grid object from a space-separated string.
	 *  The format of the string is "x1 x2 y1 y2 z1 z2 t1 t2 e1 e2". 
	 *  If the zeroBased parameter is true, the string will converted from 
	 *  0-based to 1-based values (i.e, 1 will be added to each value).
	 */
	public Grid(String list, boolean zeroBased) 
	    throws IllegalArgumentException {

	    StringTokenizer st = new StringTokenizer(list);
	    long value;
	    long[] bounds = new long[10];

	    int i = 0;
	    try {
		for (i = 0; i < 10; i++) {
		    value =  Long.valueOf(st.nextToken()).longValue();
		    bounds[i] = (zeroBased) ? value + 1 : value;
		}
	    } catch (NumberFormatException nfe) {
		throw new IllegalArgumentException("invalid number format in bounds expression (element " + (i+1) +")");
	    } catch (NullPointerException npe) {
		throw new IllegalArgumentException("not enough elements in bounds expression");
	    }

	    x = new Range.Long(bounds[0], bounds[1]);
	    y = new Range.Long(bounds[2], bounds[3]);
	    z = new Range.Long(bounds[4], bounds[5]);
	    t = new Range.Long(bounds[6], bounds[7]);
	    e = new Range.Long(bounds[8], bounds[9]);

	}

	public boolean equals(Bounds.Grid grid) {
	    return (this.x.equals(grid.x)
		    && this.y.equals(grid.y)
		    && this.z.equals(grid.z)
		    && this.t.equals(grid.t)
		    && this.e.equals(grid.e));
	}



	public Bounds.Grid union(Bounds.Grid grid) {
	    return new Bounds.Grid(this.x.union(grid.x),
				   this.y.union(grid.y),
				   this.z.union(grid.z),
				   this.t.union(grid.t),
				   this.e.union(grid.e));
	}

	/** Calculates the number of points in the grid (not size in bytes) */
	public long getSize() {
	    return (Math.abs(x.max - x.min) + 1) 
		* (Math.abs(y.max - y.min) + 1)
		* (Math.abs(z.max - z.min) + 1)
		* (Math.abs(t.max - t.min) + 1)
		* (Math.abs(e.max - e.min) + 1);
	}


	public Range.Long x;
	public Range.Long y;
	public Range.Long z;
	public Range.Long t;
	public Range.Long e;

	/** Returns a string representation of the grid bounds, with the indexes offset
	 * by one, because GrADS uses one-based array indexing. */
	public String toGradsString() {
	    return (x.min + 1) + " " 
		+ (x.max + 1) + " " 
		+ (y.min + 1) + " " 
		+ (y.max + 1) + " " 
		+ (z.min + 1) + " " 
		+ (z.max + 1) + " " 
		+ (t.min + 1) + " " 
		+ (t.max + 1) + " "
		+ (e.min + 1) + " " 
		+ (e.max + 1);
	}

	/** Returns a string representation of the grid bounds. This representation
	 * can be used to create a new Bounds.Grid object. */
	public String toString() {
	    return x.min + " " 
		+ x.max + " " 
		+ y.min + " " 
		+ y.max + " " 
		+ z.min + " " 
		+ z.max + " " 
		+ t.min + " " 
		+ t.max + " " 
		+ e.min + " " 
		+ e.max;
	}

    }

    /** Represents a 4-dimensional constraint in world (absolute) coordinates.
     *  Latitude, longitude are in degrees. Elevation has no standard units.
     *  Time is stored as a Java Date object.
     */
    public static class World {

	/** Creates a Bounds.World object with the constraints given.
	 * No min < max validation is performed.
	 */
	public World(Range.Double _lon,
		     Range.Double _lat,
		     Range.Double _lev,
		     Range.Date _time,
		     Range.Named _ens) {
	    lon = _lon;
	    lat = _lat;
	    lev = _lev;
	    time = _time;
	    ens = _ens;
	}

	/** Creates a Bounds.World object from a space-separated string.
	 *  The format of the string is "lon1 lon2 lat1 lat2 lev1 lev2 time1 time2 ens1 ens2". 
	 *  This constructor is used in parsing request URL's. 
	 */
	public World(String list)
	    throws IllegalArgumentException{

	    StringTokenizer st = new StringTokenizer(list);
	    
	    double[] bounds = new double[6];
	    int i = 0;
	    try {
		for (i = 0; i < 6; i++) {
		    bounds[i] = Double.valueOf(st.nextToken()).doubleValue();
		}

	    lon = new Range.Double(bounds[0], bounds[1]);
	    lat = new Range.Double(bounds[2], bounds[3]);
	    lev = new Range.Double(bounds[4], bounds[5]);

	    String minDateString = st.nextToken();
	    String maxDateString = st.nextToken();
	    time = new Range.Date(minDateString, maxDateString);

	    if (st.hasMoreTokens()) {
		String minEnsString = st.nextToken();
		String maxEnsString = st.nextToken();
		ens = new Range.Named(minEnsString, maxEnsString);
	    } else {
		String minEnsString = "1";
		String maxEnsString = "1";
		ens = new Range.Named(minEnsString, maxEnsString);
	    }
	    } catch (NumberFormatException nfe) {
		throw new IllegalArgumentException("invalid number format in bounds expression (element " + (i+1) +")");
	    } catch (NoSuchElementException nsee) {
		throw new IllegalArgumentException("Not enough elements in bounds expression");
	    }
  

	    
	}

	public Range.Double lon;
	public Range.Double lat;
	public Range.Double lev;
	public Range.Date time;
	public Range.Named ens;

	/** Returns a string representation of the world bounds. This representation
	 * can be used to create a new Bounds.World object. */
	public String toString() {
	    return 
		lon.min + " " + 
		lon.max + " " + 
		lat.min + " " + 
		lat.max + " " + 
		lev.min + " " + 
		lev.max + " " + 
		time.minString + " " + 
		time.maxString + " " +
		ens.min + " " +
		ens.max;
	}

    }


}
