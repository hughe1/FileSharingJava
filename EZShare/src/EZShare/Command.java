package EZShare;

import java.util.ArrayList;

/**
 * The Command class represents commands that may be sent from an EZShare client to 
 * an EZShare server.
 * 
 * Only seven formats of commands are constructible (including one invalid format): 
 * 	QUERY, PUBLISH, FETCH, EXCHANGE, SHARE, REMOVE, INVALID.
 * 
 * Fields can only be initialize using the build methods to ensure correct formatting.
 * 
 * Note:
 * Upon construction, all fields are set to null. Null fields are NOT part of the 
 * command and will be disregarded when converting to JSON for communication.
 */
public class Command extends JsonModel {
	
	/* Defined commands recognized by the server */
	public static final String QUERY_COMMAND = "QUERY";
	public static final String PUBLISH_COMMAND = "PUBLISH";
	public static final String FETCH_COMMAND = "FETCH";
	public static final String EXCHANGE_COMMAND = "EXCHANGE";
	public static final String SHARE_COMMAND = "SHARE";
	public static final String REMOVE_COMMAND = "REMOVE";
	
	public static final String INVALID_COMMAND = "INVALID"; //TODO: AZ - I think the client should simply reject this from the get go

	public static final String RESOURCE_OPTION = "resource";
	public static final String RESOURCE_TEMPLATE_OPTION = "resourceTemplate";
	
	private String command;
	private String secret;
	private Boolean relay;
	private Resource resource;
	private Resource resourceTemplate;
	private ArrayList<ServerInfo> serverList;
	
	/**
	 * Default constructor
	 */
	public Command() {
	}

	//TODO AZ - probably should do something about that multiple command arguments scenario...
	/**
	 * Constructs a Command object based on the command line arguments (clientArgs). 
	 * 
	 * Note: 
	 * Where two major options are included in the argument (i.e. if publish and 
	 * query are both present), the method will not return an error, one of the 
	 * argument will be executed, based on the following order:
	 * 		public > share > query > fetch > exchange > remove
	 * 
	 * @param clientArgs
	 */
	public Command(ClientArgs clientArgs) {
		if (clientArgs.hasOption(ClientArgs.PUBLISH_OPTION))
			buildPublish(clientArgs);
		else if (clientArgs.hasOption(ClientArgs.SHARE_OPTION))
			buildShare(clientArgs);
		else if (clientArgs.hasOption(ClientArgs.QUERY_OPTION))
			buildQuery(clientArgs);
		else if (clientArgs.hasOption(ClientArgs.FETCH_OPTION))
			buildFetch(clientArgs);
		else if (clientArgs.hasOption(ClientArgs.EXCHANGE_OPTION))
			buildExchange(clientArgs);
		else if (clientArgs.hasOption(ClientArgs.REMOVE_OPTION))
			buildRemove(clientArgs);
		else
			buildInvalid(clientArgs);
	}


	/**
	 * Builds a query Command given a ClientArgs object. Only options relevant
	 * t
	 * 
	 * @param cmd
	 *            contains input from the user.
	 * @return new ClientArgs object modelling the user input.
	 */
	public Command buildQuery(ClientArgs clientArgs) {
		// ensure that clientArgs contains a query, otherwise exit
		if (!clientArgs.hasOption(ClientArgs.QUERY_OPTION))
			clientArgs.printArgsHelp("");
		this.command = QUERY_COMMAND;
		this.relay = clientArgs.hasOption(ClientArgs.RELAY_OPTION)
				? java.lang.Boolean.parseBoolean(clientArgs.getOptionValue(ClientArgs.RELAY_OPTION)) : true;
		this.resourceTemplate = new Resource(clientArgs);
		return this;
	}

	/**
	 * 
	 * @param clientArgs
	 * @return self
	 */
	public Command buildPublish(ClientArgs clientArgs) {
		if (!clientArgs.hasOption(ClientArgs.PUBLISH_OPTION))
			clientArgs.printArgsHelp("");
		this.command = PUBLISH_COMMAND;
		this.resource = new Resource(clientArgs);
		return this;
	}

	/**
	 * 
	 * @param clientArgs
	 * @return self
	 */
	public Command buildExchange(ClientArgs clientArgs) {
		if (!clientArgs.hasOption(ClientArgs.EXCHANGE_OPTION))
			clientArgs.printArgsHelp("");
		this.command = EXCHANGE_COMMAND;
		try {
			this.addServerList(clientArgs.getOptionValue(ClientArgs.SERVERS_OPTION));
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
		if (!clientArgs.hasOption(ClientArgs.FETCH_OPTION))
			clientArgs.printArgsHelp("");
		this.command = FETCH_COMMAND;
		this.resourceTemplate = new Resource(clientArgs);
		return this;
	}

	/**
	 * 
	 * @param clientArgs
	 * @return self
	 */
	public Command buildShare(ClientArgs clientArgs) {
		if (!clientArgs.hasOption(ClientArgs.SHARE_OPTION))
			clientArgs.printArgsHelp("");
		this.command = SHARE_COMMAND;
		this.secret = clientArgs.getOptionValue(ClientArgs.SECRET_OPTION);
		this.resource = new Resource(clientArgs);
		return this;
	}

	/**
	 * 
	 * @param clientArgs
	 * @return
	 */
	public Command buildRemove(ClientArgs clientArgs) {
		if (!clientArgs.hasOption(ClientArgs.REMOVE_OPTION))
			clientArgs.printArgsHelp("");
		this.command = REMOVE_COMMAND;
		this.resource = new Resource(clientArgs);
		return this;
	}

	public Command buildInvalid(ClientArgs clientArgs) {
		this.command = INVALID_COMMAND;
		return this;
	}

	/**
	 * Convenient method to parse and add details about a server. Usual use case
	 * is for building a SHARE command.
	 * 
	 * @param str
	 *            has form host:port,host:port,...
	 */
	public void addServerList(String str) throws NumberFormatException {
		if (str == null)
			return;
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
			this.serverList.add(new ServerInfo(host, port));
		}
	}
	
	/* Getters and Setters for accessing instance variables */
	public String getCommand(){
		return command;
	}
	
	
	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public Boolean getRelay() {
		return relay;
	}

	public void setRelay(Boolean relay) {
		this.relay = relay;
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public Resource getResourceTemplate() {
		return resourceTemplate;
	}

	public void setResourceTemplate(Resource resourceTemplate) {
		this.resourceTemplate = resourceTemplate;
	}

	public ArrayList<ServerInfo> getServerList() {
		return serverList;
	}

	public void setServerList(ArrayList<ServerInfo> serverList) {
		this.serverList = serverList;
	}

	@Override
	public Command fromJson(String json) {
		return g.fromJson(json, Command.class);
	}
}
