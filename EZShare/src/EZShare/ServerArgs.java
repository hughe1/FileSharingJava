package EZShare;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The ServerArgs class represents a list of command-line arguments parsed against 
 * an Option descriptor containing the commands/options known to an EZShare server.
 */
public class ServerArgs extends ArgsManager {

	/* Defined command options recognized by the server */
	public static final String ADVERTISED_HOST_NAME_OPTION = "advertisedhostname";
	public static final String CONNECTION_INTERVAL_LIMIT_OPTION = "connectionintervallimit";
	public static final String DEBUG_OPTION = "debug";
	public static final String EXCHANGE_INTERVAL_OPTION = "exchangeinterval";
	public static final String PORT_OPTION = "port";
	public static final String SECRET_OPTION = "secret";

	public static final Integer DEFAULT_PORT = 3780;
	public static final String DEFAULT_SECRET = UUID.randomUUID().toString(); // Large random string.
	public static String DEFAULT_HOST = "";
	public static final Integer DEFAULT_SAFE_EXCHANGE_INTERVAL = 600; //10 min
	public static final Integer DEFAULT_SAFE_CONNECTION_INTERVAL = 1;
	
	static {
		//"options" is static, thus requires one-time initialization
		options.addOption(ADVERTISED_HOST_NAME_OPTION, true, "advertised hostname");
		options.addOption(CONNECTION_INTERVAL_LIMIT_OPTION, true, "connection interval limit in seconds");
		options.addOption(EXCHANGE_INTERVAL_OPTION, true, "exchange interval in seconds");
		options.addOption(PORT_OPTION, true, "server port, an integer");
		options.addOption(SECRET_OPTION, true, "secret");
		options.addOption(DEBUG_OPTION, false, "print debug information");
	}
	
	
	public ServerArgs(String[] args) {
		//Try to parse the command line arguments string against known options
		try {
			this.cmd = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			//unknown commands/options provided
			System.out.println(e.getMessage());
			this.printArgsHelp("Server"); //print Server help menu and exit
		}
	}

	
	/**
	 * The getSafePort method returns the server port specified in the command-line
	 * arguments
	 * @return the server port specified in the server arguments, if a port was not
	 * specified, the default port is returned
	 */
	public Integer getSafePort() {
		if (!this.hasOption(PORT_OPTION)) {
			return DEFAULT_PORT;
		}
		return Integer.parseInt(this.getOptionValue(PORT_OPTION));
	}

	
	//TODO AZ: Please confirm if this method should be removed, the Server does
	//not read in a host option and the default name should be stored in Server,
	//not ServerArgs
	/**
	 * 
	 * @return
	 */
	public String getSafeHost() {
		if (!this.hasOption(ServerArgs.ADVERTISED_HOST_NAME_OPTION)) {
			if (DEFAULT_HOST.equals("")) {
				// "The default advertised host name will be the operating system supplied hostname."
				try {
					DEFAULT_HOST = InetAddress.getLocalHost().getHostName();
				} catch (UnknownHostException e) {
					DEFAULT_HOST = "localhost";
					Logger logger = LogManager.getRootLogger();
					logger.error(e.getClass().getName() + " " + e.getMessage());
				} 
			}
			return DEFAULT_HOST;
		}
		return this.getOptionValue(ServerArgs.ADVERTISED_HOST_NAME_OPTION);
	}
	
	/**
	 * The getSafeSecret method returns the secret specified in the command-line
	 * arguments
	 * @return the secret specified in the server arguments, if a secret was not
	 * specified, the default secret is generated an returned
	 */
	public String getSafeSecret() {
		if (!this.hasOption(SECRET_OPTION)) {
			return DEFAULT_SECRET;
		}
		return this.getOptionValue(SECRET_OPTION);
	}
	
	/**
	 * The getSafeExchangeInterval method returns the exchange interval specified in
	 * the command-line arguments
	 * @return the interval specified in the server arguments (in seconds), if 
	 * an interval is not specified, the default value is returned
	 */
	public int getSafeExchangeInterval() {
		if (!this.hasOption(EXCHANGE_INTERVAL_OPTION)) {
			return DEFAULT_SAFE_EXCHANGE_INTERVAL; 
		}
		return Integer.parseInt(this.getOptionValue(EXCHANGE_INTERVAL_OPTION));
	}
	
	/**
	 * The getSafeConnectionInterval method returns the connection interval specified
	 * in the command-line arguments
	 * @return the interval specified in the server arguments (in seconds), if 
	 * an interval is not specified, the default value is returned
	 */
	public int getSafeConnectionInterval() {
		if (!this.hasOption(CONNECTION_INTERVAL_LIMIT_OPTION)) {
			return DEFAULT_SAFE_CONNECTION_INTERVAL; 
		}
		return Integer.parseInt(this.getOptionValue(CONNECTION_INTERVAL_LIMIT_OPTION));
	}
}
