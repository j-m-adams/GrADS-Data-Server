/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.util;

public class Strings {

    public static String replace(String source, String findStr, String replaceStr) {
	if (source == null) { return null; }

	int start = 0;
	StringBuffer result = new StringBuffer();

	while (true) {
	    int pos = source.indexOf(findStr, start);
	    if (pos == -1) {
		result.append(source.substring(start));
		break;
	    }
	    result.append(source.substring(start, pos));
	    result.append(replaceStr);
	    start = pos + 1;
	    //	    System.out.println(result.toString());
	}
	
	return result.toString();
    }

    public static String escape(String source, char escape) {
	return replace(source, "" + escape, "\\" + escape);
    }

    public static String escapeXMLSpecialChars(String source) {
       	source = replace(source, "&", "&amp;"); // must come first
	source = replace(source, "<", "&lt;");
	source = replace(source, ">", "&gt;");
	source = replace(source, "\'", "&apos;");
	source = replace(source, "\"", "&quot;");
	return source;
    }
}
