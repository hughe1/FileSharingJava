import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import EZShare.Command;
import EZShare.Resource;
import EZShare.ServerInfo;

public class EZShare {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// TODO
		JSONObject obj = new JSONObject();
		obj.put("hello", "bob");
		System.out.println(obj);
		
		JSONParser parser = new JSONParser();
		String s = "{\"hello\":\"bob\"}";
		try {
			obj = (JSONObject) parser.parse(s);
			System.out.println(obj);
			System.out.println(obj.get("hello"));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// create a resource object and convert to json using google gson
		Resource resource = new Resource();
		resource.channel = "channelgoeshere";
		Gson g = new GsonBuilder().setPrettyPrinting().create();
		System.out.println(g.toJson(resource));
		
		Command command = new Command();
		command.command = "PUBLISH";
		command.resource = resource;
		
		System.out.println(g.toJson(command));
		String json = g.toJson(command);
		System.out.println(json);
		
		Command copy = g.fromJson(json,Command.class);
		System.out.println(copy.resource.channel);
		
		// create a exchange command with two servers
		Command exchange = new Command();
		exchange.command = "EXCHANGE";
		exchange.serverList = new ArrayList<ServerInfo>();
		exchange.serverList.add(new ServerInfo("115.146.85.165",3780));
		exchange.serverList.add(new ServerInfo("115.146.85.24",3780));
		System.out.println(g.toJson(exchange));
		
		URL url;
		try {
			url = new URL("http://www.google.com");
			URI uri = url.toURI();
			System.out.println(uri.toString());
		} catch (MalformedURLException | URISyntaxException e) {
			e.printStackTrace();
		} //Some instantiated URL object
		
	}

}
