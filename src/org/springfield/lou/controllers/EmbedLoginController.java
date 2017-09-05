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

public class EmbedLoginController extends Html5Controller {
	
	
	public EmbedLoginController() {
		super();
	}
	
	/**
	 * Pointers have been attached to view
	 * @see org.springfield.lou.controllers.Html5Controller#attach(java.lang.String)
	 */
	public void attach(String sel) {
		model.setProperty("@keyeventowner","login");
		selector = sel;
		fillPage();
	}
	
	public void fillPage() {
		JSONObject data =  new JSONObject();
		screen.get(selector).render(data);
	    screen.get("#login_submit").on("mouseup","login_input","onSubmit", this);
	}
	
    public void onSubmit(Screen s,JSONObject data) {
    	String password = (String)data.get("login_input");
    	if (password.equals("geheim")) {
	    	screen.get("#screen").append("div","edit",new EmbedEditController()); 	
    	} else {
    		model.setProperty("@keyeventowner","mstvideo");
    	}
    	screen.get(selector).remove();
    }
	

		
 	 
}

