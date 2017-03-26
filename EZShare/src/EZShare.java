import java.util.ArrayList;

import EZShare.Command;
import EZShare.ServerInfo;

public class EZShare {

	public static void main(String[] args) {
		
		Command exchange = new Command();
		exchange.command = "EXCHANGE";
		exchange.serverList = new ArrayList<ServerInfo>();
		exchange.serverList.add(new ServerInfo("115.146.85.165",3780));
		exchange.serverList.add(new ServerInfo("115.146.85.24",3781));
		
		Command copyExchange = new Command().fromJson(exchange.toJson());
		
		System.out.println(exchange.toJson());
		System.out.println(copyExchange.toJsonPretty());
	}

}
