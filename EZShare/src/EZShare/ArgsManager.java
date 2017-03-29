package EZShare;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public abstract class ArgsManager {
	
	protected Options options = new Options();
	protected HelpFormatter formatter = new HelpFormatter();
	protected CommandLine cmd = null;
	
	public abstract void printArgsHelp();
}
