package EZShare;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The ClientArgs class represents a list of command line arguments parsed against 
 * an Option descriptor containing the commands and options known to an EZShare server.
 */
public class ClientArgs extends ArgsManager {

	/* Defined command options recognized by the client */
	public static final String CHANNEL_OPTION = "channel";
	public static final String DEBUG_OPTION = "debug";
	public static final String DESCRIPTION_OPTION = "description";
	public static final String EZSERVER_OPTION = "ezserver";
	public static final String HOST_OPTION = "host";
	public static final String NAME_OPTION = "name";
	public static final String OWNER_OPTION = "owner";
	public static final String PORT_OPTION = "port";
	public static final String PUBLISH_OPTION = "publish";
	public static final String QUERY_OPTION = "query";
	public static final String RELAY_OPTION = "relay";
	public static final String URI_OPTION = "uri";
	public static final String SECRET_OPTION = "secret";
	public static final String TAGS_OPTION = "tags";
	public static final String REMOVE_OPTION = "remove";
	public static final String SERVERS_OPTION = "servers";
	public static final String SHARE_OPTION = "share";
	public static final String EXCHANGE_OPTION = "exchange";
	public static final String FETCH_OPTION = "fetch";

	/**
	 * Constructor which initializes the Option descriptor with EZShare server 
	 * command/options and parse the command line string against the descriptor
	 * to intialize the .
	 * @param args
	 */
	public ClientArgs(String[] args) {
		
		//initialize the Options descriptor
		initializeClientOptions();
		
		//Try to parse the command line arguments string against known options
		try {
			this.cmd = new DefaultParser().parse(options, args);
			
			// check if at least one argument was provided
			if (args.length == 0)
				throw new ParseException("Zero arguments supplied");
		
		} catch (ParseException e) {
			//unknown or no commands/options provided
			System.out.println(e.getMessage());
			this.printArgsHelp("Client"); //print Client help menu and exit
		}
	}

	/**
	 * The intializeEZShareOptions method adds all commands/options known to an 
	 * EZShare client to the object's Option descriptor.
	 */
	public void initializeClientOptions(){
		//static variable inherited from ArgsManager, requires one-time initialization
		if (options.getOptions().isEmpty()) {
			options.addOption(CHANNEL_OPTION, true, "channel");
			options.addOption(DEBUG_OPTION, false, "print debug information");
			options.addOption(DESCRIPTION_OPTION, true, "resource description");
			options.addOption(EXCHANGE_OPTION, false, "exchange server list with server");
			options.addOption(FETCH_OPTION, false, "fetch resources from server");
			options.addOption(HOST_OPTION, true, "server host, a domain name or IP address");
			options.addOption(NAME_OPTION, true, "resource name");
			options.addOption(OWNER_OPTION, true, "owner");
			options.addOption(PORT_OPTION, true, "server port, an integer");
			options.addOption(PUBLISH_OPTION, false, "publish resource on server");
			options.addOption(QUERY_OPTION, false, "query for resources from server");
			options.addOption(REMOVE_OPTION, false, "remove resource from server");
			options.addOption(SECRET_OPTION, true, "secret");
			options.addOption(SERVERS_OPTION, true, "server list, host1:port1,host2:port2,...");
			options.addOption(SHARE_OPTION, false, "share resource on server");
			options.addOption(TAGS_OPTION, true, "resource tags, tag1,tag2,tag3,...");
			options.addOption(URI_OPTION, true, "resource URI");
			options.addOption(RELAY_OPTION, true, "relay Query");
		}
	}
	
	
	/**
	 * 
	 * @return
	 */
	public Integer getSafePort() {
		if (!this.hasOption(PORT_OPTION)) {
			return 3780; // default port
		}
		return Integer.parseInt(this.getOptionValue(PORT_OPTION));
	}

	/**
	 * 
	 * @return
	 */
	public String getSafeHost() {
		if (!this.hasOption(HOST_OPTION)) {
			// "The default advertised host name will be the operating system supplied hostname."
			try {
				return InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				Logger logger = LogManager.getRootLogger();
				logger.error(e.getClass().getName() + " " + e.getMessage());
			} 
			return "localhost"; // default host
		}
		return this.getOptionValue(HOST_OPTION);
	}

}
