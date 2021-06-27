/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.util;

import java.util.*;
import java.text.*;


/** This is actually a container for three similar classes, Range.Double, Range.Long, and Range.Date. 
 *  All represent an interval, either of real #s, integers, or time. The
 *  methods are similar in function, but take different primitive types as arguments
 *  Although there is no formal relationship between them, they are grouped together  
 *  to emphasize their similarity.
 *
 * Last modified: $Date: 2008/07/22 17:22:28 $ 
 * Revision for this file: $Revision: 1.10 $
 * Release name: $Name: v2_0 $
 * Original for this file: $Source: /homes/cvsroot/gds/gds/src/org/iges/util/Range.java,v $
 */
public class Range{

    /** Represents a range of floating-point numbers. */
    public static class Double {

	public double min;
	public double max;

	/** Creates a new Range.Double */
	public Double(double _min, double _max) {
	    min = _min;
	    max = _max;
	}

	/** Tests whether a given value is contained in this range */
	public boolean contains(Range.Double range) {
	    return (this.min <= range.min) && (this.max >= range.max);
	}
	
	/** Tests whether this range overlaps the one specified */
	public boolean overlaps(Range.Double range) {
	    return (range.min <= this.max) && (range.max >= this.min);
	}

	public boolean equals(Range.Double range) {
	    return (this.min == range.min) && (this.max == range.max);
	}
	
	/** Returns a string representation of this range */
	public String toString() {
	    return "(" + min + ", " + max + ")";
	}
    }
    
    /** Represents a range of integers. */
    public static class Long {

	public long min;
	public long max;

	/** Creates a new Range.Long */
	public Long(long _min, long _max) {
	    min = _min;
	    max = _max;
	}

	/** Tests whether a given value is contained in this range */
	public boolean contains(Range.Long range) {
	    return (this.min <= range.min) && (this.max >= range.max);
	}
	
	/** Tests whether this range overlaps the one specified */
	public boolean overlaps(Range.Long range) {
	    return (range.min <= this.max) && (range.max >= this.min);
	}

	public Range.Long union(Range.Long range) {
	    return new Range.Long(Math.min(this.min, range.min),
			     Math.max(this.max, range.max));
	}

	/** Returns the number of integers contained in this range */
	public long size() {
	    return max - min + 1;
	}
	
	/** Returns a string representation of this range */
	public String toString() {
	    return "(" + min + ", " + max + ")";
	}
    }

    /** Represents a range defined by strings (for example ensemble members). 
	No comparisons or calculations can be done on this kind of range,
	so overlaps(), contains(), size(), and union() are not supported. */
    public static class Named {

	public String min;
	public String max;

	/** Creates a new Range.String */
	public Named(String _min, String _max) {
	    min = _min;
	    max = _max;
	}

	/** Returns a string representation of this range */
	public String toString() {
	    return "(" + min + ", " + max + ")";
	}
    }



    /** Represents a range of dates. */
    public static class Date {

	
	public java.util.Date min;
	public java.util.Date max;
	public String minString;
	public String maxString;

	/** Creates a new Range.Date from two GrADS format date strings */
	public Date(String _minString, String _maxString) {
	    minString = _minString;
	    maxString = _maxString;
	}

	protected void parseDates() {
	    min = parseGradsFormat(minString);
	    max = parseGradsFormat(maxString);
	    
	    if (min == null) {
		throw new IllegalArgumentException
		    (minString + " is not a valid GrADS date");
	    }
	    if (max == null) {
		throw new IllegalArgumentException
		    (maxString + " is not a valid GrADS date");
	    }
	}	    


	/** Tests whether a given value is contained in this range */
	public boolean contains(Range.Date range) 
	    throws IllegalArgumentException {
	    parseDates();
	    return (min.getTime() <= range.min.getTime()) 
		&& (max.getTime() >= range.max.getTime());
	}
	
	/** Tests whether this range overlaps the one specified */
	public boolean overlaps(Range.Date range) 
	    throws IllegalArgumentException {
	    parseDates();
	    return (range.min.getTime() <= max.getTime()) 
		&& (range.max.getTime() >= min.getTime());
	}
	
	/** Returns a string representation of this range */
	public String toString() {
	    return "(" + min + ", " + max + ")";
	}
    }


    public static String printGradsDate(java.util.Date date) {
	SimpleDateFormat gradsFormat
	    = new SimpleDateFormat ("yyyy:M:d:H:m");
	return gradsFormat.format(date);
    }



    /** Parses a date from (hopefully) any of the various formats used
     * by GrADS into a Java Date object. */
    public static java.util.Date parseGradsFormat(String dateString) 
	throws IllegalArgumentException {

	// GrADS times are implicitly GMT. Must be specific in Java or else 
	// local time zone will be used. 
	dateString = dateString.toLowerCase() + " GMT";

	// Get a Date object from the dateString
	ParsePosition pos = new ParsePosition(0);
	java.util.Date parsedDate = null;
	for (int i = 0; i < gradsDateFormats.length; i++) {
	    parsedDate = gradsDateFormats[i].parse(dateString, pos);
	    if (parsedDate != null) {
		return parsedDate;
	    }
	}
	// If we drop out of the for loop, then none of the formats matched
	throw new IllegalArgumentException("Can't parse date string: " + 
					       dateString);
    }

    protected final static SimpleDateFormat[] gradsDateFormats = {
	// These are output by "set time"
	new SimpleDateFormat ("yyyy:M:d:H:m z",Locale.US),
	new SimpleDateFormat ("yyyy:M:d:H z",Locale.US),
	// These are legal formats in CTL files and "q time"
	new SimpleDateFormat ("H:m'z'ddMMMyyyy z",Locale.US), // single quotes for literal 'z'
	new SimpleDateFormat ("H'z'ddMMMyyyy z",Locale.US), // single quotes for literal 'z'
	new SimpleDateFormat ("ddMMMyyyy z",Locale.US), 
	new SimpleDateFormat ("MMMyyyy z",Locale.US),
	new SimpleDateFormat ("yyyy z",Locale.US),
    };
}
