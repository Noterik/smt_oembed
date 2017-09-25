package org.springfield.lou.controllers;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
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
	private int visiblecountdown = 0;
	FsNode vidnode = null;
	FsNode euscreennode  = null;
	boolean streamed = false;
	private int streamedtimer = 8;
	FsNode presetnode;
	boolean indrag = false;
	
	/**
	 * Pointers have been attached to view
	 * @see org.springfield.lou.controllers.Html5Controller#attach(java.lang.String)
	 */
	public void attach(String sel) {
		if (generator==null) generator = new Random(System.currentTimeMillis());
		selector = sel;
		String embedurl = model.getProperty("@embedurl");
		String vid = "";
		int pos = embedurl.lastIndexOf("/");
		if (pos!=-1) {
			vid = embedurl.substring(pos+1);
			
			// test for maggie
			FSList fslist = FSListManager.get("/domain/euscreenxl/user/*/*"); // get our collection from cache
			
			System.out.println("EUSCREEN SIZE="+fslist.size());
			List<FsNode> nodes = fslist.getNodesFiltered(vid.toLowerCase()); // find the item
			if (nodes!=null && nodes.size()>0) {
				euscreennode = (FsNode)nodes.get(0);
				//System.out.println("FOUND NODE !!!! NODE="+euscreennode.asXML());
			}
			
			
			vidnode = model.getNode("/domain['oembed']/stats['"+vid+"']");
			if (vidnode==null) {
				vidnode = new FsNode("stats",vid);
				model.putNode("/domain['oembed']/", vidnode);
			}
		}
		fillPage();
	    screen.get("#screen").on("keypress", "keyPressed", this);
		model.onNotify("/shared[timers]/1second","onTimeoutChecks",this); 
		model.onNotify("@editchange","onEditChange", this);
		model.onNotify("@logochange","onLogoChange", this);
	}
	
	public void onEditChange(ModelEvent event) {
		presetnode = model.getNode("/domain['oembed']/presets['default']");
		String logo = presetnode.getProperty("logo");
		if (logo!=null && logo.equals("on")) {
			screen.get("#mstvideo_logo").css("visibility","visible");
		} else {
			screen.get("#mstvideo_logo").css("visibility","hidden");		
		}
	}
	
	public void onLogoChange(ModelEvent event) {
		presetnode = model.getNode("/domain['oembed']/presets['default']");
		String logo_x = presetnode.getProperty("logo_x");
		String logo_y = presetnode.getProperty("logo_y");
		screen.get("#mstvideo_logo").css("left",logo_x);
		screen.get("#mstvideo_logo").css("top",logo_y);		
	}
	
	public void fillPage() {
		JSONObject data =  new JSONObject();
		String embedtype = model.getProperty("@embedtype");
		String embedurl = model.getProperty("@embedurl");
		
		String iframeticket = screen.getParameter("ticket");
		System.out.println("IFRAME TICKET="+iframeticket);
		if (iframeticket!=null) {
			FsNode ticketnode = model.getNode("/domain/oembed/service/marvin/ticket/"+iframeticket);
			System.out.println("ticketnode="+ticketnode);
			if (ticketnode!=null) {
				String turl = ticketnode.getProperty("url");
				//System.out.println("NODE="+ticketnode.asXML());
				if (turl.equals(embedurl)) {
					try {
						long te = Long.parseLong(ticketnode.getProperty("expire"));
						if (te>(new Date().getTime()/1000)) {
							// we have a valid play-out do noting
							System.out.println("VALID IFRAME REQEUST ON="+turl);
						} else {
							data.put("noplay","ticket expired");	
							System.out.println("ticket expired");		
						}
					} catch(Exception e) {
						data.put("noplay","ticket expire error");
						System.out.println("ticket expire error");	
					}
					//System.out.println("TURL="+turl);
					//System.out.println("PURL="+embedurl);
				} else {
					data.put("noplay","no url match");	
					System.out.println("no url match");		
				}
			} else {
				data.put("noplay","no ticket match");
				System.out.println("no ticket match");	
			}
			// now delete the ticket node
			model.deleteNode("/domain/oembed/service/marvin/ticket/"+iframeticket);
		} else {
			data.put("noplay","no ticket found");
			System.out.println("no ticket found");
		}
		
		if (euscreennode!=null) {
			data.put("provider_name",euscreennode.getProperty("provider"));
			data.put("title",euscreennode.getProperty("TitleSet_TitleSetInEnglish_title"));
			data.put("copyright",euscreennode.getProperty("iprRestrictions"));
			data.put("genre",euscreennode.getProperty("genre"));
		}
		presetnode = model.getNode("/domain['oembed']/presets['default']");
		if (presetnode!=null) {
			if (presetnode.getProperty("logo").equals("on")) {
				data.put("logo","on");
				data.put("logo_x",presetnode.getProperty("logo_x"));
				data.put("logo_y",presetnode.getProperty("logo_y"));
				data.put("logo_url",presetnode.getProperty("logo_url"));
			}
		}

		
		data.put("embedtype",embedtype);
		System.out.println("EMBEDURL="+embedurl);
		data.put("embedurl",embedurl);
		addOEmbedData(data,embedurl);
		addStatPlays();
		
		screen.get(selector).render(data);
		screen.get(selector).track("mousemove","positionChange", this);
		model.setProperty("@keyeventowner","mstvideo");
		screen.get("#mstvideo_player").track("currentTime","currentTime", this); // track the currentTime
	//	screen.get("#mstvideo_logo").draggable();

		//screen.get("#mstvideo_logo").track("screenXPerc","onDragX", this);
		//screen.get("#mstvideo_logo").track("screenYPerc","onDragY", this);
		
	    screen.get(selector).on("mousedown","onDragStart", this);
	    screen.get(selector).on("mouseup","onDragStop", this);
	    
		presetnode = model.getNode("/domain['oembed']/presets['default']");
		String logo_x = presetnode.getProperty("logo_x");
		String logo_y = presetnode.getProperty("logo_y");
		screen.get("#mstvideo_logo").css("left",logo_x);
		screen.get("#mstvideo_logo").css("top",logo_y);	
	}
	
	public void onDragStart(Screen s,JSONObject data) {
		indrag = true;
	}
	
	public void onDragStop(Screen s,JSONObject data) {
		indrag = false;
	}
	
	public void onDragX(Screen s,JSONObject data) {
		Double newx = (Double)data.get("screenXPerc");
		System.out.println("X="+data.toJSONString());
		model.setProperty("/domain['oembed']/presets['default']/logo_x",""+newx+"%");
		model.notify("@logochange"); 
	}
	
	public void onDragY(Screen s,JSONObject data) {
		Double newy = (Double)data.get("screenYPerc");
		System.out.println("Y="+newy);
	}
	
	
	public void currentTime(Screen s,JSONObject data) {
		if (streamed) return; // we know all we need to know for now
		String dates=""+new Date().toLocaleString();
		try {
			Double vt = (Double)data.get("currentTime");
			if (vt>2000) {
				streamed = true;
				int currentstreamed = Integer.parseInt(vidnode.getProperty("streamed"));
				model.setProperty("/domain['oembed']/stats['"+vidnode.getId()+"']/streamed",""+(currentstreamed+1));

				model.setProperty("/domain['oembed']/stats['"+vidnode.getId()+"']/lastplayed",dates);

				String useragent = model.getProperty("@embeduseragent");
				String platform = model.getProperty("@embedplatform");
				String name=getPlatform(platform)+"_"+getBrowser(useragent);
				model.setProperty("/domain['oembed']/stats['"+vidnode.getId()+"']/"+name,dates);

				//System.out.println("aaaa="+platform+" "+useragent);
				//System.out.println("code="+name);
				
				model.notify("@statchange"); 
				
			} 
		} catch(Exception e) {
			//model.setProperty("/domain['oembed']/stats['"+vidnode.getId()+"']/streamed","1");
			model.setProperty("/domain['oembed']/stats['"+vidnode.getId()+"']/lastplayed",dates);
			String useragent = model.getProperty("@embeduseragent");
			String platform = model.getProperty("@embedplatform");
			String name=getPlatform(platform)+"_"+getBrowser(useragent);
			model.setProperty("/domain['oembed']/stats['"+vidnode.getId()+"']/"+name,dates);
		}
	}
	
	private String getPlatform(String platform) {
		if (platform.indexOf("Win")!=-1) {
			return "win";
		} else if (platform.indexOf("Mac")!=-1) {
			return "mac";
		}
		return "unknown";
	}
	
	private String getBrowser(String useragent) {
		if (useragent.indexOf("Chrome")!=-1) {
			return "chrome";
		} else if (useragent.indexOf("Safari")!=-1) {
			return "safari";
		} else if (useragent.indexOf("Firefox")!=-1) {
			return "firefox";
		}
		return "unknown";
	}
	
	private void addStatPlays() {
		if (vidnode!=null) {
			try {
				int currentplays = Integer.parseInt(vidnode.getProperty("plays"));
				model.setProperty("/domain['oembed']/stats['"+vidnode.getId()+"']/plays",""+(currentplays+1));
				model.notify("@statchange"); 
			} catch(Exception e) {
				model.setProperty("/domain['oembed']/stats['"+vidnode.getId()+"']/plays","1");
				model.notify("@statchange"); 
			}
		}
	}
	
	private void addStatSlowStream() {
		if (vidnode!=null) {
			try {
				int currentslowstream = Integer.parseInt(vidnode.getProperty("slowstream"));
				model.setProperty("/domain['oembed']/stats['"+vidnode.getId()+"']/slowstream",""+(currentslowstream+1));
				model.notify("@statchange"); 
			} catch(Exception e) {
				model.setProperty("/domain['oembed']/stats['"+vidnode.getId()+"']/slowstream","1");
				model.notify("@statchange"); 
				
			}
		}
	}
	
    public void keyPressed(Screen s,JSONObject data) {
	    String kc = model.getProperty("@keyeventowner");
	    if (kc==null || !kc.equals("mstvideo")) return;
	    
    	Long which = (Long)data.get("which");
		if (which==76) {
	    	screen.get("#screen").append("div","login",new EmbedLoginController()); 	
		}
    }
	
	public void onTimeoutChecks(ModelEvent e) {
		if (visiblecountdown>0) {
			visiblecountdown = visiblecountdown - 1;
		} else {
			screen.get("#mstvideo_player_metadata").css("visibility","hidden");
		}
		// check the slow stream count
		if (streamedtimer>0) {
			streamedtimer = streamedtimer - 1;
		} else if (streamedtimer!=-1) {
			streamedtimer = -1;
			if (!streamed) {
				addStatSlowStream();
			}
		}
	}
	
	public void positionChange(Screen s,JSONObject data) {
		// make diff visible, reset timer
		
	    String kc = model.getProperty("@keyeventowner");
	    if (kc==null || !kc.equals("mstvideo")) return;
		screen.get("#mstvideo_player_metadata").css("visibility","visible");
		visiblecountdown = 4;
		if (indrag) {
			Double newx = (Double)data.get("screenXP");
			Double newy = (Double)data.get("screenYP");
			model.setProperty("/domain['oembed']/presets['default']/logo_x",""+newx+"%");
			model.setProperty("/domain['oembed']/presets['default']/logo_y",""+newy+"%");
			model.notify("@logochange"); 
		}
	}
	
	
	private void addOEmbedData(JSONObject data,String embedurl) {
			// strip off the fake start part if needed 
		JSONObject newdata = new JSONObject();
			int pos=embedurl.indexOf("/euscreen/");
			if (pos!=-1) {
				embedurl = embedurl.substring(pos+9);
				newdata.put("mstticket","true");
				System.out.println("SIGNAL TICKET");
			}
			String ticket  = sendTicket(embedurl);
			String url = "https://stream.noterik.com/progressive"+embedurl+"/rawvideo/1/raw.mp4";

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

