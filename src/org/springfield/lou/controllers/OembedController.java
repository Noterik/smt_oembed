package org.springfield.lou.controllers;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.simple.*;
import org.springfield.fs.*;
import org.springfield.lou.homer.LazyHomer;
import org.springfield.lou.model.*;

public class OembedController extends Html5Controller {
	
	String url;
	String type;
	String useragent;
	String platform;
	
	public OembedController(String u,String t,String p,String ua) {
		super();
		url=u;
		type=t;
		useragent = ua;
		platform = p;
		System.out.println("TYPE="+t);
	}
	
	/**
	 * Pointers have been attached to view
	 * @see org.springfield.lou.controllers.Html5Controller#attach(java.lang.String)
	 */
	public void attach(String sel) {
		selector = sel;
    	model.setProperty("@embedurl",url);
    	model.setProperty("@embedtype",type);
    	model.setProperty("@embedplatform",platform);
    	model.setProperty("@embeduseragent",useragent);
		fillPage();
		
		String embedtype = model.getProperty("@embedtype");
		if (embedtype.equals("flickr")) {
	    	screen.get(selector).append("div","flickr",new FlickrController()); 	
		} else if (embedtype.equals("mstvideo")) {
	    	screen.get(selector).append("div","mstvideo",new MstVideoController()); 
		}
		
	}
	
	public void fillPage() {
		JSONObject data =  new JSONObject();
		screen.get(selector).render(data);
	}
	

		
 	 
}

