package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.net.ServerSocketFactory;

public class Server {

	private ServerArgs serverArgs;
	private HashMap<Resource,String> resources = new HashMap<>();
	private HashMap<String,Consumer<Command>> processManager = new HashMap<>();
	public static final int TIME_OUT_LIMIT = 5000;

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
		// TODO: populate resources from file/Database
		processManager.put("PUBLISH", this::processPublish);
	}

	/**
	 * Examines the command line arguments
	 * 
	 * @return Command object encapsulating the arguments provided
	 */
	public Command parseCommand() {
		if (serverArgs.cmd.hasOption("advertisedhostname")) {
			System.out.println("advertisedhostname command found");
		} else if (serverArgs.cmd.hasOption("port")) {
			System.out.println("port command found");
		}
		return null;
	}

	/**
	 * 
	 */
	public void listen() {
		// TODO: Implement blocking until client request
		ServerSocketFactory factory = ServerSocketFactory.getDefault();

		try (ServerSocket server = factory.createServerSocket(this.serverArgs.getSafePort())) {
			System.out.println("Listening for request...");

			// Wait for connection
			while (true) {
				Socket client = server.accept();
				System.out.println("Received request");
				
				// TODO: replace this with a call to a class that will serve client
				// i.e., implement a runnable class
				Thread t = new Thread(() -> serveClient(client));
				t.start();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/**
	 * 
	 * @param client
	 */
	private void serveClient(Socket client) {
		try (Socket clientSocket = client) {
			clientSocket.setSoTimeout(TIME_OUT_LIMIT);
			DataInputStream input = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

			String request = input.readUTF();
			Command command = (new Command()).fromJson(request);
			System.out.println("Request: " + command.toJson());
						
			// this handles all types of queries
			this.processManager.get(command.command).accept(command);
			clientSocket.close();
			
		} catch (SocketTimeoutException e) {
			System.out.println(e.getClass().getName() + " " + e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void processPublish(Command command) {
		// TODO Auto-generated method stub
	}
}
