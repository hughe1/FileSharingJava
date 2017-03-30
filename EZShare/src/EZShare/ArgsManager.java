package EZShare;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public abstract class ArgsManager {
	
	protected Options options = new Options();
	protected HelpFormatter formater = new HelpFormatter();
	protected CommandLine cmd = null;
	
	/**
	 * 
	 */
	public void printArgsHelp(String msg) {
		// print to screen the arguments
		this.formater.printHelp(msg, this.options);
		// exit the program entirely
		System.exit(1);
	}
	
	/**
	 * Wrapper method to access super.cmd. Required if class is used in
	 * another package. i.e., super.cmd has protected access. This method
	 * expects the option to be present.
	 * 
	 * @param option the name of the option
	 * @return channel if cmd.hasOption("channel") == true otherwise it
	 * calls this.printArgsHelp() -> exits the program.
	 */
	public String getSafeOptionValue(String option) {
		if (!cmd.hasOption(option)) {
			this.printArgsHelp("Options not correct\n");
		}
		// return value if exists, otherwise null
		return cmd.getOptionValue(option);
	}
	
	/**
	 * Wrapper method to access super.cmd. Required if class is used in
	 * an external package.
	 * 
	 * @param option is the name of the option to look for
	 * @return a String of the value if present, otherwise returns null.
	 */
	public String getOptionValue(String option) {
		return cmd.getOptionValue(option);
	}
	
	/**
	 * wrapper method for base.cmd. Required if this class is used in another
	 * package. super.cmd has protected access.
	 * 
	 * @param the name of the "option"
	 * @return true/false
	 */
	public boolean hasOption(String option) {
		return cmd.hasOption(option);
	}
}
