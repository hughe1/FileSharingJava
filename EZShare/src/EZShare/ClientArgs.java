package EZShare;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

public class ClientArgs extends ArgsManager {

	/**
	 * 
	 * @param args
	 */
	public ClientArgs(String[] args) {
		// builds the client argument options
		this.options.addOption("channel", true, "channel");
		this.options.addOption("debug", false, "print debug information");
		this.options.addOption("description", true, "resource description");
		this.options.addOption("exchange", false, "exchange server list with server");
		this.options.addOption("fetch", false, "fetch resources from server");
		this.options.addOption("host", true, "server host, a domain name or IP address");
		this.options.addOption("name", true, "resource name");
		this.options.addOption("owner", true, "owner");
		this.options.addOption("port", true, "server port, an integer");
		this.options.addOption("publish", false, "publish resource on server");
		this.options.addOption("query", false, "query for resources from server");
		this.options.addOption("remove", false, "remove resource from server");
		this.options.addOption("secret", true, "secret");
		this.options.addOption("servers", true, "server list, host1:port1,host2:port2,...");
		this.options.addOption("share", false, "share resource on server");
		this.options.addOption("tags", true, "resource tags, tag1,tag2,tag3,...");
		this.options.addOption("uri", true, "resource URI");
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
		if (!this.hasOption("port")) {
			return 3780; // default port
		}
		return Integer.parseInt(this.getOptionValue("port"));
	}

	/**
	 * 
	 * @return
	 */
	public String getSafeHost() {
		if (!this.hasOption("host")) {
			return "localhost"; // default host
		}
		return this.getOptionValue("host");
	}

}
