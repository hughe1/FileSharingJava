package EZShare;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerArgs extends ArgsManager {

	public ServerArgs(String[] args) {
		/**
		 * add all of the argument options for the server
		 */
		options.addOption(Command.ADVERTISED_HOST_NAME_OPTION, true, "advertised hostname");
		options.addOption(Command.CONNECTION_INTERVAL_LIMIT_OPTION, true, "connection interval limit in seconds");
		options.addOption(Command.PORT_OPTION, true, "server port, an integer");
		options.addOption(Command.SECRET_OPTION, true, "secret");
		options.addOption(Command.DEBUG_OPTION, false, "print debug information");
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
		if (!this.hasOption(Command.PORT_OPTION)) {
			return 3780; // default port
		}
		return Integer.parseInt(this.getOptionValue(Command.PORT_OPTION));
	}

	/**
	 * 
	 * @return
	 */
	public String getSafeHost() {
		if (!this.hasOption(Command.HOST_OPTION)) {
			// "The default advertised host name will be the operating system supplied hostname."
			try {
				return InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				Logger logger = LogManager.getRootLogger();
				logger.error(e.getClass().getName() + " " + e.getMessage());
			} 
			return "localhost"; // default host
		}
		return this.getOptionValue(Command.HOST_OPTION);
	}
	
	/**
	 * 
	 * @return
	 */
	public String getSafeSecret() {
		if (!this.hasOption(Command.SECRET_OPTION)) {
			// Generate a default secret which is a large random string.
			return UUID.randomUUID().toString();
		}
		return this.getOptionValue(Command.SECRET_OPTION);
	}
	
	/**
	 * 
	 * @return
	 */
	public int getSafeExchangeInterval() {
		if (!this.hasOption(Command.EXCHANGE_INTERVAL_OPTION)) {
			return 600; 
		}
		return Integer.parseInt(this.getOptionValue(Command.EXCHANGE_INTERVAL_OPTION));
	}
	
	/**
	 * 
	 * @return
	 */
	public int getSafeConnectionInterval() {
		if (!this.hasOption(Command.CONNECTION_INTERVAL_LIMIT_OPTION)) {
			return 1; 
		}
		return Integer.parseInt(this.getOptionValue(Command.CONNECTION_INTERVAL_LIMIT_OPTION));
	}
}
