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
		options.addOption(Command.CHANNEL_OPTION, true, "channel");
		options.addOption(Command.DEBUG_OPTION, false, "print debug information");
		options.addOption(Command.DESCRIPTION_OPTION, true, "resource description");
		options.addOption(Command.EXCHANGE_OPTION, false, "exchange server list with server");
		options.addOption(Command.FETCH_OPTION, false, "fetch resources from server");
		options.addOption(Command.HOST_OPTION, true, "server host, a domain name or IP address");
		options.addOption(Command.NAME_OPTION, true, "resource name");
		options.addOption(Command.OWNER_OPTION, true, "owner");
		options.addOption(Command.PORT_OPTION, true, "server port, an integer");
		options.addOption(Command.PUBLISH_OPTION, false, "publish resource on server");
		options.addOption(Command.QUERY_OPTION, false, "query for resources from server");
		options.addOption(Command.REMOVE_OPTION, false, "remove resource from server");
		options.addOption(Command.SECRET_OPTION, true, "secret");
		options.addOption(Command.SERVERS_OPTION, true, "server list, host1:port1,host2:port2,...");
		options.addOption(Command.SHARE_OPTION, false, "share resource on server");
		options.addOption(Command.TAGS_OPTION, true, "resource tags, tag1,tag2,tag3,...");
		options.addOption(Command.URI_OPTION, true, "resource URI");
		options.addOption(Command.RELAY_OPTION, true, "relay Query");
		
		// Attempts to parse the args, in case of parse exception print help menu and exit
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

}
