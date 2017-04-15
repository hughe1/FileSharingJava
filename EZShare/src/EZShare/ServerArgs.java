package EZShare;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerArgs extends ArgsManager {

	/* Defined command options recognized by the server */
	public static final String ADVERTISED_HOST_NAME_OPTION = "advertisedhostname";
	public static final String CONNECTION_INTERVAL_LIMIT_OPTION = "connectionintervallimit";
	public static final String DEBUG_OPTION = "debug";
	public static final String EXCHANGE_INTERVAL_OPTION = "exchangeInterval";
	public static final String PORT_OPTION = "port";
	public static final String SECRET_OPTION = "secret";
	
	
	public ServerArgs(String[] args) {
		/**
		 * add all of the argument options for the server
		 */
		options.addOption(ADVERTISED_HOST_NAME_OPTION, true, "advertised hostname");
		options.addOption(CONNECTION_INTERVAL_LIMIT_OPTION, true, "connection interval limit in seconds");
		options.addOption(PORT_OPTION, true, "server port, an integer");
		options.addOption(SECRET_OPTION, true, "secret");
		options.addOption(DEBUG_OPTION, false, "print debug information");
		
		
		/**
		 * try and parse the options, otherwise print a help menu and exit
		 */
		try {
			this.cmd = new DefaultParser().parse(options, args);

			// Alex: Don't think this is necessary since server should able to
			// be started with just java -cp ezshare.jar EZShare.Server
			// check to see if at least one argument was provided
			// if (args.length == 0) throw new ParseException("zero arguments
			// were supplied");
		} catch (ParseException e) {
			// Alex: Don't think this is necessary since server should able to
			// be started with just java -cp ezshare.jar EZShare.Server
			// this.printArgsHelp("Server: zero arguments were supplied\n");
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
		if (!this.hasOption(ClientArgs.HOST_OPTION)) {
			// "The default advertised host name will be the operating system supplied hostname."
			try {
				return InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				Logger logger = LogManager.getRootLogger();
				logger.error(e.getClass().getName() + " " + e.getMessage());
			} 
			return "localhost"; // default host
		}
		return this.getOptionValue(ClientArgs.HOST_OPTION);
	}
	
	/**
	 * 
	 * @return
	 */
	public String getSafeSecret() {
		if (!this.hasOption(SECRET_OPTION)) {
			// Generate a default secret which is a large random string.
			return UUID.randomUUID().toString();
		}
		return this.getOptionValue(SECRET_OPTION);
	}
	
	/**
	 * 
	 * @return
	 */
	public int getSafeExchangeInterval() {
		if (!this.hasOption(EXCHANGE_INTERVAL_OPTION)) {
			return 600; 
		}
		return Integer.parseInt(this.getOptionValue(EXCHANGE_INTERVAL_OPTION));
	}
	
	/**
	 * 
	 * @return
	 */
	public int getSafeConnectionInterval() {
		if (!this.hasOption(CONNECTION_INTERVAL_LIMIT_OPTION)) {
			return 1; 
		}
		return Integer.parseInt(this.getOptionValue(CONNECTION_INTERVAL_LIMIT_OPTION));
	}
}
