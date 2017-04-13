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
		this.options.addOption(Constants.advertisedHostNameOption, true, "advertised hostname");
		this.options.addOption(Constants.connectionIntervalLimitOption, true, "connection interval limit in seconds");
		this.options.addOption(Constants.portOption, true, "server port, an integer");
		this.options.addOption(Constants.secretOption, true, "secret");
		this.options.addOption(Constants.debugOption, false, "print debug information");
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
		if (!this.hasOption(Constants.portOption)) {
			return 3780; // default port
		}
		return Integer.parseInt(this.getOptionValue(Constants.portOption));
	}

	/**
	 * 
	 * @return
	 */
	public String getSafeHost() {
		if (!this.hasOption(Constants.hostOption)) {
			// "The default advertised host name will be the operating system supplied hostname."
			try {
				return InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				Logger logger = LogManager.getRootLogger();
				logger.error(e.getClass().getName() + " " + e.getMessage());
			} 
			return "localhost"; // default host
		}
		return this.getOptionValue(Constants.hostOption);
	}
	
	/**
	 * 
	 * @return
	 */
	public String getSafeSecret() {
		if (!this.hasOption(Constants.secretOption)) {
			// "The default secret will be a large random string."
			return UUID.randomUUID().toString();
		}
		return this.getOptionValue(Constants.secretOption);
	}
	
	/**
	 * 
	 * @return
	 */
	public int getSafeExchangeInterval() {
		if (!this.hasOption(Constants.exchangeIntervalOption)) {
			return 600; 
		}
		return Integer.parseInt(this.getOptionValue(Constants.exchangeIntervalOption));
	}
	
	/**
	 * 
	 * @return
	 */
	public int getSafeConnectionInterval() {
		if (!this.hasOption(Constants.connectionIntervalLimitOption)) {
			return 1; 
		}
		return Integer.parseInt(this.getOptionValue(Constants.connectionIntervalLimitOption));
	}
}
