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
	
	/**
	 * Builds a query Command given a ClientArgs object. Use case:
	 * Command command = new Command().buildQuery(...);
	 * 
	 * @param cmd contains input from the user.
	 * @return new ClientArgs object modelling the user input.
	 */
	public Command buildQuery(ClientArgs clientArgs) {
		// ensure that clientArgs contains a query, otherwise exit
		if(!clientArgs.hasOption("query")) clientArgs.printArgsHelp("");
		this.command = "query";
		this.resourceTemplate = new Resource();
		this.resourceTemplate.name = clientArgs.getOptionValue("name");
		this.resourceTemplate.addTags(clientArgs.getOptionValue("tags"));
		this.resourceTemplate.description = clientArgs.getOptionValue("description");
		this.resourceTemplate.uri = clientArgs.getOptionValue("uri");
		this.resourceTemplate.channel = clientArgs.getOptionValue("channel");
		this.resourceTemplate.ezserver = clientArgs.getOptionValue("ezserver");
		// return this object for convenience
		return this;
	}
	
}
