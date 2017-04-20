package EZShare;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The ClientArgs class represents a list of command-line arguments parsed against 
 * an Option descriptor containing the commands/options known to an EZShare client.
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
	public static final String URI_OPTION = "uri";
	public static final String SECRET_OPTION = "secret";
	public static final String TAGS_OPTION = "tags";
	public static final String REMOVE_OPTION = "remove";
	public static final String SERVERS_OPTION = "servers";
	public static final String SHARE_OPTION = "share";
	public static final String EXCHANGE_OPTION = "exchange";
	public static final String FETCH_OPTION = "fetch";

	public static final Integer DEFAULT_PORT = 3780;
	
	/**
	 * The static initializer adds all commands/options known to an EZShare client 
	 * to the class' Option descriptor.
	 */
	static {
		//"options" is static, thus requires one-time initialization
		// AF Had to comment out the if below because server needs to access 
		// clientArgs when exchanging and options was set to serverArgs
		//if (options.getOptions().isEmpty()) {
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
		//}
	}
	
	/**
	 * Constructor that parses the command-line arguments against the client 
	 * commands descriptor to initialize the object's CommandLine variable (cmd).
	 * @param args
	 */
	public ClientArgs(String[] args) {		
		//Try to parse the command line arguments string against known options
		try {
			cmd = new DefaultParser().parse(options, args);
			
		} catch (ParseException e) {
			//unknown or no commands/options provided
			System.out.println(e.getMessage());
			this.printArgsHelp("Client"); //print Client help menu and exit
		}
	}
	
	/**
	 * The getCommandOption method returns the server command (i.e. publish, share, 
	 * query, fetch, exchange, remove) that is set by the client arguments. 
	 * If more than one command option is set, "INVALID" will be returned.
	 * @return the server command option set in the calling CliengArgs object. 
	 * If more than one command option is used, "INVALID" will be returned.
	 */
	public String getCommandOption(){
		int num_command = 0;
		String command = "INVALID";
		
		if(hasOption(PUBLISH_OPTION)){
			num_command++;
			command = PUBLISH_OPTION;
		}
		
		if(hasOption(SHARE_OPTION)){
			num_command++;
			command = SHARE_OPTION;
		}
		
		if(hasOption(QUERY_OPTION)){
			num_command++;
			command = QUERY_OPTION;
		}
		
		if(hasOption(FETCH_OPTION)){
			num_command++;
			command = FETCH_OPTION;
		}
		
		if(hasOption(EXCHANGE_OPTION)){
			num_command++;
			command = EXCHANGE_OPTION;
		}
		
		if(hasOption(REMOVE_OPTION)){
			num_command++;
			command = REMOVE_OPTION;
		}
		
		//final check on multiple command options
		if(num_command > 1) {
			command = "INVALID";
		}
		return command;
	}

	/**
	 * The getSafePort method returns the server port specified in the command-line
	 * arguments
	 * @return the server port specified in the client arguments, if a port was not
	 * specified, the default port is returned
	 */
	public Integer getSafePort() {
		if (!this.hasOption(PORT_OPTION)) {
			return DEFAULT_PORT; // default port
		}
		return Integer.parseInt(this.getOptionValue(PORT_OPTION));
	}

	/**
	 * The getSafeHost method returns the server host specified in the command-line
	 * arguments
	 * @return the server host specified in the client arguments, 
	 */
	public String getSafeHost() {
		if (!this.hasOption(HOST_OPTION)) {
			// "The default advertised host name will be the operating system supplied hostname."
			try {
				return InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				Logger logger = LogManager.getRootLogger();
				logger.error(e.getClass().getName() + " " + e.getMessage());
				return "localhost"; // default if failed to retrieve OS host name
			} 
		}
		return this.getOptionValue(HOST_OPTION);
	}

}
