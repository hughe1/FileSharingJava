package EZShare;

import java.util.ArrayList;

/**
 * The Command class represents commands that may be sent from an EZShare client to 
 * an EZShare server.
 * 
 * Only seven formats of commands are constructible (including invalid for testing): 
 * 		QUERY, PUBLISH, FETCH, EXCHANGE, SHARE, REMOVE, INVALID.
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

	/**
	 * Constructs a Command object based on the command line arguments (clientArgs). 
	 * 
	 * Note: 
	 * If less or more than one server command options are included in the 
	 * client arguments (i.e. if publish and query are both present), an INVALID 
	 * Command object will be constructed.
	 * 
	 * @param clientArgs
	 */
	public Command(ClientArgs clientArgs) {
		switch (clientArgs.getCommandOption().toUpperCase()) {
		case PUBLISH_COMMAND:
			buildPublish(clientArgs);
			break;
		case SHARE_COMMAND:
			buildShare(clientArgs);
			break;
		case QUERY_COMMAND:
			buildQuery(clientArgs);
			break;
		case FETCH_COMMAND:
			buildFetch(clientArgs);
			break;
		case EXCHANGE_COMMAND:
			buildExchange(clientArgs);
			break;
		case REMOVE_COMMAND:
			buildRemove(clientArgs);
			break;
		default:
			//No valid command option or multiple commands found
			buildInvalid(clientArgs);
			break;
		}
	}


	/**
	 * Builds a query Command based on the arguments in a given a ClientArgs object. 
	 * Only fields relevant to the command are set according to the client arguments.
	 * 
	 * @param clientArgs
	 * @return a Command object corresponding the the query options provided.
	 */
	public Command buildQuery(ClientArgs clientArgs) {
		this.command = QUERY_COMMAND;
		this.relay = clientArgs.getSafeRelay();
		this.resourceTemplate = new Resource(clientArgs);
		return this;
	}

	/**
	 * Builds a publish Command based on the arguments in a given a ClientArgs object. 
	 * Only fields relevant to the command are set according to the client arguments.
	 * 
	 * @param clientArgs
	 * @return a Command object corresponding the the publish options provided.
	 */
	public Command buildPublish(ClientArgs clientArgs) {
		this.command = PUBLISH_COMMAND;
		this.resource = new Resource(clientArgs);
		return this;
	}

	/**
	 * Builds an exchange Command based on the arguments in a given a ClientArgs object. 
	 * Only fields relevant to the command are set according to the client arguments.
	 * 
	 * @param clientArgs
	 * @return a Command object corresponding the the exchange options provided.
	 */
	public Command buildExchange(ClientArgs clientArgs) {
		this.command = EXCHANGE_COMMAND;
		try {
			this.addServerList(clientArgs.getOptionValue(ClientArgs.SERVERS_OPTION));
		} catch (NumberFormatException e) {
			// server list in the client arguments is of the wrong format
			clientArgs.printArgsHelp("servers options have the wrong format");
		}
		return this;
	}

	/**
	 * Builds a fetch Command based on the arguments in a given a ClientArgs object. 
	 * Only fields relevant to the command are set according to the client arguments.
	 * 
	 * @param clientArgs
	 * @return a Command object corresponding the the fetch options provided.
	 */
	public Command buildFetch(ClientArgs clientArgs) {
		this.command = FETCH_COMMAND;
		this.resourceTemplate = new Resource(clientArgs);
		return this;
	}

	/**
	 * Builds a share Command based on the arguments in a given a ClientArgs object. 
	 * Only fields relevant to the command are set according to the client arguments.
	 * 
	 * @param clientArgs
	 * @return a Command object corresponding the the share options provided.
	 */
	public Command buildShare(ClientArgs clientArgs) {
		this.command = SHARE_COMMAND;
		this.secret = clientArgs.getOptionValue(ClientArgs.SECRET_OPTION);
		this.resource = new Resource(clientArgs);
		return this;
	}

	/**
	 * Builds a remove Command based on the arguments in a given a ClientArgs object. 
	 * Only fields relevant to the command are set according to the client arguments.
	 * 
	 * @param clientArgs
	 * @return a Command object corresponding the the remove options provided.
	 */
	public Command buildRemove(ClientArgs clientArgs) {
		this.command = REMOVE_COMMAND;
		this.resource = new Resource(clientArgs);
		return this;
	}
	
	/**
	 * Builds an invalid Command (mostly for testing purpose). Client can simply
	 * return an error instead, if needed.
	 * 
	 * @param clientArgs
	 * @return a Command object that will generate a command is invalid response from
	 * the server.
	 */
	public Command buildInvalid(ClientArgs clientArgs) {
		this.command = INVALID_COMMAND;
		return this;
	}

	/**
	 * The addServerList is helper method to parse and add details about a server
	 * to the serverList. Normally used when building a SHARE command.
	 * 
	 * @param str
	 *            has the form host:port,host:port,...
	 */
	public void addServerList(String str) throws NumberFormatException {
		if (str == null)
			return;
		this.serverList = new ArrayList<ServerInfo>();
		String[] addresses = str.split(",");
		for (String token : addresses) {
			// split by ":"
			String[] hostPortToken = token.split(":");
			
			// Check for wrongly formatted host:port strings
			if (hostPortToken.length > 1) {
				// get the host in position 0
				String host = hostPortToken[0];
				// get the port in position 1
				int port = Integer.parseInt(hostPortToken[1]);
				// add a new ServerInfo object into the list
				this.serverList.add(new ServerInfo(host, port));
			}
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
