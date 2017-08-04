package org.springfield.lou.controllers;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.springfield.fs.*;
import org.springfield.lou.model.*;
import org.springfield.lou.screen.Screen;

public class FlickrController extends Html5Controller {
	
	private int visiblecountdown = 0;
	
	/**
	 * Pointers have been attached to view
	 * @see org.springfield.lou.controllers.Html5Controller#attach(java.lang.String)
	 */
	public void attach(String sel) {
		selector = sel;
		fillPage();
		model.onNotify("/shared[timers]/1second","onTimeoutChecks",this); 
	}
	
	public void onTimeoutChecks(ModelEvent e) {
		if (visiblecountdown>0) {
			visiblecountdown = visiblecountdown - 1;
		} else {
			screen.get("#flickr_player_metadata").css("visibility","hidden");
		}
	}
	
	public void fillPage() {
		JSONObject data =  new JSONObject();
		String embedtype = model.getProperty("@embedtype");
		String embedurl = model.getProperty("@embedurl");
		
		data.put("embedtype",embedtype);
		data.put("embedurl",embedurl);
		//addOEmbedData(data,"https://www.flickr.com/services/oembed/?format=json&url=http%3A//www.flickr.com/photos/128068908@N03/34556930614");
		addOEmbedData(data,"https://www.flickr.com/services/oembed/?format=json&url=http://"+embedurl);

		
		screen.get(selector).render(data);
		screen.get(selector).track("mousemove","positionChange", this);
	}
	
	public void positionChange(Screen s,JSONObject data) {
		// make diff visible, reset timer
		screen.get("#flickr_player_metadata").css("visibility","visible");
		visiblecountdown = 4;
	}
	
	
	private void addOEmbedData(JSONObject data,String url) {
		StringBuilder result = new StringBuilder();
		try {
			URL serverUrl = new URL(url);
			HttpURLConnection urlConnection = (HttpURLConnection)serverUrl.openConnection();
	
			urlConnection.setDoOutput(true);
			urlConnection.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close();

			JSONParser jp = new JSONParser();

			JSONObject newdata = (JSONObject)jp.parse(result.toString());
		
			System.out.println("DATA="+newdata.toJSONString());
			data.put("flickr", newdata);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	
		
 	 
}

