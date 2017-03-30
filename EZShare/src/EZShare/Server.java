package EZShare;

public class Server {
	
	
	private ServerArgs serverArgs;
	
<<<<<<< HEAD
	
=======
	public static void main(String[] args) {
		Server server = new Server(args);
		server.parseCommand();
		server.listen();
	}
	
	/**
	 * Constructor for Server
	 * 
	 * @param args String[] command line arguments
	 */
	public Server(String[] args) {
		serverArgs = new ServerArgs(args);
	}
	
	/**
	 * Examines the command line arguments
	 * 
	 * @return Command object encapsulating the arguments provided
	 */
	public Command parseCommand() {
		if(serverArgs.cmd.hasOption("advertisedhostname")) {
			System.out.println("advertisedhostname command found");
		}
		else if(serverArgs.cmd.hasOption("port")) {
			System.out.println("port command found");
		}
		return null;
	}

	/**
	 * 
	 */
>>>>>>> 74f791b832536697885f84cc404a435dd7edf3f5
	public void listen() {
		System.out.println("listening...");

		// TODO: Implement blocking until client request
	}
}
