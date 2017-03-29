package EZShare;

import java.util.ArrayList;

public class Command extends JsonModel {
	
	public String command;
	public String secret;
	public Boolean relay;
	public Resource resource;
	public Resource resourceTemplate;
	public ArrayList<ServerInfo> serverList;
	
	@Override
	public Command fromJson(String json) {
		return g.fromJson(json, Command.class);
	}
	
}
