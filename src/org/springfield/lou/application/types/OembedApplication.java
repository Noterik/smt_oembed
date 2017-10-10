/* 
*SceneApplication.java
* 
* Copyright (c) 2016 Noterik B.V.

*/
package org.springfield.lou.application.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springfield.fs.FSList;
import org.springfield.fs.FSListManager;
import org.springfield.fs.FsNode;
import org.springfield.lou.application.*;
import org.springfield.lou.controllers.*;
import org.springfield.lou.homer.LazyHomer;
import org.springfield.lou.screen.*;
import org.springfield.lou.servlet.LouServlet;

public class OembedApplication extends Html5Application {
	
	/**
	 */
 	public OembedApplication(String id) {
		super(id); 
		this.setSessionRecovery(true);
	}
 	
 	/**
 	 * A new screen has joined our appliction url
 	 *  
 	 * @see org.springfield.lou.application.Html5Application#onNewScreen(org.springfield.lou.screen.Screen)
 	 */
    public void onNewScreen(Screen s) {
    	s.get("#screen").attach(new ScreenController()); // add the screen tag (backwards comp)
    	String embedurl = s.getParameter("url");
    	String embedtype = findEmbedType(embedurl);
    	
		Capabilities cap = s.getCapabilities();
		String platform = s.getCapabilities().getCapability("platform");
		String useragent = s.getCapabilities().getCapability("useragent");
		
		//System.out.println("CAP="+cap.getCapabilities().toString());
		
    	s.get("#screen").append("div","oembed",new OembedController(embedurl,embedtype,platform,useragent)); 
    }
    
    private String findEmbedType(String url) {
    	System.out.println("URL="+url);
    	if (url.indexOf("flickr.com")!=-1) {
    		return "flickr";
    	} else if (url.indexOf("/domain/")!=-1 && url.indexOf("/video/")!=-1) {
    		return "mstvideo";
    	} else if (url.indexOf("/domain/")!=-1 && url.indexOf("/audio/")!=-1) {
    	    	return "mstaudio";
    	} else if (url.indexOf(".mp4")!=-1) {
    		return "mstvideo";
    	} else if (url.indexOf(".mp3") != -1) {
    	    	return "mstaudio";
    	}
    	return null;
    }

}
