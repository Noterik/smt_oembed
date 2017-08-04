package org.springfield.lou.controllers;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.Scanner;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.springfield.fs.*;
import org.springfield.lou.homer.LazyHomer;
import org.springfield.lou.model.*;
import org.springfield.lou.screen.Screen;

public class MstVideoController extends Html5Controller {
	
	static Random generator;
	
	/**
	 * Pointers have been attached to view
	 * @see org.springfield.lou.controllers.Html5Controller#attach(java.lang.String)
	 */
	public void attach(String sel) {
		if (generator==null) generator = new Random(System.currentTimeMillis());
		selector = sel;
		fillPage();
	}
	
	public void fillPage() {
		JSONObject data =  new JSONObject();
		String embedtype = model.getProperty("@embedtype");
		String embedurl = model.getProperty("@embedurl");
		
		data.put("embedtype",embedtype);
		data.put("embedurl",embedurl);
		addOEmbedData(data,embedurl);

		
		screen.get(selector).render(data);
	}
	
	
	private void addOEmbedData(JSONObject data,String embedurl) {
			String ticket  = sendTicket(embedurl);
			String url = "http://stream.noterik.com/progressive"+embedurl+"/rawvideo/1/raw.mp4";
			JSONObject newdata = new JSONObject();
			newdata.put("url", url);
			newdata.put("ticket", ticket);
			data.put("mstvideo", newdata);
	}

	private static String sendTicket(String videoFile) {
		String ipAddress = LazyHomer.getExternalIpNumber();
		String random = ""+generator.nextInt(999999999);
		String ticket = "mst_"+LazyHomer.getExternalIpNumber()+"_"+random;
		try {
		URL serverUrl = new URL("http://ticket.noterik.com:8080/lenny/acl/ticket");
		HttpURLConnection urlConnection = (HttpURLConnection)serverUrl.openConnection();
	
		Long Sytime = System.currentTimeMillis();
		Sytime = Sytime / 1000;
		String expiry = Long.toString(Sytime+(15*60));
		
		// Indicate that we want to write to the HTTP request body
		
		urlConnection.setDoOutput(true);
		urlConnection.setRequestMethod("POST");
		urlConnection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
		videoFile=videoFile.substring(1);
	
		System.out.println("I send this video address to the ticket server:"+videoFile);
		System.out.println("And this ticket:"+ticket);
		System.out.println("And this EXPIRY:"+expiry);
		
		// Writing the post data to the HTTP request body
		BufferedWriter httpRequestBodyWriter = 
		new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
			String content = "<fsxml><properties><ticket>"+ticket+"</ticket>"
			+ "<uri>/"+videoFile+"</uri><ip>"+ipAddress+"</ip> "
			+ "<role>user</role>"
			+ "<expiry>"+expiry+"</expiry><maxRequests>1</maxRequests></properties></fsxml>";
		httpRequestBodyWriter.write(content);
		httpRequestBodyWriter.close();
	
		// Reading from the HTTP response body
		Scanner httpResponseScanner = new Scanner(urlConnection.getInputStream());
		while(httpResponseScanner.hasNextLine()) {
			System.out.println(httpResponseScanner.nextLine());
		}
		httpResponseScanner.close();			
		} catch(Exception e) {
			e.printStackTrace();	
		}
		return ticket;
	}
		
 	 
}

