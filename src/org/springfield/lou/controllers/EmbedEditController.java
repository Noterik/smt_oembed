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
import org.springfield.lou.screen.Screen;

public class EmbedEditController extends Html5Controller {
	
	FsNode presetnode;
	String vid = "";
	
	public EmbedEditController() {
		super();
	}
	
	/**
	 * Pointers have been attached to view
	 * @see org.springfield.lou.controllers.Html5Controller#attach(java.lang.String)
	 */
	public void attach(String sel) {
		selector = sel;
		fillPage();
		model.setProperty("@keyeventowner","edit");
		model.onNotify("@statchange","onStatChange", this);
	}
	
	public void onStatChange(ModelEvent event) {
		fillPage();
	}
	
	public void fillPage() {
		presetnode = model.getNode("/domain['oembed']/presets['default']");
		JSONObject data =  new JSONObject();
		String embedurl = model.getProperty("@embedurl");
		String embedtype = model.getProperty("@embedtype");

		int pos = embedurl.lastIndexOf("/");
		if (pos!=-1) {
			vid = embedurl.substring(pos+1);
			addStats(data,vid);
		}
		data.put("videoid",vid);
		if (presetnode.getProperty("logo").equals("on")) {
			data.put("logo","on");
		}
		
		screen.get(selector).render(data);
	    screen.get("#edit_done").on("mouseup","onDone", this);
	    screen.get(".edit_logo_button").on("mouseup","onLogoButton", this);
	    screen.get("#edit_reset").on("mouseup","onReset", this);
	}
	
    public void onReset(Screen s,JSONObject data) {
    	model.setProperty("/domain['oembed']/stats['"+vid+"']/plays","0");
    	model.setProperty("/domain['oembed']/stats['"+vid+"']/streamed","0");
    	model.setProperty("/domain['oembed']/stats['"+vid+"']/slowstream","0");
    	model.setProperty("/domain['oembed']/stats['"+vid+"']/lastplayed","");
    	model.setProperty("/domain['oembed']/stats['"+vid+"']/mac_safari","");
       	model.setProperty("/domain['oembed']/stats['"+vid+"']/mac_chrome","");
       	model.setProperty("/domain['oembed']/stats['"+vid+"']/mac_firefox","");
    	fillPage();
    }
	
    public void onLogoButton(Screen s,JSONObject data) {
    	String button = (String)data.get("id");
    	if (button.indexOf("_on")!=-1) {
    		model.setProperty("/domain['oembed']/presets['default']/logo","on");
			model.notify("@editchange"); 
    	} else {
    		model.setProperty("/domain['oembed']/presets['default']/logo","off");
			model.notify("@editchange"); 
    	}
    	fillPage();
    }
	
	private void addStats(JSONObject data,String vid) {
		FsNode statnode = model.getNode("/domain['oembed']/stats['"+vid+"']");
		if (statnode!=null) {
			String plays = statnode.getProperty("plays");
			if (plays!=null && !plays.equals("")) {
				data.put("plays", plays);
			} else {
				data.put("plays", "0");
			}
			String streamed = statnode.getProperty("streamed");
			if (streamed!=null && !streamed.equals("")) {
				data.put("streamed", streamed);
			} else {
				data.put("streamed", "0");
			}
			String slowstream = statnode.getProperty("slowstream");
			if (slowstream!=null && !slowstream.equals("")) {
				data.put("slowstream", slowstream);
			} else {
				data.put("slowstream", "0");
			}
			String lastplayed = statnode.getProperty("lastplayed");
			if (lastplayed!=null && !lastplayed.equals("")) {
				data.put("lastplayed", lastplayed);
			} else {
				data.put("lastplayed", "unknown");
			}
			String mac_safari = statnode.getProperty("mac_safari");			
			if (mac_safari!=null && !mac_safari.equals("")) {
				data.put("mac_safari", mac_safari);
			} else {
				data.put("mac_safari", "unknown");
			}
			String mac_chrome = statnode.getProperty("mac_chrome");			
			if (mac_chrome!=null && !mac_chrome.equals("")) {
				data.put("mac_chrome", mac_chrome);
			} else {
				data.put("mac_chrome", "unknown");
			}
			String mac_firefox = statnode.getProperty("mac_firefox");			
			if (mac_firefox!=null && !mac_firefox.equals("")) {
				data.put("mac_firefox", mac_firefox);
			} else {
				data.put("mac_firefox", "unknown");
			}


		}
	}
	
    public void onDone(Screen s,JSONObject data) {
		model.setProperty("@keyeventowner","mstvideo");
    	screen.get(selector).remove();
    }
	

		
 	 
}

