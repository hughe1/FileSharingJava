package EZShare;

import java.util.ArrayList;

public class Command extends JsonModel {
	
	public String command;
	public String secret;
	public Boolean relay;
	public Resource resource;
	public Resource resourceTemplate;
	public ArrayList<ServerInfo> serverList;
	
	/**
	 * default constructor
	 */
	public Command() {
		
	}
	
	/**
	 * Builds a command based on the clientArgs. Warning it doesn't check for
	 * the case where two major arguments have been passed. i.e., if a publish
	 * and query are both present.
	 * 
	 * @param clientArgs
	 */
	public Command(ClientArgs clientArgs) {
		if(clientArgs.hasOption("publish")) buildPublish(clientArgs);
		else if(clientArgs.hasOption("remove")) buildRemove(clientArgs);
		else if(clientArgs.hasOption("share")) buildShare(clientArgs);
		else if(clientArgs.hasOption("query")) buildQuery(clientArgs);
		else if(clientArgs.hasOption("fetch")) buildFetch(clientArgs);
		else if(clientArgs.hasOption("remove")) buildExchange(clientArgs);
	}
	

	@Override
	public Command fromJson(String json) {
		return g.fromJson(json, Command.class);
	}
	
	/**
	 * Builds a query Command given a ClientArgs object. Use case:
	 * Command command = new Command().buildQuery(...);
	 * 
	 * @param cmd contains input from the user.
	 * @return new ClientArgs object modeling the user input.
	 */
	public Command buildQuery(ClientArgs clientArgs) {
		// ensure that clientArgs contains a query, otherwise exit
		if(!clientArgs.hasOption("query")) clientArgs.printArgsHelp("");
		this.command = "QUERY";
		this.resourceTemplate = new Resource(clientArgs);
		return this;
	}
	
	/**
	 * 
	 * @param clientArgs
	 * @return
	 */
	public Command buildPublish(ClientArgs clientArgs) {
		if(!clientArgs.hasOption("publish")) clientArgs.printArgsHelp("");
		this.command = "PUBLISH";
		this.resource = new Resource(clientArgs);
		return this;
	}
	
	/**
	 * 
	 * @param clientArgs
	 * @return
	 */
	public Command buildExchange(ClientArgs clientArgs) {
		// TODO Auto-generated method stub
		return this;
		
	}
	
	/**
	 * 
	 * @param clientArgs
	 * @return
	 */
	public Command buildFetch(ClientArgs clientArgs) {
		// TODO Auto-generated method stub
		return this;
		
	}

	/**
	 * 
	 * @param clientArgs
	 * @return
	 */
	public Command buildShare(ClientArgs clientArgs) {
		// TODO Auto-generated method stub
		return this;
		
	}
	
	/**
	 * 
	 * @param clientArgs
	 * @return
	 */
	public Command buildRemove(ClientArgs clientArgs) {
		// TODO Auto-generated method stub
		return this;
		
	}
}
