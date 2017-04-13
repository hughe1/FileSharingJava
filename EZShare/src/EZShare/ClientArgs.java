package EZShare;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientArgs extends ArgsManager {

	/**
	 * 
	 * @param args
	 */
	public ClientArgs(String[] args) {
		// builds the client argument options
		this.options.addOption(Constants.channelOption, true, "channel");
		this.options.addOption(Constants.debugOption, false, "print debug information");
		this.options.addOption(Constants.descriptionOption, true, "resource description");
		this.options.addOption(Constants.exchangeOption, false, "exchange server list with server");
		this.options.addOption(Constants.fetchOption, false, "fetch resources from server");
		this.options.addOption(Constants.hostOption, true, "server host, a domain name or IP address");
		this.options.addOption(Constants.nameOption, true, "resource name");
		this.options.addOption(Constants.ownerOption, true, "owner");
		this.options.addOption(Constants.portOption, true, "server port, an integer");
		this.options.addOption(Constants.publishOption, false, "publish resource on server");
		this.options.addOption(Constants.queryOption, false, "query for resources from server");
		this.options.addOption(Constants.removeOption, false, "remove resource from server");
		this.options.addOption(Constants.secretOption, true, "secret");
		this.options.addOption(Constants.serversOption, true, "server list, host1:port1,host2:port2,...");
		this.options.addOption(Constants.shareOption, false, "share resource on server");
		this.options.addOption(Constants.tagsOption, true, "resource tags, tag1,tag2,tag3,...");
		this.options.addOption(Constants.uriOption, true, "resource URI");
		this.options.addOption(Constants.relayOption, true, "relay Query");
		// attempts to parse the args otherwise print help menu and exit
		try {
			this.cmd = new DefaultParser().parse(options, args);
			// see if at least one argument was provided
			if (args.length == 0)
				throw new ParseException("zero arguments supplied");
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			this.printArgsHelp("Client");
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

}
