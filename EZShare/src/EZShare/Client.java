package EZShare;


/**
 * The class contains functionality to parse command-line arguments according to
 * the assignment specifications. Additionally, it can also communicate with an
 * EZShare.Server that is listening for EZShare.Client, according to the protocol
 * defined in the assignment specifications.
 * 
 * @author Koteski, B
 *
 */
public class Client {
	
	private ClientArgs clientArgs;
	
	public static void main(String[] args) {
		Client client = new Client(args);
		client.parseCommand();
	}
	
	/**
	 * Constructor for Client
	 * 
	 * @param args String[] command line arguments
	 */
	public Client(String[] args) {
		clientArgs = new ClientArgs(args);
	}
	
	/**
	 * Examines the command line arguments
	 * 
	 * @return Command object encapsulating the arguments provided
	 */
	public Command parseCommand() {
		if(clientArgs.hasOption("fetch")) {
			System.out.println("fetch command found");
		}
		else if(clientArgs.hasOption("query")) {
			System.out.println("query command found");
			// get port
			if(!clientArgs.hasOption("port")) {
				clientArgs.printArgsHelp("port arg not provided\n");
			}
			int port = 0;
			try {
				port = Integer.parseInt(clientArgs.cmd.getOptionValue("port"));	
			} catch(NumberFormatException e) {
				clientArgs.printArgsHelp("port not a valid number\n");
			}
			
			// get host
			if(!clientArgs.cmd.hasOption("host")) {
				clientArgs.printArgsHelp("host arg not provided\n");
			}
			String host = clientArgs.cmd.getOptionValue("host");
			Command command = new Command().buildQuery(clientArgs);
			System.out.println(command.toJsonPretty());
			
		}
		return null;
	}
	
}
