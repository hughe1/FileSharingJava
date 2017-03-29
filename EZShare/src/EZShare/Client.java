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
		if(clientArgs.cmd.hasOption("fetch")) {
			System.out.println("fetch command found");
		}
		else if(clientArgs.cmd.hasOption("query")) {
			System.out.println("query command found");
		}
		return null;
	}
	
}
