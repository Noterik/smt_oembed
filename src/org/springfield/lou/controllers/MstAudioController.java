package org.springfield.lou.controllers;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.json.simple.*;
import org.springfield.fs.*;
import org.springfield.lou.homer.LazyHomer;
import org.springfield.lou.model.*;
import org.springfield.lou.screen.Screen;

public class MstAudioController extends Html5Controller {
	
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
			} else {
			    System.out.println("node not found ");
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
			screen.get("#mstaudio_logo").css("visibility","visible");
		} else {
			screen.get("#mstaudio_logo").css("visibility","hidden");		
		}
	}
	
	public void onLogoChange(ModelEvent event) {
		presetnode = model.getNode("/domain['oembed']/presets['default']");
		String logo_x = presetnode.getProperty("logo_x");
		String logo_y = presetnode.getProperty("logo_y");
		screen.get("#mstaudio_logo").css("left",logo_x);
		screen.get("#mstaudio_logo").css("top",logo_y);		
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
		addOEmbedData(data,embedurl,euscreennode);
		addStatPlays();
		
		screen.get(selector).render(data);
		screen.get(selector).track("mousemove","positionChange", this);
		model.setProperty("@keyeventowner","mstaudio");
		screen.get("#mstaudio_player").track("currentTime","currentTime", this); // track the currentTime
	//	screen.get("#mstaudio_logo").draggable();

		//screen.get("#mstaudio_logo").track("screenXPerc","onDragX", this);
		//screen.get("#mstaudio_logo").track("screenYPerc","onDragY", this);
		
	    screen.get(selector).on("mousedown","onDragStart", this);
	    screen.get(selector).on("mouseup","onDragStop", this);
	    
		presetnode = model.getNode("/domain['oembed']/presets['default']");
		String logo_x = presetnode.getProperty("logo_x");
		String logo_y = presetnode.getProperty("logo_y");
		screen.get("#mstaudio_logo").css("left",logo_x);
		screen.get("#mstaudio_logo").css("top",logo_y);	
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
	    if (kc==null || !kc.equals("mstaudio")) return;
	    
    	Long which = (Long)data.get("which");
		if (which==76) {
	    	screen.get("#screen").append("div","login",new EmbedLoginController()); 	
		}
    }
	
	public void onTimeoutChecks(ModelEvent e) {
		if (visiblecountdown>0) {
			visiblecountdown = visiblecountdown - 1;
		} else {
			screen.get("#mstaudio_player_metadata").css("visibility","hidden");
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
	    if (kc==null || !kc.equals("mstaudio")) return;
		screen.get("#mstaudio_player_metadata").css("visibility","visible");
		visiblecountdown = 4;
		if (indrag) {
			Double newx = (Double)data.get("screenXP");
			Double newy = (Double)data.get("screenYP");
			model.setProperty("/domain['oembed']/presets['default']/logo_x",""+newx+"%");
			model.setProperty("/domain['oembed']/presets['default']/logo_y",""+newy+"%");
			model.notify("@logochange"); 
		}
	}
	
	
	private void addOEmbedData(JSONObject data,String embedurl, FsNode node) {
		String url = "";

		JSONObject newdata = new JSONObject();

		// strip off the fake start part if needed 
		int pos=embedurl.indexOf("euscreen.eu/euscreen/");
		if (pos!=-1) {
			newdata.put("euscreen","true");			
			embedurl = embedurl.substring(pos+21);
			
			if (embedurl.startsWith("http")) {					
				System.out.println("External EUscreen provider");
				
				url = embedurl;
			} else {
				if (embedurl.startsWith("audio1/")) {
					embedurl = embedurl.substring(embedurl.indexOf("audio1/")+7);
				}				
				FsNode rawNode = Fs.getNode(node.getPath() + "/rawaudio/1");
				
				url = "https://audio1.noterik.com/"+embedurl+"/rawaudio/1/raw."+rawNode.getProperty("extension");
			}
		}		
		newdata.put("url", url);
		data.put("mstaudio", newdata);
	}

	private static String sendTicket(String audioFile) {
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
	
		System.out.println("I send this audio address to the ticket server:"+audioFile);
		System.out.println("And this ticket:"+ticket);
		System.out.println("And this EXPIRY:"+expiry);
		
		// Writing the post data to the HTTP request body
		BufferedWriter httpRequestBodyWriter = 
		new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
			String content = "<fsxml><properties><ticket>"+ticket+"</ticket>"
			+ "<uri>/"+audioFile+"</uri><ip>"+ipAddress+"</ip> "
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

