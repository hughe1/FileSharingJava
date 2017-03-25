import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Project used to learn & test the Apache Commons CLI API.
 * download: https://commons.apache.org/proper/commons-cli/download_cli.cgi
 * documentation (1.4): https://commons.apache.org/proper/commons-cli/apidocs/index.html
 * add to project: project Properties > java build path > libraries > add external JARs
 */

public class TestCLI {

	public static void main(String[] args) {
		Options options = new Options();
		
		/**
		 * @param: "t" the name of the option
		 * @param: false, if the options has an argument
		 * @param: "string" description
		 */
		options.addOption("t", true, "display current time");
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			help(options);
		}
		
		if(cmd.hasOption("t")) {
			System.out.println("option t given: " + cmd.getOptionValue("t"));			
		}
		else {
			help(options);
		}
	}
	
	public static void help(Options options) {
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("Main", options);
		System.exit(1);
	}

}
