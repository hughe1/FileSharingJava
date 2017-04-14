package EZShare;

import java.util.ArrayList;

/**
 * The Command class represents message that may be sent to a server.
 * 
 * Note:
 * Null fields are NOT part of the message and will be disregarded when converting 
 * to JSON.
 */
public class Command extends JsonModel {
	
	/* Defined commands recognized by the server */
	public static final String QUERY_COMMAND = "QUERY";
	public static final String PUBLISH_COMMAND = "PUBLISH";
	public static final String FETCH_COMMAND = "FETCH";
	public static final String EXCHANGE_COMMAND = "EXCHANGE";
	public static final String SHARE_COMMAND = "SHARE";
	public static final String REMOVE_COMMAND = "REMOVE";
	public static final String INVALID_COMMAND = "INVALID";

	/* Defined command options recognized by the server */
	public static final String ADVERTISED_HOST_NAME_OPTION = "advertisedhostname";
	public static final String CHANNEL_OPTION = "channel";
	public static final String CONNECTION_INTERVAL_LIMIT_OPTION = "connectionintervallimit";
	public static final String DEBUG_OPTION = "debug";
	public static final String DESCRIPTION_OPTION = "description";
	public static final String EXCHANGE_OPTION = "exchange";
	public static final String EXCHANGE_INTERVAL_OPTION = "exchangeInterval";
	public static final String EZSERVER_OPTION = "ezserver";
	public static final String FETCH_OPTION = "fetch";
	public static final String HOST_OPTION = "host";
	public static final String NAME_OPTION = "name";
	public static final String OWNER_OPTION = "owner";
	public static final String PORT_OPTION = "port";
	public static final String PUBLISH_OPTION = "publish";
	public static final String QUERY_OPTION = "query";
	public static final String RELAY_OPTION = "relay";
	public static final String RESOURCE_OPTION = "resource";
	public static final String RESOURCE_TEMPLATE_OPTION = "resourceTemplate";
	public static final String REMOVE_OPTION = "remove";
	public static final String SECRET_OPTION = "secret";
	public static final String SERVERS_OPTION = "servers";
	public static final String SHARE_OPTION = "share";
	public static final String TAGS_OPTION = "tags";
	public static final String URI_OPTION = "uri";
	
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
	 * Constructs a command based on the command line arguments (clientArgs). 
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
		if (clientArgs.hasOption(PUBLISH_OPTION))
			buildPublish(clientArgs);
		else if (clientArgs.hasOption(SHARE_OPTION))
			buildShare(clientArgs);
		else if (clientArgs.hasOption(QUERY_OPTION))
			buildQuery(clientArgs);
		else if (clientArgs.hasOption(FETCH_OPTION))
			buildFetch(clientArgs);
		else if (clientArgs.hasOption(EXCHANGE_OPTION))
			buildExchange(clientArgs);
		else if (clientArgs.hasOption(REMOVE_OPTION))
			buildRemove(clientArgs);
		else
			buildInvalid(clientArgs);
	}

	

	/**
	 * Builds a query Command given a ClientArgs object. Use case: Command
	 * command = new Command().buildQuery(...);
	 * 
	 * @param cmd
	 *            contains input from the user.
	 * @return new ClientArgs object modelling the user input.
	 */
	public Command buildQuery(ClientArgs clientArgs) {
		// ensure that clientArgs contains a query, otherwise exit
		if (!clientArgs.hasOption(QUERY_OPTION))
			clientArgs.printArgsHelp("");
		this.command = QUERY_COMMAND;
		this.relay = clientArgs.hasOption(RELAY_OPTION)
				? java.lang.Boolean.parseBoolean(clientArgs.getOptionValue(RELAY_OPTION)) : true;
		this.resourceTemplate = new Resource(clientArgs);
		return this;
	}

	/**
	 * 
	 * @param clientArgs
	 * @return self
	 */
	public Command buildPublish(ClientArgs clientArgs) {
		if (!clientArgs.hasOption(PUBLISH_OPTION))
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
		if (!clientArgs.hasOption(EXCHANGE_OPTION))
			clientArgs.printArgsHelp("");
		this.command = EXCHANGE_COMMAND;
		try {
			this.addServerList(clientArgs.getOptionValue(SERVERS_OPTION));
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
		if (!clientArgs.hasOption(FETCH_OPTION))
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
		if (!clientArgs.hasOption(SHARE_OPTION))
			clientArgs.printArgsHelp("");
		this.command = SHARE_COMMAND;
		this.secret = clientArgs.getOptionValue(SECRET_OPTION);
		this.resource = new Resource(clientArgs);
		return this;
	}

	/**
	 * 
	 * @param clientArgs
	 * @return
	 */
	public Command buildRemove(ClientArgs clientArgs) {
		if (!clientArgs.hasOption(REMOVE_OPTION))
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
