/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.grads.client;

import java.io.*;
import java.util.*;
import java.net.*;

public class Uploader {


    public static void main(String[] args) {
	if (args.length < 3) {
	    syntaxError();
	}

	String serverURL = args[0];
	String datasetName = args[1];
	File dataFile = new File(args[2]);
	if (!dataFile.exists()) {
	    System.out.println("error: specified data file " + dataFile + " does not exist.");
	    syntaxError();
	}
	long length = dataFile.length();

	
	try {
	    URL uploadURL = new URL(serverURL + "/dods/" + datasetName);
	    HttpURLConnection con = (HttpURLConnection)uploadURL.openConnection();
	    con.setRequestMethod("POST");
	    con.setRequestProperty("ContentLength", String.valueOf(length));
	    con.setDoOutput(true);
	    con.setDoInput(true);

	    try {
		con.connect();
	    } catch (IOException ioe) {
		System.out.println("error: couldn't connect to server " + serverURL);
		syntaxError();
	    }
	    
	    InputStream fileStream = 
		new BufferedInputStream
		    (new FileInputStream
			(dataFile));

	    OutputStream uploadStream = 
		new BufferedOutputStream
		    (con.getOutputStream());

	    int bytesRead;
	    byte[] buf = new byte[16384];
	    while (true) {
		bytesRead = fileStream.read(buf, 0, buf.length);
		if (bytesRead == -1) {
		    break;
		}
		uploadStream.write(buf, 0, bytesRead);
	    }
	    uploadStream.close();

	    BufferedReader responseReader =
		new BufferedReader
		    (new InputStreamReader
			(con.getInputStream()));

	    String nextLine;
	    do {
		nextLine = responseReader.readLine();
		if (nextLine == null) {
		    break;
		}
		System.out.println(nextLine);
	    } while (true);

	} catch (MalformedURLException mue) {
	    System.out.println("error: specified URL " + serverURL + " is invalid.");
	    syntaxError();
	} catch (IOException ioe) {
	    System.out.println("error: connected ok but couldn't transfer file. more info:");
	    ioe.printStackTrace();
	    System.out.println(ioe.getMessage());
	}
	
	

    }

    private static void syntaxError(){ 
	System.out.println("usage: java grads.client.Uploader server_url shorthand_name udf_data_file");
		System.exit(0);
    }


}
