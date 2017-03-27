import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import EZShare.Command;
import EZShare.Resource;
import EZShare.ServerInfo;

public class EZShare {

	public static void main(String[] args) {
		
//		Command exchange = new Command();
//		
//		exchange.command = "EXCHANGE";
//		exchange.serverList = new ArrayList<ServerInfo>();
//		exchange.serverList.add(new ServerInfo("115.146.85.165",3780));
//		exchange.serverList.add(new ServerInfo("115.146.85.24",3781));
//		
//		Command copyExchange = new Command().fromJson(exchange.toJson());
//		
//		System.out.println(exchange.toJson());
//		System.out.println(copyExchange.toJsonPretty());
		
		HashSet<Resource> map = new HashSet<>();
		Resource r1 = new Resource();
		Resource r2 = new Resource();
		r2.uri = "google.com";
		map.add(r1);
		System.out.println(map.contains(r2));
	}

}
