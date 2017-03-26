package EZShare;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

public class ServerArgs extends ArgsManager {

	public ServerArgs(String[] args) {
		/**
		 * add all of the argument options for the server 
		 */
		this.options.addOption("advertisedhostname",true,"advertised hostname");
		this.options.addOption("connectionintervallimit",true,"connection interval limit in seconds");
		this.options.addOption("port", true, "server port, an integer");
		this.options.addOption("secret", true, "secret");
		this.options.addOption("debug",false,"print debug information");		
		/**
		 * try and parse the options, otherwise print a help menu and exit
		 */
		try {
			this.cmd = new DefaultParser().parse(options, args);
			// check to see if at least one argument was provided
			if (args.length == 0) throw new ParseException("zero arguments were supplied");
		} catch (ParseException e) {
			this.printArgsHelp();
		}
	}
	
	@Override
	public void printArgsHelp() {
		// print the help menu
		this.formater.printHelp("Server", this.options);
		// exit the program entirely
		System.exit(1);
	}

}
