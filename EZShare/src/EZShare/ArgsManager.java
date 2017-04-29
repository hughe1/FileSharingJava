package EZShare;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 * The ArgsManager abstract class represents a list of arguments parsed against
 * a Options descriptor (see org.apache.commons.cli.CommandLine). It is intended
 * to be extended by the ClientArgs and ServerArgs classes.
 * 
 * The abstract class provides the implementation of methods for printing help
 * for an Options descriptor, and for retrieving an options's value as specified
 * in the list of arguments.
 * 
 */
public abstract class ArgsManager {

	protected static Options options = new Options();
	protected static HelpFormatter formatter = new HelpFormatter();
	protected CommandLine cmd;

	/**
	 * The printArgsHelp method prints the help for all options with usage
	 * statements then exits the program.
	 */
	public void printArgsHelp(String msg) {
		// Print to screen the help for options with the specified input msg and
		// automatically generated usage statement
		formatter.printHelp(msg, options, true);
		System.exit(1);
	}

	// TODO AZ: @Bobby I'm not sure about these method comments, could you
	// confirm,
	// not sure why protected access is preferred since there's no package issue
	/**
	 * Wrapper method to access super.cmd. Required if class is used in another
	 * package. i.e. super.cmd has protected access. This method expects the
	 * option to be present.
	 * 
	 * @param option
	 *            the name of the option
	 * @return value of option if cmd.hasOption("channel") == true otherwise it
	 *         calls this.printArgsHelp() -> exits the program.
	 */
	public String getSafeOptionValue(String option) {
		if (!cmd.hasOption(option)) {
			printArgsHelp("Options not correct\n");
		}
		// return value if exists, otherwise null
		return cmd.getOptionValue(option);
	}

	/**
	 * Wrapper method to access super.cmd. Required if class is used in an
	 * external package.
	 * 
	 * @param option
	 *            is the name of the option to look for
	 * @return a String of the value if present, otherwise returns null.
	 */
	public String getOptionValue(String option) {
		return cmd.getOptionValue(option);
	}

	/**
	 * Wrapper method to access super.cmd. Required if class is used in an
	 * external package.
	 * 
	 * @param option
	 *            is the name of the option to look for
	 * @param defaultValue
	 *            is the default value for the option to look for
	 * @return a String of the value if present, otherwise returns the default
	 *         value.
	 */
	public String getOptionValue(String option, String defaultValue) {
		return cmd.getOptionValue(option, defaultValue);
	}

	/**
	 * Wrapper method for base.cmd. Required if this class is used in another
	 * package. super.cmd has protected access.
	 * 
	 * @param the
	 *            name of the "option"
	 * @return true/false
	 */
	public boolean hasOption(String option) {
		return cmd.hasOption(option);
	}
}
