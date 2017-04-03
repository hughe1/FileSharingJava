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
		if (clientArgs.hasOption("publish"))
			buildPublish(clientArgs);
		else if (clientArgs.hasOption("share"))
			buildShare(clientArgs);
		else if (clientArgs.hasOption("query"))
			buildQuery(clientArgs);
		else if (clientArgs.hasOption("fetch"))
			buildFetch(clientArgs);
		else if (clientArgs.hasOption("exchange"))
			buildExchange(clientArgs);
		else if (clientArgs.hasOption("remove"))
			buildRemove(clientArgs);
		else 
			buildInvalid(clientArgs);
	}

	@Override
	public Command fromJson(String json) {
		return g.fromJson(json, Command.class);
	}

	/**
	 * Builds a query Command given a ClientArgs object. Use case: Command
	 * command = new Command().buildQuery(...);
	 * 
	 * @param cmd contains input from the user.
	 * @return new ClientArgs object modeling the user input.
	 */
	public Command buildQuery(ClientArgs clientArgs) {
		// ensure that clientArgs contains a query, otherwise exit
		if (!clientArgs.hasOption("query"))
			clientArgs.printArgsHelp("");
		this.command = "QUERY";
		this.resourceTemplate = new Resource(clientArgs);
		return this;
	}

	/**
	 * 
	 * @param clientArgs
	 * @return self
	 */
	public Command buildPublish(ClientArgs clientArgs) {
		if (!clientArgs.hasOption("publish"))
			clientArgs.printArgsHelp("");
		this.command = "PUBLISH";
		this.resource = new Resource(clientArgs);
		return this;
	}

	/**
	 * 
	 * @param clientArgs
	 * @return self
	 */
	public Command buildExchange(ClientArgs clientArgs) {
		if(!clientArgs.hasOption("exchange"))
			clientArgs.printArgsHelp("");
		this.command = "EXCHANGE";
		try {
			this.addServerList(clientArgs.getOptionValue("servers"));
		} catch (NumberFormatException e) {
			System.out.println(e.getClass().getName() + " " + e.getMessage());
			System.exit(1);
		}
		return this;
	}

	/**
	 * 
	 * @param clientArgs
	 * @return self
	 */
	public Command buildFetch(ClientArgs clientArgs) {
		if(!clientArgs.hasOption("fetch"))
			clientArgs.printArgsHelp("");
		this.command = "FETCH";
		this.resourceTemplate = new Resource(clientArgs);
		return this;
	}

	/**
	 * 
	 * @param clientArgs
	 * @return self
	 */
	public Command buildShare(ClientArgs clientArgs) {
		if(!clientArgs.hasOption("share"))
			clientArgs.printArgsHelp("");
		this.command = "SHARE";
		this.secret = clientArgs.getOptionValue("secret");
		this.resource = new Resource(clientArgs);
		return this;
	}

	/**
	 * 
	 * @param clientArgs
	 * @return
	 */
	public Command buildRemove(ClientArgs clientArgs) {
		if(!clientArgs.hasOption("remove"))
			clientArgs.printArgsHelp("");
		this.command = "REMOVE";
		this.resource = new Resource(clientArgs);
		return this;
	}
	
	public Command buildInvalid(ClientArgs clientArgs) {
		this.command = "INVALID";
		return this;
	}
	
	/**
	 * Convenient method to parse and add details about a server. Usual
	 * use case is for building a SHARE command.
	 * 
	 * @param str has form host:port,host:port,...
	 */
	public void addServerList(String str) throws NumberFormatException {
		if(str == null) return;
		this.serverList = new ArrayList<ServerInfo>();
		String[] addresses = str.split(",");
		for (String token : addresses) {
			// split by ":"
			String[] hostPortToken = token.split(":");
			// get the host in position 0
			String host = hostPortToken[0];
			// get the port in position 1
			int port = Integer.parseInt(hostPortToken[1]);
			// add a new ServerInfo object into the list
			this.serverList.add(new ServerInfo(host,port));
		}
	}
}
